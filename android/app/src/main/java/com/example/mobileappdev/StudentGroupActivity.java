package com.example.mobileappdev;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobileappdev.adapter.StudentAdapter;
import com.example.mobileappdev.data.local.entity.StudentEntity;
import com.example.mobileappdev.data.remote.ApiClient;
import com.example.mobileappdev.session.SessionManager;
import com.example.mobileappdev.util.CapacityFormatter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudentGroupActivity extends AppCompatActivity {

    public static final String EXTRA_GROUP_ID = "extra_group_id";
    private static final String[] PROGRAMME_ORDER = {"CS", "IT", "DS"};

    private TextView tvGroupTitle, tvCapacityRatio, tvPlacesRemainingPill,
            tvProgrammeBreakdown, tvMembersTitle, tvErrorTitle, tvTitle;
    private LinearProgressIndicator progressCapacity;
    private RecyclerView rvGroupMembers;
    private CircularProgressIndicator loadingIndicator;
    private View errorState, btnRetry, btnBack;
    private StudentAdapter adapter;

    private long groupId = -1;          // -1 means "the signed-in student's own group"
    private boolean drilledIn = false;  // opened from another screen with an explicit group
    private boolean isLecturer = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SessionManager session = SessionManager.getInstance(this);
        if (!session.isLoggedIn()) {
            Intent i = new Intent(this, SignInActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
            return;
        }
        isLecturer = session.isLecturer();

        long extra = getIntent().getLongExtra(EXTRA_GROUP_ID, -1);
        if (extra <= 0) extra = getIntent().getLongExtra("groupId", -1);
        groupId = extra;
        drilledIn = extra > 0;

        // A lecturer should only arrive here from the Groups overview, with a group chosen.
        if (isLecturer && !drilledIn) { finish(); return; }

        setContentView(R.layout.activity_group_detail);
        bindViews();
        setupBottomNav();
        setupRecyclerView();
        load();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { finish(); }
        });
    }

    private void bindViews() {
        tvTitle = findViewById(R.id.tvTitle);
        tvGroupTitle = findViewById(R.id.tvGroupTitle);
        tvCapacityRatio = findViewById(R.id.tvCapacityRatio);
        tvPlacesRemainingPill = findViewById(R.id.tvPlacesRemainingPill);
        progressCapacity = findViewById(R.id.progressCapacity);
        tvProgrammeBreakdown = findViewById(R.id.tvProgrammeBreakdown);
        tvMembersTitle = findViewById(R.id.tvMembersTitle);
        rvGroupMembers = findViewById(R.id.rvGroupMembers);
        loadingIndicator = findViewById(R.id.loading_indicator);
        errorState = findViewById(R.id.error_state);
        tvErrorTitle = findViewById(R.id.tvErrorTitle);
        btnRetry = findViewById(R.id.btnRetry);
        btnBack = findViewById(R.id.btnBack);

        if (tvTitle != null) {
            tvTitle.setText(R.string.group_title);
        }

        if (drilledIn) {                       // tabs never show a back arrow; drill-ins do
            if (btnBack != null) {
                btnBack.setVisibility(View.VISIBLE);
                btnBack.setOnClickListener(v -> finish());
            }
        }
        if (isLecturer) {                      // the member list is for lecturers only
            if (tvMembersTitle != null) tvMembersTitle.setVisibility(View.VISIBLE);
            if (rvGroupMembers != null) rvGroupMembers.setVisibility(View.VISIBLE);
        }
        if (btnRetry != null) {
            btnRetry.setOnClickListener(v -> load());
        }
    }

    private void setupBottomNav() {
        BottomNavigationView nav = findViewById(R.id.bottomNav);
        if (nav == null) return;
        if (isLecturer) { nav.setVisibility(View.GONE); return; }

        nav.getMenu().clear();
        nav.inflateMenu(R.menu.bottom_nav_student);
        nav.setSelectedItemId(R.id.navGroup);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Class<?> target = id == R.id.navHome ? ProfileActivity.class
                    : id == R.id.navSync ? SyncActivity.class : null;
            if (target != null) {
                Intent i = new Intent(this, target);
                i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(i);
            }
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null && !isLecturer) {
            bottomNav.setSelectedItemId(R.id.navGroup);
        }
    }

    private void setupRecyclerView() {
        if (rvGroupMembers != null) {
            rvGroupMembers.setLayoutManager(new LinearLayoutManager(this));
            adapter = new StudentAdapter(student ->
                    Toast.makeText(this, student.name, Toast.LENGTH_SHORT).show());
            rvGroupMembers.setAdapter(adapter);
        }
    }

    // ----- loading -----

    private void load() {
        hideError();
        showLoading(true);
        if (groupId > 0) loadGroupData(); else resolveOwnGroup();
    }

    /** The Group tab has no extra, so first ask the server which group THIS student is in. */
    private void resolveOwnGroup() {
        ApiClient.getStudentService(this).getMyProfile().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call,
                                   @NonNull Response<Map<String, Object>> response) {
                if (response.code() == 401 || response.code() == 403) {
                    showLoading(false);
                    new SessionExpiredDialogFragment()
                            .show(getSupportFragmentManager(), SessionExpiredDialogFragment.TAG);
                    return;
                }
                if (!response.isSuccessful() || response.body() == null
                        || !(response.body().get("data") instanceof Map)) {
                    showLoading(false);
                    showError();
                    return;
                }
                Map<?, ?> data = (Map<?, ?>) response.body().get("data");
                Object gid = data.get("group_id");
                if (gid instanceof Number) {
                    groupId = ((Number) gid).longValue();
                    loadGroupData();
                } else {
                    showLoading(false);
                    showNoGroup();            // unassigned student
                }
            }
            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                showLoading(false);
                showError();
            }
        });
    }

    private void loadGroupData() {
        ApiClient.getGroupService(this).getGroup(groupId).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call,
                                   @NonNull Response<Map<String, Object>> response) {
                showLoading(false);
                if (!response.isSuccessful() || response.body() == null
                        || !Objects.equals(response.body().get("success"), Boolean.TRUE)
                        || !(response.body().get("data") instanceof Map)) {
                    showError();
                    return;
                }
                updateGroupUi((Map<?, ?>) response.body().get("data"));
            }
            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                showLoading(false);
                showError();
            }
        });
    }

    // ----- rendering -----

    private void updateGroupUi(Map<?, ?> data) {
        Object code = data.get("groupCode") != null ? data.get("groupCode") : data.get("group_code");
        String groupCode = code != null ? code.toString() : "G" + groupId;

        if (tvGroupTitle != null) {
            tvGroupTitle.setText(groupCode);   // just "G01": the label above already says "Lab Group"
        }

        int capacity = intOf(data.get("capacity"), 15);
        int occupied = intOf(data.get("occupied"), 0);
        CapacityFormatter.bind(this, occupied, capacity,
                tvCapacityRatio, tvPlacesRemainingPill, progressCapacity);

        showProgrammeBreakdown(data);
        if (isLecturer) showMembers(data, groupCode);
    }

    private void showProgrammeBreakdown(Map<?, ?> data) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        Object list = data.get("programmeBreakdown");
        Object map = data.get("programmeCounts");

        if (list instanceof List) {
            for (Object item : (List<?>) list) {
                if (item == null) continue;
                String[] parts = item.toString().split(":", 2);
                if (parts.length == 2) counts.put(codeOf(parts[0]), intOf(parts[1].trim(), 0));
            }
        } else if (map instanceof Map) {
            for (Map.Entry<?, ?> e : ((Map<?, ?>) map).entrySet()) {
                counts.put(codeOf(String.valueOf(e.getKey())), intOf(e.getValue(), 0));
            }
        }

        StringBuilder sb = new StringBuilder();
        for (String code : PROGRAMME_ORDER) {          // fixed order, always all three
            if (sb.length() > 0) sb.append('\n');
            Integer n = counts.get(code);
            sb.append(fullName(code)).append(": ").append(n != null ? n : 0);
        }
        if (tvProgrammeBreakdown != null) {
            tvProgrammeBreakdown.setText(sb.toString());
        }
    }

    private void showMembers(Map<?, ?> data, String groupCode) {
        List<StudentEntity> members = new ArrayList<>();
        Object raw = data.get("members");
        if (raw instanceof List) {
            for (Object o : (List<?>) raw) {
                if (!(o instanceof Map)) continue;
                Map<?, ?> m = (Map<?, ?>) o;
                StudentEntity s = new StudentEntity();
                Object sIdObj = m.get("student_id");
                s.serverId = sIdObj instanceof Number ? ((Number) sIdObj).longValue() : 0;
                Object sNumObj = m.get("student_number");
                s.studentNumber = sNumObj != null ? sNumObj.toString() : "";
                Object name = m.get("student_name") != null ? m.get("student_name") : m.get("name");
                s.name = name != null ? name.toString() : "";
                Object prog = m.get("programme_code") != null ? m.get("programme_code") : m.get("programme_name");
                s.programme = prog != null ? prog.toString() : "";
                s.labGroup = groupCode;
                members.add(s);
            }
        }
        if (adapter != null) {
            adapter.setStudents(members);
        }
    }

    // ----- helpers -----

    private static int intOf(Object o, int fallback) {
        if (o instanceof Number) return ((Number) o).intValue();
        try { return Integer.parseInt(String.valueOf(o).trim()); }
        catch (Exception e) { return fallback; }
    }

    private static String codeOf(String raw) {
        String s = raw.trim().toLowerCase(Locale.ROOT);
        if (s.equals("cs") || s.contains("computer science")) return "CS";
        if (s.equals("it") || s.contains("information technology")) return "IT";
        if (s.equals("ds") || s.contains("data science")) return "DS";
        return raw.trim().toUpperCase(Locale.ROOT);
    }

    private String fullName(String code) {
        switch (code) {
            case "CS": return getString(R.string.programme_cs);
            case "IT": return getString(R.string.programme_it);
            case "DS": return getString(R.string.programme_ds);
            default:   return code;
        }
    }

    private void showLoading(boolean loading) {
        if (loadingIndicator != null) {
            loadingIndicator.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

    private void showError() {
        if (tvErrorTitle != null) tvErrorTitle.setText(R.string.group_load_failed);
        if (btnRetry != null) btnRetry.setVisibility(View.VISIBLE);
        if (errorState != null) errorState.setVisibility(View.VISIBLE);
    }

    private void showNoGroup() {
        if (tvErrorTitle != null) tvErrorTitle.setText(R.string.group_none_yet);
        if (btnRetry != null) btnRetry.setVisibility(View.GONE);
        if (errorState != null) errorState.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        if (errorState != null) {
            errorState.setVisibility(View.GONE);
        }
    }
}
