# CohortHub - Project Task Matrix & Team Implementation Roadmap

**Course:** ICT361 Mobile Application Development (Mulungushi University)  
**Project:** CohortHub - Student Registration & Lab Group Management System  
**Reference Document:** `ICT361_Android_Group_Lab.pdf`

---

## 1. Executive Status Overview

- **Current Build Status:** **BUILD SUCCESSFUL** (`:app:assembleDebug` & `:app:testDebugUnitTest`)
- **Parallel Work Strategy:** Divided into Phase 1 (Backend - zero Android file risk), Phase 2 (Shared Android Infrastructure - unified menus & API interfaces), and Phase 3 (Isolated Screen Features). Each teammate has a non-overlapping component to avoid Git merge conflicts.

---

## 2. Parallel Implementation Roadmap & Bug Fix Assignments

### Phase 1: Server-Side Fixes (No Android File Conflicts)

#### Member 11: Josephat Lungu (202403000) — Backend Auth & Student Endpoints
- `[ ]` **Fix 1A & 1B (`server/routes/auth.routes.js`, `server/routes/student.routes.js`):**
  - Add `GET /api/v1/auth/me` returning authenticated user profile and role.
  - Add `POST /api/v1/students/:id/group-requests` checking target group capacity via `SELECT ... FOR UPDATE` row locking.
  - Add `PATCH /api/v1/students/:id/student-number` inserting requests into `correction_requests` or `sync_operations`.
  - Add `POST /api/v1/students` (lecturer create) and `GET /api/v1/students/:id`.
  - Move `const connection = await pool.getConnection()` inside `try` blocks to prevent unhandled connection leaks.

#### Member 12: Joyce Gondwe (202410082) — Backend Groups, Sync & Roster Endpoints
- `[ ]` **Fix 1A, 1C & 1D (`server/routes/group.routes.js`, `server/routes/sync.routes.js`):**
  - Add `GET /api/v1/groups/:groupId` returning group capacity, member list, and programme code breakdown (`SELECT p.code, COUNT(*) ...`).
  - Add `GET /api/v1/sync/changes?since=...` returning incremental delta changes.
  - Accept string group codes in roster filter (`if (group && group !== 'UNASSIGNED') { SELECT group_id FROM lab_groups WHERE group_code = ? }`).
  - Add `version = version + 1` increments to `DELETE`, group `assign`, and group `transfer` routes.

---

### Phase 2: Shared Android Infrastructure (Completed Before Screen Work)

#### Member 6: Foster Namukanzye (202407636) — Navigation Architecture & Base Activity
- `[ ]` **Fix 2A (`BaseNavActivity.java`, `bottom_nav_menu.xml`, `menu_bottom_nav.xml`):**
  - Merge student and lecturer bottom navigation menus into a single `bottom_nav_menu.xml`.
  - Create `BaseNavActivity.java` extending `AppCompatActivity` to handle `BottomNavigationView` role-based item visibility (Lecturer: Students/More; Student: Home/Group/Sync/Profile).
  - Use `FLAG_ACTIVITY_REORDER_TO_FRONT` without `finish()` to keep screens alive in the back stack.

#### Member 7: Antonette Kapinga (202401120) — Shared Retrofit API Contracts
- `[ ]` **Fix 2B (`StudentApiService.java`, `GroupApiService.java`, `SyncApiService.java`, `AuthApiService.java`):**
  - Add `@GET("api/v1/students/{id}")`, `@POST("api/v1/students")`, `@PATCH("api/v1/students/{id}/student-number")`, and `@POST("api/v1/students/{id}/group-requests")` to `StudentApiService`.
  - Add `@GET("api/v1/groups/{groupId}")` to `GroupApiService`.
  - Add `@GET("api/v1/sync/changes")` to `SyncApiService`.
  - Add `@GET("api/v1/auth/me")` to `AuthApiService`.

#### Member 4: Ezekiel Judge — Registration Claim Code Input
- `[ ]` **Fix 2C (`activity_registration.xml`, `RegistrationActivity.java`):**
  - Add `TextInputEditText` (`etClaimCode`) with hint `"Claim code (optional, for pre-registered students)"` in `activity_registration.xml`.
  - Read `claimCode` in `RegistrationActivity.java` and send it in the register request payload body when non-empty.

---

### Phase 3: Individual Screen Fixes (Parallel Screen Tasks)

#### Member 1: Henry Mapulanga (202402976) — Profile & Sync Status Real-Time
- `[ ]` **Fix 3A (`ProfileActivity.java`):**
  - Save `System.currentTimeMillis()` in `SharedPreferences` on profile fetch and display formatted string in `tvLastUpdatedValue`.
  - Observe `PendingOperationDao.observeQueueForAccount()` LiveData — display "Pending" when local operations exist.
  - Wire `rowRequestGroupChange` to `GroupPickerBottomSheetDialogFragment` and call `StudentApiService.requestGroupChange()`.
  - Wire `rowRequestNumberCorrection` dialog to `StudentApiService.patchStudentNumber()`.

