package com.example.mobileappdev;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobileappdev.adapter.StudentAdapter;
import com.example.mobileappdev.data.remote.ApiClient;
import com.example.mobileappdev.model.Student;
import com.example.mobileappdev.session.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UnassignedActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextView tvCountPill;
    private RecyclerView recyclerView;
    private CircularProgressIndicator loadingIndicator;
    private View emptyState;
    private View errorState;
    private MaterialButton btnErrorRetry;
    private BottomNavigationView bottomNav;

    private StudentAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unassigned);

        if (!SessionManager.getInstance(this).isLoggedIn()) {
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        }

        bindViews();
        setupNavigation();
        setupRecyclerView();
        loadUnassignedStudents();
    }

    private void bindViews() {
        toolbar = findViewById(R.id.toolbar);
        tvCountPill = findViewById(R.id.unassigned_count);
        recyclerView = findViewById(R.id.unassigned_list);
        loadingIndicator = findViewById(R.id.loading_indicator);
        emptyState = findViewById(R.id.empty_state);
        errorState = findViewById(R.id.error_state);
        btnErrorRetry = findViewById(R.id.error_retry);
        bottomNav = findViewById(R.id.bottom_navigation);
    }

    private void setupNavigation() {
        toolbar.setNavigationOnClickListener(v -> finish());

        bottomNav.setSelectedItemId(R.id.nav_unassigned);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_unassigned) {
                return true;
            } else if (id == R.id.nav_students) {
                startActivity(new Intent(UnassignedActivity.this, MainActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_more) {
                startActivity(new Intent(UnassignedActivity.this, ProfileActivity.class));
                finish();
                return true;
            }
            return false;
        });

        if (btnErrorRetry != null) {
            btnErrorRetry.setOnClickListener(v -> loadUnassignedStudents());
        }
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StudentAdapter(this::showGroupAssignmentDialog);
        recyclerView.setAdapter(adapter);
    }

    private void loadUnassignedStudents() {
        showLoadingState();

        ApiClient.getStudentService(this).getRoster(null, "UNASSIGNED", null)
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                        if (!response.isSuccessful() || response.body() == null
                                || !Boolean.TRUE.equals(response.body().get("success"))) {
                            handleFailure(response.code());
                            return;
                        }

                        List<?> dataList = (List<?>) response.body().get("data");
                        List<Student> students = new ArrayList<>();
                        if (dataList != null) {
                            for (Object item : dataList) {
                                if (item instanceof Map) {
                                    students.add(Student.fromMap((Map<?, ?>) item));
                                }
                            }
                        }

                        updateUiWithStudents(students);
                    }

                    @Override
                    public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                        showErrorState();
                    }
                });
    }

    private void updateUiWithStudents(List<Student> students) {
        hideAllContentStates();
        adapter.setStudents(students);

        int count = students.size();
        String countText = count + (count == 1 ? " student" : " students");
        tvCountPill.setText(countText);

        if (students.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }

    private void showLoadingState() {
        hideAllContentStates();
        loadingIndicator.setVisibility(View.VISIBLE);
    }

    private void showErrorState() {
        hideAllContentStates();
        errorState.setVisibility(View.VISIBLE);
    }

    private void hideAllContentStates() {
        if (loadingIndicator != null) loadingIndicator.setVisibility(View.GONE);
        if (emptyState != null) emptyState.setVisibility(View.GONE);
        if (errorState != null) errorState.setVisibility(View.GONE);
        if (recyclerView != null) recyclerView.setVisibility(View.GONE);
    }

    private void handleFailure(int code) {
        if (code == 401 || code == 403) {
            SessionManager.getInstance(this).clearSession();
            startActivity(new Intent(this, SignInActivity.class));
            finish();
        } else {
            showErrorState();
        }
    }

    private void showGroupAssignmentDialog(Student student) {
        ApiClient.getGroupService(this).listGroups().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (!response.isSuccessful() || response.body() == null
                        || !Boolean.TRUE.equals(response.body().get("success"))) {
                    Toast.makeText(UnassignedActivity.this, "Failed to load groups", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<?> groupsData = (List<?>) response.body().get("data");
                if (groupsData == null || groupsData.isEmpty()) {
                    Toast.makeText(UnassignedActivity.this, "No groups available", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<Long> groupIds = new ArrayList<>();
                List<String> groupNames = new ArrayList<>();

                for (Object item : groupsData) {
                    if (item instanceof Map) {
                        Map<?, ?> groupMap = (Map<?, ?>) item;
                        long gId = groupMap.get("group_id") instanceof Number ?
                                ((Number) groupMap.get("group_id")).longValue() : 0;
                        String code = groupMap.get("group_code") != null ? groupMap.get("group_code").toString() : "";
                        String name = groupMap.get("name") != null ? groupMap.get("name").toString() : "";
                        long occupied = groupMap.get("occupied") instanceof Number ?
                                ((Number) groupMap.get("occupied")).longValue() : 0;
                        long capacity = groupMap.get("capacity") instanceof Number ?
                                ((Number) groupMap.get("capacity")).longValue() : 0;

                        groupIds.add(gId);
                        groupNames.add(code + " - " + name + " (" + occupied + "/" + capacity + ")");
                    }
                }

                final int[] selectedIndex = {0};

                new MaterialAlertDialogBuilder(UnassignedActivity.this)
                        .setTitle("Assign " + student.getName() + " to Group")
                        .setSingleChoiceItems(groupNames.toArray(new CharSequence[0]), 0, (dialog, which) -> {
                            selectedIndex[0] = which;
                        })
                        .setPositiveButton("Assign", (dialog, which) -> {
                            if (selectedIndex[0] >= 0 && selectedIndex[0] < groupIds.size()) {
                                long targetGroupId = groupIds.get(selectedIndex[0]);
                                executeAssignStudent(student, targetGroupId);
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(UnassignedActivity.this, "Network error loading groups", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void executeAssignStudent(Student student, long groupId) {
        Map<String, Object> body = new HashMap<>();
        body.put("studentId", student.getId());

        ApiClient.getGroupService(this).assignStudent(groupId, body)
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && Boolean.TRUE.equals(response.body().get("success"))) {
                            Toast.makeText(UnassignedActivity.this,
                                    "Assigned " + student.getName() + " to group", Toast.LENGTH_SHORT).show();
                            loadUnassignedStudents();
                        } else if (response.code() == 409) {
                            Toast.makeText(UnassignedActivity.this,
                                    "Selected group is full", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(UnassignedActivity.this,
                                    "Failed to assign student to group", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                        Toast.makeText(UnassignedActivity.this,
                                "Network error during assignment", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
