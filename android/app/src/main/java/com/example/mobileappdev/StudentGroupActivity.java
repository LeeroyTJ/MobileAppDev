package com.example.mobileappdev;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobileappdev.data.local.entity.StudentEntity;
import com.example.mobileappdev.data.remote.ApiClient;
import com.example.mobileappdev.session.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudentGroupActivity extends AppCompatActivity {

    private TextView tvGroupName;
    private TextView tvCapacityOccupancy;
    private ProgressBar progressCapacity;
    private TextView tvPlacesRemainingPill;
    private TextView tvProgrammeSplitDetails;

    private StudentAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SessionManager sessionManager = SessionManager.getInstance(this);
        if (!sessionManager.isLoggedIn()) {
            Intent intent = new Intent(this, SignInActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        // Role Guard: Student only
        if (sessionManager.isLecturer()) {
            Intent intent = new Intent(this, RosterActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_group_detail);

        bindViews();
        setupNavigation();
        loadGroupData();
    }

    private void bindViews() {
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        tvGroupName = findViewById(R.id.tvGroupName);
        tvCapacityOccupancy = findViewById(R.id.tvCapacityOccupancy);
        progressCapacity = findViewById(R.id.progressCapacity);
        tvPlacesRemainingPill = findViewById(R.id.tvPlacesRemainingPill);
        tvProgrammeSplitDetails = findViewById(R.id.tvProgrammeSplitDetails);
        RecyclerView rvGroupMembers = findViewById(R.id.rvGroupMembers);

        adapter = new StudentAdapter(new StudentAdapter.OnStudentClickListener() {
            @Override
            public void onStudentClick(StudentEntity student) {
                Toast.makeText(StudentGroupActivity.this, student.name + " (" + student.studentNumber + ")", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStudentOptionsClick(StudentEntity student, View anchorView) {
                onStudentClick(student);
            }
        });

        if (rvGroupMembers != null) {
            rvGroupMembers.setLayoutManager(new LinearLayoutManager(this));
            rvGroupMembers.setAdapter(adapter);
        }
    }

    private void setupNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.navGroup);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.navHome) {
                    startActivity(new Intent(this, ProfileActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.navGroup) {
                    return true;
                } else if (id == R.id.navSync) {
                    startActivity(new Intent(this, SyncActivity.class));
                    finish();
                    return true;
                }
                return false;
            });
        }
    }

    private void loadGroupData() {
        ApiClient.getStudentService(this).getMyProfile().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null
                        && Boolean.TRUE.equals(response.body().get("success"))) {

                    Map<?, ?> data = (Map<?, ?>) response.body().get("data");
                    if (data != null) {
                        Object groupCodeObj = data.get("group_code");
                        String groupCode = groupCodeObj != null ? groupCodeObj.toString() : "Unassigned";

                        tvGroupName.setText("Lab Group " + groupCode);
                        fetchGroupRoster(groupCode);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                tvGroupName.setText("Lab Group G02");
                fetchGroupRoster("G02");
            }
        });
    }

    private void fetchGroupRoster(String groupCode) {
        ApiClient.getStudentService(this).getRoster(null, groupCode, null).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null
                        && Boolean.TRUE.equals(response.body().get("success"))) {

                    List<?> dataList = (List<?>) response.body().get("data");
                    if (dataList != null) {
                        List<StudentEntity> members = new ArrayList<>();
                        int csCount = 0, itCount = 0, dsCount = 0;

                        for (Object item : dataList) {
                            if (item instanceof Map) {
                                Map<?, ?> map = (Map<?, ?>) item;
                                StudentEntity student = new StudentEntity();
                                student.studentNumber = map.get("student_number") != null ? map.get("student_number").toString() : "";
                                student.name = map.get("student_name") != null ? map.get("student_name").toString() : "";
                                student.programme = map.get("programme_code") != null ? map.get("programme_code").toString() : "CS";
                                student.labGroup = groupCode;
                                student.syncStatus = StudentEntity.STATUS_SYNCED;

                                if ("CS".equalsIgnoreCase(student.programme)) csCount++;
                                else if ("IT".equalsIgnoreCase(student.programme)) itCount++;
                                else if ("DS".equalsIgnoreCase(student.programme)) dsCount++;

                                members.add(student);
                            }
                        }

                        int occupied = members.size();
                        int remaining = Math.max(0, 15 - occupied);

                        tvCapacityOccupancy.setText(occupied + " / 15 places filled");
                        progressCapacity.setProgress(occupied);
                        tvPlacesRemainingPill.setText(remaining + " places remaining");
                        tvProgrammeSplitDetails.setText("CS: " + csCount + " • IT: " + itCount + " • DS: " + dsCount);

                        adapter.submitList(members);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                tvCapacityOccupancy.setText("11 / 15 places filled");
                progressCapacity.setProgress(11);
                tvPlacesRemainingPill.setText("4 places remaining");
            }
        });
    }
}
