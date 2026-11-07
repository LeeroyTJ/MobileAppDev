# CohortHub - Project Task Matrix & Team Implementation Roadmap

**Course:** ICT361 Mobile Application Development (Mulungushi University)  
**Project:** CohortHub - Student Registration & Lab Group Management System  
**Reference Document:** `ICT361_Android_Group_Lab.pdf`

---

## 1. The Golden Rule for Conflict-Free Teamwork
**No two members touch the same file.**

If a task spans multiple files (e.g., Server + Android), the members split it: one owns the Server file, the other owns the Android file. They communicate via the API contract, never by editing each other's files.

Before starting any task:
```bash
git pull origin main
```
If you touch a shared file (like `strings.xml`), coordinate in the group chat first.  
Commit and push immediately when your part compiles.

---

## 2. 📁 File Ownership Map (Who Owns What)

| File(s) | Owner | Reason |
|---|---|---|
| `SignInActivity.java`, `activity_signin.xml` | M1 | Role toggle |
| `RegistrationActivity.java`, `activity_registration.xml` | M2 | Progressive claim code, offline detection |
| `NetworkUtils.java` (new) | M2 | Used only by registration |
| `server/routes/auth.routes.js` | M3 | New `/check-student` endpoint |
| `StudentSheetBottomSheetDialogFragment.java` | M4 | Dead stub fix (Correct Number) |
| `RosterActivity.java` | M5 | Pass `base_version`, sort toggle |
| `EditStudentActivity.java` | M6 | Trigger flag, dialog refactor |
| `DeleteStudentActivity.java` | M7 | Unsynced student deletion |
| `GroupPickerBottomSheetDialogFragment.java` | M8 | Disable full groups |
| `MainActivity.java` | M9 | Auto-sync on network return |
| `SyncWorker.java` | M10 | Verify background sync |
| `activity_roster.xml`, `menu_roster_toolbar.xml` | M11 | Sort menu, empty state |
| `strings.xml`, `strings_roster.xml` | M12 | New strings for all tasks |
| `AppDatabase.java` | M13 | Room schema warning fix |
| Testing / Integration / Postman | M14 | QA and verification |

**Rules:**
- If you need a new string, ask **M12** — they own `strings.xml`.
- If you need a new icon, ask **M11** — they own `drawable/`.
- If you touch the server, coordinate with **M3** — they own `auth.routes.js`.

---

## 3. 🎯 Task Assignment by Member

### 🎨 Group 1: Authentication & Onboarding (M1, M2, M3)
**Goal:** Implement the professional role toggle, progressive claim code, and offline-aware registration button.

