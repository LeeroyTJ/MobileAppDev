package com.example.mobileappdev;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobileappdev.adapter.StudentAdapter;
import com.example.mobileappdev.data.remote.ApiClient;
import com.example.mobileappdev.model.Student;
import com.example.mobileappdev.session.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.example.mobileappdev.data.local.entity.StudentEntity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudentGroupActivity extends AppCompatActivity {

    public static final String EXTRA_GROUP_ID = "extra_group_id";

    private MaterialToolbar toolbar;
    private TextView tvGroupTitle;
    private TextView tvCapacityRatio;
    private TextView tvPlacesRemainingPill;
    private LinearProgressIndicator progressCapacity;
    private TextView tvProgrammeBreakdown;
    private RecyclerView rvGroupMembers;
    private CircularProgressIndicator loadingIndicator;
    private View errorState;
    private MaterialButton btnRetry;

    private StudentAdapter adapter;
    private long groupId = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_detail);

        if (!SessionManager.getInstance(this).isLoggedIn()) {
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        }

        if (getIntent() != null && getIntent().hasExtra(EXTRA_GROUP_ID)) {
            groupId = getIntent().getLongExtra(EXTRA_GROUP_ID, 1);
        } else if (getIntent() != null && getIntent().hasExtra("groupId")) {
            groupId = getIntent().getLongExtra("groupId", 1);
        }

        bindViews();
        setupRecyclerView();
        loadGroupDetails();
    }

    private void bindViews() {
        toolbar = findViewById(R.id.toolbar);
        tvGroupTitle = findViewById(R.id.tvGroupTitle);
        tvCapacityRatio = findViewById(R.id.tvCapacityRatio);
        tvPlacesRemainingPill = findViewById(R.id.tvPlacesRemainingPill);
        progressCapacity = findViewById(R.id.progressCapacity);
        tvProgrammeBreakdown = findViewById(R.id.tvProgrammeBreakdown);
        rvGroupMembers = findViewById(R.id.rvGroupMembers);
        loadingIndicator = findViewById(R.id.loading_indicator);
        errorState = findViewById(R.id.error_state);
        btnRetry = findViewById(R.id.btnRetry);

        toolbar.setNavigationOnClickListener(v -> finish());
        if (btnRetry != null) {
            btnRetry.setOnClickListener(v -> loadGroupDetails());
        }
    }

    private void setupRecyclerView() {
        rvGroupMembers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StudentAdapter(this::onStudentClicked);
        rvGroupMembers.setAdapter(adapter);
    }

    private void loadGroupDetails() {
        showLoading(true);
        hideError();

        // Fix 3B: Use GroupApiService.getGroup(groupId) instead of getRoster()
        ApiClient.getGroupService(this).getGroup(groupId).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                showLoading(false);

                if (!response.isSuccessful() || response.body() == null
                        || !Boolean.TRUE.equals(response.body().get("success"))) {
                    // Fix 3B: Removed hardcoded "G02" fallback. Show error state on API failure.
                    showError();
                    return;
                }

                Map<?, ?> data = (Map<?, ?>) response.body().get("data");
                if (data == null) {
                    showError();
                    return;
                }

                updateGroupUi(data);
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                showLoading(false);
                // Fix 3B: Removed hardcoded "G02" fallback. Show error state on failure.
                showError();
            }
        });
    }

    private void updateGroupUi(Map<?, ?> data) {
        String groupCode = data.get("groupCode") != null ? data.get("groupCode").toString() :
                (data.get("group_code") != null ? data.get("group_code").toString() : "Group " + groupId);
        String groupName = data.get("name") != null ? data.get("name").toString() : "";

        int capacity = data.get("capacity") instanceof Number ? ((Number) data.get("capacity")).intValue() : 15;
        int occupied = data.get("occupied") instanceof Number ? ((Number) data.get("occupied")).intValue() : 0;
        int placesRemaining = data.get("placesRemaining") instanceof Number ?
                ((Number) data.get("placesRemaining")).intValue() : Math.max(0, capacity - occupied);

        // Populate header title & capacity metrics
        String title = "Lab Group " + groupCode + (groupName.isEmpty() ? "" : " - " + groupName);
        tvGroupTitle.setText(title);
        toolbar.setTitle(groupCode + " Details");

        tvCapacityRatio.setText(occupied + " / " + capacity + " Students");
        tvPlacesRemainingPill.setText(placesRemaining + " places remaining");

        progressCapacity.setMax(capacity);
        progressCapacity.setProgress(occupied);

        // Format programme breakdown with full names
        formatProgrammeBreakdown(data);

        // Populate members list
        List<Student> membersList = new ArrayList<>();
        List<?> membersData = (List<?>) data.get("members");
        if (membersData != null) {
            for (Object obj : membersData) {
                if (obj instanceof Map) {
                    membersList.add(Student.fromMap((Map<?, ?>) obj));
                }
            }
        }
        adapter.setStudents(membersList);
    }

    private void formatProgrammeBreakdown(Map<?, ?> data) {
        List<String> formattedLines = new ArrayList<>();

        List<?> breakdownList = (List<?>) data.get("programmeBreakdown");
        if (breakdownList != null && !breakdownList.isEmpty()) {
            for (Object item : breakdownList) {
                if (item != null) {
                    String line = item.toString();
                    formattedLines.add(formatFullProgrammeName(line));
                }
            }
        } else {
            Map<?, ?> countsMap = (Map<?, ?>) data.get("programmeCounts");
            if (countsMap != null) {
                for (Map.Entry<?, ?> entry : countsMap.entrySet()) {
                    String prog = entry.getKey() != null ? entry.getKey().toString() : "Other";
                    String fullName = getFullProgrammeName(prog);
                    formattedLines.add(fullName + ": " + entry.getValue());
                }
            }
        }

        if (formattedLines.isEmpty()) {
            tvProgrammeBreakdown.setText("No programme breakdown available");
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < formattedLines.size(); i++) {
                sb.append(formattedLines.get(i));
                if (i < formattedLines.size() - 1) {
                    sb.append("\n");
                }
            }
            tvProgrammeBreakdown.setText(sb.toString());
        }
    }

    private String formatFullProgrammeName(String line) {
        if (line.contains(":")) {
            String[] parts = line.split(":", 2);
            String prog = parts[0].trim();
            String count = parts[1].trim();
            return getFullProgrammeName(prog) + ": " + count;
        }
        return line;
    }

    private String getFullProgrammeName(String code) {
        if ("CS".equalsIgnoreCase(code) || "Computer Science".equalsIgnoreCase(code) || code.toLowerCase().contains("computer science")) {
            return "Computer Science";
        } else if ("IT".equalsIgnoreCase(code) || "Information Technology".equalsIgnoreCase(code) || code.toLowerCase().contains("information technology")) {
            return "Information Technology";
        } else if ("DS".equalsIgnoreCase(code) || "Data Science".equalsIgnoreCase(code) || code.toLowerCase().contains("data science")) {
            return "Data Science";
        } else if ("SE".equalsIgnoreCase(code) || "Software Engineering".equalsIgnoreCase(code)) {
            return "Software Engineering";
        } else if ("CE".equalsIgnoreCase(code) || "Computer Engineering".equalsIgnoreCase(code)) {
            return "Computer Engineering";
        }
        return code;
    }

    private void onStudentClicked(Student student) {
        Toast.makeText(this, student.getName() + " (" + student.getStudentNumber() + ")", Toast.LENGTH_SHORT).show();
    }

    private void showLoading(boolean loading) {
        if (loadingIndicator != null) {
            loadingIndicator.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

    private void showError() {
        if (errorState != null) {
            errorState.setVisibility(View.VISIBLE);
        }
    }

    private void hideError() {
        if (errorState != null) {
            errorState.setVisibility(View.GONE);
        }
    }
}