#### Member 3: Emmanuel Sikubeka — Student Group Detail & Programme Breakdown
- `[ ]` **Fix 3B (`StudentGroupActivity.java`, `activity_group_detail.xml`):**
  - Remove hardcoded "G02" fallback on API failure.
  - Replace `getRoster()` with `GroupApiService.getGroup(groupId)` to load real group capacity, member list, and programme breakdown.
  - Format programme breakdown with full names (`Computer Science: 5`, `Information Technology: 4`, `Data Science: 2`).
  - Populate `progressCapacity` and `tvPlacesRemainingPill` from live group capacity metrics.

#### Member 8: Sylvester Chansa (202406183) — Edit Student Activity & Capacity Spinner
- `[ ]` **Fix 3C (`EditStudentActivity.java`):**
  - Populate group spinner dynamically from `GroupApiService.listGroups()` showing real occupancy.
  - Wire `changeGroup` to `GroupApiService.assignStudent()` / `transferStudent()`, handling 409 `GROUP_FULL`.
  - Wire `correctStudentNumber` to `StudentApiService.patchStudentNumber()` with local 9-digit regex validation.
  - Wire `editDetails` to `StudentApiService.updateStudent()`, handling 409 `VERSION_CONFLICT`.
  - Read `student_server_id` extra for all API invocations.

#### Member 9: Andrew Kalengo (202406795) — Delete Student Activity Integration
- `[ ]` **Fix 3D (`DeleteStudentActivity.java`):**
  - Read `student_server_id` extra passed from student details sheet and edit details screen.
  - Wire `btnDelete` to `StudentApiService.deleteStudent()`.
  - Display progress spinner, disable action buttons during network processing, and delete from local Room DB on success or offline fallback.

#### Member 2: Nathan Kamfwa (202408447) — More Sheet & Group Overview Fragment
- `[ ]` **Fix 3E (`bottom_sheet_more.xml`, `MoreBottomSheetDialogFragment.java`, `GroupOverviewBottomSheetDialogFragment.java`):**
  - Remove `optionUnassigned` block from `bottom_sheet_more.xml` and click listener from `MoreBottomSheetDialogFragment.java`.
  - Create `GroupOverviewBottomSheetDialogFragment.java` and `bottom_sheet_group_overview.xml`.
  - Display all 4 lab groups with capacity progress bars, occupancy text, places remaining pills, and programme breakdowns from `GroupApiService`.

#### Member 5: Neo Maseba (202407198) — Sync Timeline & Pending Operations Queue
- `[ ]` **Fix 3F (`SyncActivity.java`, `activity_sync.xml`):**
  - Add RecyclerView observing `PendingOperationDao.observeQueueForAccount(accountId)`.
  - Display operation type, student info, status, and timestamp in real-time.
  - Display "Last synced" timestamp from `SharedPreferences`.
  - Add student request section (group changes and student number corrections).

#### Member 10: Joel Nduba (202408068) — Room Local Storage Sync Helpers
- `[ ]` **Room Database Helpers (`StudentDao.java`, `PendingOperationDao.java`):**
  - Provide LiveData reactive observer queries to support `BaseNavActivity`, `RosterActivity`, and screen viewmodels without blocking main UI thread.

#### Member 13: Thabo Jumbe (202406168) — Team Lead & Quality Assurance
- `[ ]` **Task 13.1:** Execute parallel build checks (`:app:assembleDebug`) and unit test suite (`:app:testDebugUnitTest`).
- `[ ]` **Task 13.2:** Verify end-to-end integration across Phase 1, Phase 2, and Phase 3.
- `[ ]` **Task 13.3:** Compile final submission package (`app-debug.apk`, `schema.sql`, setup `README.md`, AI log) and record group video demonstration.

---

## 3. Parallel Conflict Avoidance Summary

| Phase | Responsibility | Scope | Risk of Merge Conflicts |
|---|---|---|---|
| **Phase 1** | Server API & MySQL | `server/routes/*.js` | **NONE** (No Android file touches) |
| **Phase 2** | Shared Navigation & Interfaces | `BaseNavActivity.java`, API interfaces, `bottom_nav_menu.xml` | **NONE** (Executed before screen work) |
| **Phase 3** | Screen Features | `ProfileActivity`, `StudentGroupActivity`, `EditStudentActivity`, `DeleteStudentActivity`, `MoreBottomSheet`, `SyncActivity` | **NONE** (Each teammate owns a distinct screen file) |