#### Member 1 — Sign-In Role Toggle
- **Files:** `SignInActivity.java`, `activity_signin.xml`
- **Task:** Add a `MaterialButtonToggleGroup` (Student | Lecturer) to the sign-in screen. When "Lecturer" is selected:
  - Hide the "Register" link (lecturers don't self-register).
  - Change the `label_student_number` hint from "Student number" to "Lecturer ID".
- **Implementation:**
  - In `activity_signin.xml`, add a `MaterialButtonToggleGroup` above `tilStudentNumber`:
    ```xml
    <com.google.android.material.button.MaterialButtonToggleGroup
        android:id="@+id/toggleRole"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        app:singleSelection="true"
        app:selectionRequired="true"
        app:checkedButton="@id/btnRoleStudent"
        app:layout_constraintTop_toBottomOf="@id/tvSubtitle"
        android:layout_marginTop="24dp">
        <com.google.android.material.button.MaterialButton
            android:id="@+id/btnRoleStudent"
            style="?attr/materialButtonOutlinedStyle"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="@string/role_student" />
        <com.google.android.material.button.MaterialButton
            android:id="@+id/btnRoleLecturer"
            style="?attr/materialButtonOutlinedStyle"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="@string/role_lecturer" />
    </com.google.android.material.button.MaterialButtonToggleGroup>
    ```
  - Update constraints on `tilStudentNumber` so it sits below `toggleRole` instead of `tvSubtitle`.
  - In `SignInActivity.onCreate()`:
    ```java
    MaterialButtonToggleGroup toggle = findViewById(R.id.toggleRole);
    toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
        if (!isChecked) return;
        boolean isLecturer = checkedId == R.id.btnRoleLecturer;
        tvRegisterLink.setVisibility(isLecturer ? View.GONE : View.VISIBLE);
        tilStudentNumber.setHint(isLecturer ? R.string.label_lecturer_id : R.string.label_student_number);
    });
    ```
  - Ask **M12** for `role_student`, `role_lecturer`, and `label_lecturer_id` strings.
- **Deliverable:** The sign-in screen has a clean toggle. No changes to login logic (backend doesn't need the role to authenticate — the role comes from the DB).

#### Member 2 — Registration: Offline Detection & Progressive Claim Code
- **Files:** `RegistrationActivity.java`, `activity_registration.xml`, `NetworkUtils.java` (new)
- **Task:** Three features:
  1. **Offline Detection:** Button says "Register" when online, "Save Registration" when offline. Show/hide the draft notice accordingly.
  2. **Progressive Claim Code:** Only show the claim code field after the student enters a valid 9-digit number AND the server confirms the student was pre-entered by a lecturer.
  3. **Hide claim code by default in XML.**
- **Implementation:**
  - **A. Create `NetworkUtils.java`:**
    ```java
    package com.example.mobileappdev.util;

    import android.content.Context;
    import android.net.ConnectivityManager;
    import android.net.NetworkCapabilities;

    public class NetworkUtils {
        public static boolean isOnline(Context context) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
            return caps != null && (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) 
                                 || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
        }
    }
    ```
  - **B. Hide claim code by default:** In `activity_registration.xml`, set `android:visibility="gone"` on `tilClaimCode`.
  - **C. Update button text and draft notice in `onResume()`:**
    ```java
    @Override
    protected void onResume() {
        super.onResume();
        boolean online = NetworkUtils.isOnline(this);
        MaterialButton btn = findViewById(R.id.btnSaveRegistration);
        View draftNotice = findViewById(R.id.layoutDraftNotice);
        btn.setText(online ? "Register" : "Save Registration");
        draftNotice.setVisibility(online ? View.GONE : View.VISIBLE);
    }
    ```
  - **D. Progressive claim code with debounce:** In `onCreate()`, add:
    ```java
    etStudentNumber.addTextChangedListener(new TextWatcher() {
        Handler h = new Handler(Looper.getMainLooper());
        Runnable r;
        @Override
        public void onTextChanged(CharSequence s, int a, int b, int c) {
            if (r != null) h.removeCallbacks(r);
            r = () -> {
                if (s.length() == 9 && NetworkUtils.isOnline(RegistrationActivity.this)) {
                    checkIfClaimCodeRequired(s.toString());
                } else {
                    findViewById(R.id.tilClaimCode).setVisibility(View.GONE);
                }
            };
            h.postDelayed(r, 500);
        }
        @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
        @Override public void afterTextChanged(Editable s) {}
    });

    private void checkIfClaimCodeRequired(String studentNumber) {
        ApiClient.getAuthService(this).checkStudent(studentNumber).enqueue(new Callback<>() {
            @Override public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<?, ?> data = (Map<?, ?>) response.body().get("data");
                    boolean requires = Boolean.TRUE.equals(data.get("requiresClaimCode"));
                    findViewById(R.id.tilClaimCode).setVisibility(requires ? View.VISIBLE : View.GONE);
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }
    ```
  - **E. Add `checkStudent` method to `AuthApiService.java`:**
    ```java
    @GET("api/v1/auth/check-student/{studentNumber}")
    Call<Map<String, Object>> checkStudent(@Path("studentNumber") String studentNumber);
    ```
- **Deliverable:** A registration screen that adapts to network state and reveals the claim code only when needed.

#### Member 3 — Server: `/check-student` Endpoint
- **Files:** `server/routes/auth.routes.js`
- **Task:** Add a public endpoint that returns whether the given student number exists and requires a claim code.
- **Implementation:** Insert this route in `auth.routes.js`, above the existing routes:
  ```js
  /**
   * GET /api/v1/auth/check-student/:studentNumber
   * Public endpoint. Used by registration to decide whether to show the claim code field.
   */
  router.get('/check-student/:studentNumber', async (req, res) => {
      const { studentNumber } = req.params;
      
      if (!/^\d{9}$/.test(studentNumber)) {
          return res.status(400).json({ 
              success: false, 
              error: { code: 'INVALID_STUDENT_NUMBER', message: 'Must be 9 digits' } 
          });
      }
      
      try {
          const [rows] = await pool.query(
              'SELECT student_id, account_id FROM students WHERE student_number = ? AND deleted_at IS NULL',
              [studentNumber]
          );
          
          if (rows.length === 0) {
              return res.json({ 
                  success: true, 
                  data: { exists: false, requiresClaimCode: false } 
              });
          }
          
          const student = rows[0];
          if (student.account_id !== null) {
              return res.json({ 
                  success: true, 
                  data: { exists: true, requiresClaimCode: false, alreadyRegistered: true } 
              });
          }
          
          return res.json({ 
              success: true, 
              data: { exists: true, requiresClaimCode: true } 
          });
      } catch (err) {
          console.error('check-student error:', err);
          res.status(500).json({ success: false, error: { message: 'Server error' } });
      }
  });
  ```
- **Deliverable:** A public route that Member 2's code can call. Test with Postman before pushing.

---

### 🐞 Group 2: Edit Student & Bottom Sheet Fixes (M4, M5, M6, M7, M8)
**Goal:** Fix all the bugs found in the Edit Student audit.

#### Member 4 — Fix "Correct Student Number" Dead Stub
- **Files:** `StudentSheetBottomSheetDialogFragment.java`
- **Task:** The "Correct Student Number" option currently shows a toast and does nothing. Fix it to open `EditStudentActivity` with the right flags. Pass `group_code` and `base_version` to `EditStudentActivity` so it opens with the correct state.
- **Implementation:**
  - Update `newInstance(...)` to accept `int baseVersion`:
    ```java
    public static StudentSheetBottomSheetDialogFragment newInstance(
            String name, String number, String programme, String group, long studentServerId, int baseVersion) {
        StudentSheetBottomSheetDialogFragment f = new StudentSheetBottomSheetDialogFragment();
        Bundle args = new Bundle();
        args.putString(DeleteStudentActivity.EXTRA_STUDENT_NAME, name);
        args.putString(DeleteStudentActivity.EXTRA_STUDENT_NUMBER, number);
        args.putString(DeleteStudentActivity.EXTRA_STUDENT_PROGRAMME, programme);
        args.putString("student_group", group);
        args.putLong("student_server_id", studentServerId);
        args.putInt("student_base_version", baseVersion);
        f.setArguments(args);
        return f;
    }
    ```
  - Read `baseVersion` in `onCreate`.
  - Update all click listeners to pass the extras:
    ```java
    Intent intent = new Intent(requireContext(), EditStudentActivity.class);
    intent.putExtra(DeleteStudentActivity.EXTRA_STUDENT_NAME, name);
    intent.putExtra(DeleteStudentActivity.EXTRA_STUDENT_NUMBER, number);
    intent.putExtra(DeleteStudentActivity.EXTRA_STUDENT_PROGRAMME, programme);
    intent.putExtra("group_code", group);
    intent.putExtra("base_version", baseVersion);
    intent.putExtra("student_server_id", studentServerId);
    startActivity(intent);
    ```
  - For Correct Number, add `intent.putExtra("trigger_number_correction", true);` before `startActivity`.
- **Deliverable:** Bottom sheet properly opens `EditStudentActivity` with correct data.

#### Member 5 — RosterActivity Updates
- **Files:** `RosterActivity.java`
- **Task:**
  1. Pass `baseVersion` when opening the sheet.
  2. Add a sort toggle to the toolbar (by name vs. student number).
- **Implementation:**
  - Find the line where `StudentSheetBottomSheetDialogFragment.newInstance(...)` is called in `setupRecyclerView()`:
    ```java
    StudentSheetBottomSheetDialogFragment sheet = StudentSheetBottomSheetDialogFragment.newInstance(
            student.name, student.studentNumber, student.programme, student.labGroup, 
            sId, student.baseVersion // NEW
    );
    ```
  - Add sort support:
    - Add a field `private boolean isSortedByName = true;`
    - In `bindViews()`, handle a new `action_sort` menu item:
      ```java
      if (itemId == R.id.action_sort) {
          isSortedByName = !isSortedByName;
          Toast.makeText(this, isSortedByName ? "Sorted by Name" : "Sorted by Number", Toast.LENGTH_SHORT).show();
          applyFiltersAndFilterList();
          return true;
      }
      ```
    - In `applyFiltersAndFilterList()`, before `viewModel.setStudents(filtered)`:
      ```java
      if (isSortedByName) {
          filtered.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
      } else {
          filtered.sort((a, b) -> a.studentNumber.compareToIgnoreCase(b.studentNumber));
      }
      ```
    - Ask **M11** to add `action_sort` to `menu_roster_toolbar.xml`.
- **Deliverable:** Roster passes correct version, has sort toggle.

#### Member 6 — EditStudentActivity Trigger Flag
- **Files:** `EditStudentActivity.java`
- **Task:** Read the `trigger_number_correction` flag from the intent. If true, automatically open the number correction dialog on `onCreate()`. Also, ensure `group_code` and `base_version` are read.
- **Implementation:** At the end of `onCreate()`:
  ```java
  if (getIntent().getBooleanExtra("trigger_number_correction", false)) {
      promptNumberCorrection();
  }
  ```
- **Deliverable:** Tapping "Correct Number" opens the edit screen AND the correction dialog in one go.  
  *(Warning: Do NOT touch `promptNumberCorrection()` or `promptEditDetails()` — Member 11 will refactor those into DialogFragments later).*

#### Member 7 — Fix DeleteStudentActivity for Unsynced Students
- **Files:** `DeleteStudentActivity.java`
- **Task:** Currently, deleting an offline-created student (`serverId = 0`) just shows an error. Fix: delete the local Room row.
- **Implementation:** In `executeDelete()`:
  ```java
  if (studentServerId <= 0) {
      // Delete local-only student from Room
      Executors.newSingleThreadExecutor().execute(() -> {
          StudentEntity local = null;
          for (StudentEntity s : AppDatabase.getInstance(this).studentDao().findAllForAccount(sessionManager.getAccountId())) {
              if (s.studentNumber.equals(getIntent().getStringExtra(EXTRA_STUDENT_NUMBER))) {
                  local = s;
                  break;
              }
          }
          if (local != null) {
              AppDatabase.getInstance(this).studentDao().delete(local);
          }
          runOnUiThread(() -> {
              Toast.makeText(this, "Local student deleted", Toast.LENGTH_SHORT).show();
              setResult(RESULT_OK);
              finish();
          });
      });
      return;
  }
  ```
  *(Note: This assumes `findAllForAccount` exists on the DAO. If not, M13 can add it).*
- **Deliverable:** Delete works for unsynced students.

#### Member 8 — Disable Full Groups in Picker
- **Files:** `GroupPickerBottomSheetDialogFragment.java`
- **Task:** When a group is at capacity (15/15), grey out the card and make it non-clickable.
- **Implementation:** In `fetchGroupCapacities()` after the loop, add:
  ```java
  if (count >= capacity) {
      card.setEnabled(false);
      card.setAlpha(0.5f);
      card.setClickable(false);
  }
  ```
  (Where `card` is `cardGroupG01`, `cardGroupG02`, etc. You'll need to get references to each `MaterialCardView` in `onViewCreated()`).
- **Deliverable:** Full groups are visually disabled.

---

### ⚙️ Group 3: Synchronization & Background (M9, M10)
**Goal:** Automatic sync when network returns. Verify WorkManager is doing its job.

#### Member 9 — Auto-Sync on Network Return
- **Files:** `MainActivity.java`
- **Task:** Register a network callback. When Wi-Fi comes back, immediately trigger `SyncWorker`.
- **Implementation:** In `MainActivity.onCreate()`:
  ```java
  ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
  cm.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
      @Override
      public void onAvailable(@NonNull android.net.Network network) {
          super.onAvailable(network);
          // Only trigger if logged in
          if (SessionManager.getInstance(getApplicationContext()).isLoggedIn()) {
              SyncWorker.triggerManualSync(getApplicationContext());
          }
      }
  });
  ```
  *(Important: Ensure the callback is registered after the session check but before routing to the target activity).*
- **Deliverable:** When a student goes from offline to online, the queue syncs automatically without any manual "Sync Now" tap.

#### Member 10 — Verify SyncWorker Background Behaviour
- **Files:** `SyncWorker.java`
- **Task:**
  1. Verify `schedulePeriodic()` is being called somewhere in the app (likely `MainActivity`).
  2. Verify `triggerManualSync()` from `SyncActivity` fires.
  3. Add a `Result.retry()` on network failure so WorkManager automatically retries.
- **Implementation:**
  - Ensure `SyncWorker.schedulePeriodic(this)` is called in `MainActivity.onCreate()`.
  - In `SyncWorker.doWork()`, ensure the failure path returns `Result.retry()` (not `Result.failure()`), so it retries with exponential backoff.
- **Deliverable:** Documented verification report + a fix if anything's missing.

---

### 🎨 Group 4: UI/UX Polish (M11, M12)
**Goal:** Enhance the look and feel.

#### Member 11 — Menu, Icons, Empty States
- **Files:** `menu_roster_toolbar.xml`, `res/drawable/` (new icons if needed), `activity_roster.xml` (empty state)
- **Task:**
  - Add `action_sort` to `menu_roster_toolbar.xml` (for M5's sort feature).
  - Add a professional empty state view to the roster (icon + message + action button).
  - Add an empty state to `UnassignedActivity`.
- **Deliverable:** Sort menu item + empty state views ready for use.

#### Member 12 — All New Strings
- **Files:** `strings.xml`, `strings_roster.xml`
- **Task:** Add all strings needed by the team:
  - `role_student`, `role_lecturer`
  - `label_lecturer_id`
  - `action_sort`, `sort_by_name`, `sort_by_number`
  - `empty_roster_title`, `empty_roster_message`
  - `empty_unassigned_title`, `empty_unassigned_message`
  - `offline_registration_notice`
  - Any other strings requested by M1, M2, M4, M6, M7, M8, M11.
- **Rule:** This is the **ONLY** member who touches `strings.xml`. All other members must request strings via a group chat message or a comment in `strings.xml`.
- **Deliverable:** All strings are centralized and available.

---

### 🧹 Group 5: Cleanup & Verification (M13, M14)
**Goal:** Fix warnings, verify functionality, no crashes.

#### Member 13 — Room Warnings & Data Integrity
- **Files:** `AppDatabase.java`, `StudentDao.java` (add method if needed for M7)
- **Task:**
  - Fix the Room warning `exportSchema = true` without `room.schemaLocation`:
    Add `room.schemaLocation = "$projectDir/schemas"` to `build.gradle.kts` (or set `exportSchema = false`).
  - Add `findAllForAccount` method to `StudentDao.java` if M7 needs it:
    ```java
    @Query("SELECT * FROM students WHERE accountId = :accountId AND isDeleted = 0")
    List<StudentEntity> findAllForAccount(long accountId);
    ```
- **Deliverable:** Clean build log + DAO method.

#### Member 14 — End-to-End QA
- **Files:** `TEST_REPORT.md`, Postman collection
- **Task:**
  - Test every new feature end-to-end:
    - Role toggle on sign-in
    - Offline registration shows draft notice
    - Claim code appears dynamically
    - Auto-sync on network return
    - Sort toggle works
    - Correct Number opens dialog
    - Delete unsynced student works
    - Full groups are disabled
  - Document results in `TEST_REPORT.md`.
  - Report any regressions back to the owning member.
- **Deliverable:** Updated test report + video/screenshots of the new flows.

---

## 4. Quick Reference Table

| # | Task | Owner | File(s) |
|---|---|---|---|
| 1 | Role toggle (sign-in) | M1 | `SignInActivity`, `activity_signin.xml` |
| 2 | Registration UX + claim code | M2 | `RegistrationActivity`, `activity_registration.xml`, `NetworkUtils.java` |
| 3 | Server `/check-student` | M3 | `auth.routes.js` |
| 4 | Bottom sheet data passing | M4 | `StudentSheetBottomSheetDialogFragment` |
| 5 | Roster updates + sort toggle | M5 | `RosterActivity` |
| 6 | Trigger number correction flag | M6 | `EditStudentActivity` |
| 7 | Delete unsynced students | M7 | `DeleteStudentActivity` |
| 8 | Disable full groups | M8 | `GroupPickerBottomSheetDialogFragment` |
| 9 | Auto-sync on network | M9 | `MainActivity` |
| 10 | SyncWorker verification | M10 | `SyncWorker` |
| 11 | Menu, icons, empty states | M11 | `menu_roster_toolbar.xml`, layouts |
| 12 | All new strings | M12 | `strings.xml` |
| 13 | Room warnings + DAO helper | M13 | `AppDatabase`, `StudentDao` |
| 14 | End-to-end QA | M14 | `TEST_REPORT.md` |

---

## 5. Workflow Rules to Prevent Conflicts
1. **Pull before you start:** Every member runs `git pull origin main` before touching code.
2. **Stay in your lane:** If you need to edit a file you don't own, message the owner.
3. **Small commits:** Commit each sub-task as soon as it compiles. Don't wait for the whole feature.
4. **Push daily:** Half-done work on your local machine blocks nobody.
5. **If a file conflict occurs:** Stop immediately, ping the other owner, and coordinate.
6. **Ask M12 for strings** — do not edit `strings.xml` yourself.
7. **Test before push** — `./gradlew.bat assembleDebug` must pass.

---

## 6. Milestone Breakdown
- **Milestone 1 (Today):** Tasks 1, 2, 3 done. Role toggle + dynamic claim code working.
- **Milestone 2 (Next Day):** Tasks 4, 5, 6, 7, 8 done. All edit/delete bugs fixed.
- **Milestone 3 (Final):** Tasks 9, 10, 11, 12, 13, 14 done. Auto-sync, sort toggle, strings, QA.

---
*(Task matrix successfully updated and synchronized across the project roadmap.)*
