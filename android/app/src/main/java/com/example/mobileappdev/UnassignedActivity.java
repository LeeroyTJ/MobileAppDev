package com.example.mobileappdev;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobileappdev.adapter.StudentAdapter;
import com.example.mobileappdev.data.local.AppDatabase;
import com.example.mobileappdev.session.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;

public class UnassignedActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private AppDatabase db;
    private RecyclerView unassignedList;
    private TextView unassignedCount;
    private View emptyState;
    private StudentAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = SessionManager.getInstance(this);
        if (!sessionManager.isLoggedIn()) {
            Intent intent = new Intent(this, SignInActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_unassigned);

        db = AppDatabase.getInstance(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        unassignedList = findViewById(R.id.unassigned_list);
        unassignedCount = findViewById(R.id.unassigned_count);
        emptyState = findViewById(R.id.empty_state);

        setupRecyclerView();
        loadUnassignedStudents();
        setupBottomNavigation();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    private void setupRecyclerView() {
        if (unassignedList != null) {
            unassignedList.setLayoutManager(new LinearLayoutManager(this));
            adapter = new StudentAdapter(student -> {
                StudentSheetBottomSheetDialogFragment sheet = StudentSheetBottomSheetDialogFragment.newInstance(
                        student.name,
                        student.studentNumber,
                        student.programme,
                        student.labGroup,
                        student.serverId
                );
                sheet.show(getSupportFragmentManager(), "StudentSheet");
            });
            unassignedList.setAdapter(adapter);
        }
    }

    private void loadUnassignedStudents() {
        long accountId = sessionManager.getAccountId();
        db.studentDao().observeUnassigned(accountId).observe(this, students -> {
            if (students != null) {
                if (adapter != null) {
                    adapter.setStudents(students);
                }
                if (unassignedCount != null) {
                    unassignedCount.setText(String.valueOf(students.size()));
                }
                if (emptyState != null) {
                    emptyState.setVisibility(students.isEmpty() ? View.VISIBLE : View.GONE);
                }
                if (unassignedList != null) {
                    unassignedList.setVisibility(students.isEmpty() ? View.GONE : View.VISIBLE);
                }
            }
        });
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_unassigned);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_unassigned) {
                    return true;
                } else if (id == R.id.nav_students) {
                    startActivity(new Intent(this, RosterActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_more) {
                    MoreBottomSheetDialogFragment sheet = new MoreBottomSheetDialogFragment();
                    sheet.show(getSupportFragmentManager(), MoreBottomSheetDialogFragment.TAG);
                    return true;
                }
                return false;
            });
        }
    }
}
