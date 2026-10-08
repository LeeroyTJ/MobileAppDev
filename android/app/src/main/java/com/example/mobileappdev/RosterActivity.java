package com.example.mobileappdev;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobileappdev.data.local.AppDatabase;
import com.example.mobileappdev.data.local.entity.StudentEntity;
import com.example.mobileappdev.data.remote.ApiClient;
import com.example.mobileappdev.session.SessionManager;
import com.example.mobileappdev.viewmodel.RosterViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RosterActivity extends AppCompatActivity {

    private RosterViewModel viewModel;
    private SessionManager sessionManager;
    private AppDatabase db;

    private TextInputEditText searchInput;
    private TextView offlineBanner;
    private ChipGroup chipGroupProgramme;
    private ChipGroup chipGroupLabGroup;
    private MaterialCardView activeFiltersCard;
    private TextView activeFiltersCount;
    private TextView activeFiltersDetail;
    private MaterialButton buttonClearFilters;
    private TextView resultCount;
    private RecyclerView rosterList;
    private CircularProgressIndicator loadingIndicator;
    private View emptyState;
    private TextView emptyTitle;
    private TextView emptyMessage;
    private MaterialButton emptyAction;
    private View errorState;
    private BottomNavigationView bottomNavigation;
    private FloatingActionButton fabAdd;

    private StudentAdapter adapter;

    // Search debounce timer (300ms)
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private List<StudentEntity> cachedAccountStudents = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_roster);

        sessionManager = SessionManager.getInstance(this);
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        }

        db = AppDatabase.getInstance(this);
        viewModel = new ViewModelProvider(this).get(RosterViewModel.class);

        bindViews();
        setupRecyclerView();
        setupSearchDebounce(); // Task 4.2
        setupChipFilters();    // Task 4.3
        setupClearFilters();
        setupBottomNavigation();
        setupFab();
        observeViewModel();
        loadLocalStudents();
        fetchRemoteRoster();
    }

    private void bindViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        searchInput = findViewById(R.id.search_input);
        offlineBanner = findViewById(R.id.offline_banner);
        chipGroupProgramme = findViewById(R.id.chip_group_programme);
        chipGroupLabGroup = findViewById(R.id.chip_group_lab_group);
        activeFiltersCard = findViewById(R.id.active_filters_card);
        activeFiltersCount = findViewById(R.id.active_filters_count);
        activeFiltersDetail = findViewById(R.id.active_filters_detail);
        buttonClearFilters = findViewById(R.id.button_clear_filters);
        resultCount = findViewById(R.id.result_count);
        
        MaterialButton buttonOpenFilters = findViewById(R.id.button_open_filters);
        buttonOpenFilters.setOnClickListener(v -> {
            FilterBottomSheetDialogFragment dialog = new FilterBottomSheetDialogFragment();
            dialog.show(getSupportFragmentManager(), FilterBottomSheetDialogFragment.TAG);
        });

        rosterList = findViewById(R.id.roster_list);
        loadingIndicator = findViewById(R.id.loading_indicator);
        emptyState = findViewById(R.id.empty_state);
        emptyTitle = findViewById(R.id.empty_title);
        emptyMessage = findViewById(R.id.empty_message);
        emptyAction = findViewById(R.id.empty_action);
        errorState = findViewById(R.id.error_state);

        MaterialButton errorRetry = findViewById(R.id.error_retry);
        errorRetry.setOnClickListener(v -> fetchRemoteRoster());

        bottomNavigation = findViewById(R.id.bottom_navigation);
        fabAdd = findViewById(R.id.fab_add);

        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.inflateMenu(R.menu.menu_roster_toolbar);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_share) {
                shareSanitizedGroupSummary();
                return true;
            }
            return false;
        });
    }

    private void setupRecyclerView() {
        adapter = new StudentAdapter(new StudentAdapter.OnStudentClickListener() {
            @Override
            public void onStudentClick(StudentEntity student) {
                Toast.makeText(RosterActivity.this, student.name + " (" + student.studentNumber + ")", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStudentOptionsClick(StudentEntity student, View anchorView) {
                Toast.makeText(RosterActivity.this, "Options for " + student.name, Toast.LENGTH_SHORT).show();
            }
        });

        rosterList.setLayoutManager(new LinearLayoutManager(this));
        rosterList.setAdapter(adapter);
    }

    /**
     * Task 4.2: Wire search bar input with a 300ms debounce timer to update RosterViewModel.setSearchQuery()
     */
    private void setupSearchDebounce() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                String query = s != null ? s.toString().trim() : "";
                searchRunnable = () -> viewModel.setSearchQuery(query);
                searchHandler.postDelayed(searchRunnable, 300);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    /**
     * Task 4.3: Connect programme chips (chip_cs, chip_it, chip_ds) and group chips (chip_g01-chip_g04, chip_unassigned) to dynamically update filter states
     */
    private void setupChipFilters() {
        chipGroupProgramme.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                viewModel.setProgrammeFilter("");
                return;
            }

            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chip_cs) {
                viewModel.setProgrammeFilter("CS");
            } else if (checkedId == R.id.chip_it) {
                viewModel.setProgrammeFilter("IT");
            } else if (checkedId == R.id.chip_ds) {
                viewModel.setProgrammeFilter("DS");
            } else {
                viewModel.setProgrammeFilter("");
            }
        });

        chipGroupLabGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                viewModel.setGroupFilter("");
                return;
            }

            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chip_g01) {
                viewModel.setGroupFilter("G01");
            } else if (checkedId == R.id.chip_g02) {
                viewModel.setGroupFilter("G02");
            } else if (checkedId == R.id.chip_g03) {
                viewModel.setGroupFilter("G03");
            } else if (checkedId == R.id.chip_g04) {
                viewModel.setGroupFilter("G04");
            } else if (checkedId == R.id.chip_unassigned) {
                viewModel.setGroupFilter("Unassigned");
            } else {
                viewModel.setGroupFilter("");
            }
        });
    }

    private void setupClearFilters() {
        View.OnClickListener clearListener = v -> clearAllFilters();
        buttonClearFilters.setOnClickListener(clearListener);
        emptyAction.setOnClickListener(clearListener);
    }

    private void clearAllFilters() {
        chipGroupProgramme.clearCheck();
        chipGroupLabGroup.clearCheck();
        searchInput.setText("");
        viewModel.clearAllFilters();
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_students);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_students) {
                return true;
            } else if (itemId == R.id.nav_more) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });
    }

    private void setupFab() {
        if (sessionManager.isLecturer()) {
            fabAdd.setVisibility(View.VISIBLE);
            fabAdd.setOnClickListener(v ->
                    Toast.makeText(RosterActivity.this, "Add Student Dialog", Toast.LENGTH_SHORT).show()
            );
        } else {
            fabAdd.setVisibility(View.GONE);
        }
    }

    private void observeViewModel() {
        viewModel.getStudents().observe(this, students -> {
            if (students == null || students.isEmpty()) {
                rosterList.setVisibility(View.GONE);
                emptyState.setVisibility(View.VISIBLE);
                boolean hasFilter = !TextUtils.isEmpty(viewModel.getSearchQuery().getValue())
                        || !TextUtils.isEmpty(viewModel.getProgrammeFilter().getValue())
                        || !TextUtils.isEmpty(viewModel.getGroupFilter().getValue());
                if (hasFilter) {
                    emptyTitle.setText(R.string.roster_empty_filtered_title);
                    emptyMessage.setText(R.string.roster_empty_filtered_message);
                    emptyAction.setVisibility(View.VISIBLE);
                } else {
                    emptyTitle.setText(R.string.roster_empty_none_title);
                    emptyMessage.setText(R.string.roster_empty_none_message);
                    emptyAction.setVisibility(View.GONE);
                }
            } else {
                emptyState.setVisibility(View.GONE);
                rosterList.setVisibility(View.VISIBLE);
                adapter.submitList(students);
            }
        });

        viewModel.getStudentsFound().observe(this, count -> {
            int studentCount = count != null ? count : 0;
            resultCount.setText(getResources().getQuantityString(R.plurals.roster_count_found, studentCount, studentCount));
        });

        // Combined filter state observer to update active filters card and filter the cached student list
        Runnable applyFiltersRunnable = this::applyFiltersAndFilterList;

        viewModel.getSearchQuery().observe(this, query -> applyFiltersRunnable.run());
        viewModel.getProgrammeFilter().observe(this, prog -> applyFiltersRunnable.run());
        viewModel.getGroupFilter().observe(this, group -> applyFiltersRunnable.run());
        viewModel.getStatusFilter().observe(this, status -> applyFiltersRunnable.run());
    }

    private void loadLocalStudents() {
        long accountId = sessionManager.getAccountId();
        db.studentDao().observeAllForAccount(accountId).observe(this, students -> {
            if (students != null) {
                cachedAccountStudents = students;
                applyFiltersAndFilterList();
            }
        });
    }

    private void applyFiltersAndFilterList() {
        String query = viewModel.getSearchQuery().getValue();
        String prog = viewModel.getProgrammeFilter().getValue();
        String group = viewModel.getGroupFilter().getValue();
        String status = viewModel.getStatusFilter().getValue();

        List<String> activeFilterLabels = new ArrayList<>();
        if (!TextUtils.isEmpty(prog)) activeFilterLabels.add(prog);
        if (!TextUtils.isEmpty(group)) activeFilterLabels.add(group);
        if (!TextUtils.isEmpty(status)) activeFilterLabels.add(status);
        if (!TextUtils.isEmpty(query)) activeFilterLabels.add("\"" + query + "\"");

        if (!activeFilterLabels.isEmpty()) {
            activeFiltersCard.setVisibility(View.VISIBLE);
            int count = activeFilterLabels.size();
            activeFiltersCount.setText(getResources().getQuantityString(R.plurals.filters_active, count, count));
            activeFiltersDetail.setText(TextUtils.join(" • ", activeFilterLabels));
        } else {
            activeFiltersCard.setVisibility(View.GONE);
        }

        List<StudentEntity> filtered = new ArrayList<>();
        for (StudentEntity student : cachedAccountStudents) {
            if (student == null) continue;

            if (!TextUtils.isEmpty(prog) && !prog.equalsIgnoreCase(student.programme)) {
                continue;
            }

            if (!TextUtils.isEmpty(group)) {
                if ("Unassigned".equalsIgnoreCase(group)) {
                    if (student.labGroup != null) continue;
                } else if (!group.equalsIgnoreCase(student.labGroup)) {
                    continue;
                }
            }

            if (!TextUtils.isEmpty(status) && !status.equalsIgnoreCase(student.syncStatus)) {
                continue;
            }

            if (!TextUtils.isEmpty(query)) {
                String qLower = query.toLowerCase();
                boolean nameMatch = student.name.toLowerCase().contains(qLower);
                boolean numberMatch = student.studentNumber.toLowerCase().contains(qLower);
                if (!nameMatch && !numberMatch) {
                    continue;
                }
            }

            filtered.add(student);
        }

        viewModel.setStudents(filtered);
        viewModel.setStudentsFound(filtered.size());
    }

    private void fetchRemoteRoster() {
        loadingIndicator.setVisibility(View.VISIBLE);
        errorState.setVisibility(View.GONE);

        String prog = viewModel.getProgrammeFilter().getValue();
        String grp = viewModel.getGroupFilter().getValue();
        String search = viewModel.getSearchQuery().getValue();

        ApiClient.getStudentService(this).getRoster(prog, grp, search).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                loadingIndicator.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    offlineBanner.setVisibility(View.GONE);
                } else {
                    offlineBanner.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                loadingIndicator.setVisibility(View.GONE);
                offlineBanner.setVisibility(View.VISIBLE);
            }
        });
    }

    private void shareSanitizedGroupSummary() {
        int g01Count = 0;
        int g02Count = 0;
        int g03Count = 0;
        int g04Count = 0;
        int unassignedCount = 0;

        for (StudentEntity student : cachedAccountStudents) {
            if (student == null) continue;
            String group = student.labGroup;
            if ("G01".equalsIgnoreCase(group)) g01Count++;
            else if ("G02".equalsIgnoreCase(group)) g02Count++;
            else if ("G03".equalsIgnoreCase(group)) g03Count++;
            else if ("G04".equalsIgnoreCase(group)) g04Count++;
            else unassignedCount++;
        }

        int totalEnrolled = cachedAccountStudents.size();

        StringBuilder summary = new StringBuilder();
        summary.append("CohortHub Lab Group Summary\n");
        summary.append("Total Enrolled Students: ").append(totalEnrolled).append("\n\n");
        summary.append("Group Capacity Breakdown (Max 15 per group):\n");
        summary.append("• Lab Group G01: ").append(g01Count).append("/15 places filled\n");
        summary.append("• Lab Group G02: ").append(g02Count).append("/15 places filled\n");
        summary.append("• Lab Group G03: ").append(g03Count).append("/15 places filled\n");
        summary.append("• Lab Group G04: ").append(g04Count).append("/15 places filled\n");
        summary.append("• Unassigned Students: ").append(unassignedCount);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "CohortHub Lab Group Summary");
        shareIntent.putExtra(Intent.EXTRA_TEXT, summary.toString());

        startActivity(Intent.createChooser(shareIntent, "Share Group Summary"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        searchHandler.removeCallbacksAndMessages(null);
    }
}
