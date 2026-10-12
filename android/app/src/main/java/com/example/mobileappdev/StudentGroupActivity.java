package com.example.mobileappdev;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobileappdev.adapter.StudentAdapter;
import com.example.mobileappdev.data.local.entity.StudentEntity;
import com.example.mobileappdev.data.remote.ApiClient;
import com.example.mobileappdev.session.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
        MaterialButton btnRetry = findViewById(R.id.btnRetry);

        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }
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

        ApiClient.getGroupService(this).getGroup(groupId).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                showLoading(false);

                if (!response.isSuccessful() || response.body() == null
                        || !Objects.equals(response.body().get("success"), Boolean.TRUE)) {
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
                showError();
            }
        });
    }

    private void updateGroupUi(Map<?, ?> data) {
        Object gcObj = data.get("groupCode");
        if (gcObj == null) {
            gcObj = data.get("group_code");
        }
        String groupCode = gcObj != null ? gcObj.toString() : "Group " + groupId;

        Object nameObj = data.get("name");
        String groupName = nameObj != null ? nameObj.toString() : "";

        Object capObj = data.get("capacity");
        int capacity = capObj instanceof Number ? ((Number) capObj).intValue() : 15;

        Object occObj = data.get("occupied");
        int occupied = occObj instanceof Number ? ((Number) occObj).intValue() : 0;

        Object remObj = data.get("placesRemaining");
        int placesRemaining = remObj instanceof Number ?
                ((Number) remObj).intValue() : Math.max(0, capacity - occupied);

        String title = "Lab Group " + groupCode + (groupName.isEmpty() ? "" : " - " + groupName);
        tvGroupTitle.setText(title);
        toolbar.setTitle(groupCode + " Details");

        String capacityRatioText = occupied + " / " + capacity + " Students";
        tvCapacityRatio.setText(capacityRatioText);

        String placesRemainingText = placesRemaining + " places remaining";
        tvPlacesRemainingPill.setText(placesRemainingText);

        progressCapacity.setMax(capacity);
        progressCapacity.setProgress(occupied);

        formatProgrammeBreakdown(data);

        List<StudentEntity> membersList = new ArrayList<>();
        List<?> membersData = (List<?>) data.get("members");
        if (membersData != null) {
            for (Object obj : membersData) {
                if (obj instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) obj;
                    Object sIdObj = map.get("student_id");
                    long serverId = sIdObj instanceof Number ? ((Number) sIdObj).longValue() : 0;

                    Object sNumObj = map.get("student_number");
                    String sNum = sNumObj != null ? sNumObj.toString() : "";

                    Object sNameObj = map.get("student_name");
                    if (sNameObj == null) sNameObj = map.get("name");
                    String sName = sNameObj != null ? sNameObj.toString() : "";

                    Object progCodeObj = map.get("programme_code");
                    if (progCodeObj == null) progCodeObj = map.get("programme_name");
                    String prog = progCodeObj != null ? progCodeObj.toString() : "CS";

                    StudentEntity student = new StudentEntity();
                    student.serverId = serverId;
                    student.studentNumber = sNum;
                    student.name = sName;
                    student.programme = prog;
                    student.labGroup = groupCode;
                    membersList.add(student);
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
                    formattedLines.add(formatFullProgrammeName(item.toString()));
                }
            }
        } else {
            Map<?, ?> countsMap = (Map<?, ?>) data.get("programmeCounts");
            if (countsMap != null) {
                countsMap.forEach((key, value) -> {
                    String prog = key != null ? key.toString() : "Other";
                    formattedLines.add(getFullProgrammeName(prog) + ": " + value);
                });
            }
        }

        if (formattedLines.isEmpty()) {
            tvProgrammeBreakdown.setText("No programme breakdown available");
        } else {
            tvProgrammeBreakdown.setText(String.join("\n", formattedLines));
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

    private void onStudentClicked(StudentEntity student) {
        Toast.makeText(this, student.name + " (" + student.studentNumber + ")", Toast.LENGTH_SHORT).show();
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
