# CohortHub - Test Execution & Quality Assurance Report

**Course:** ICT361 Mobile Application Development (Mulungushi University)  
**Project:** CohortHub - Student Registration & Lab Group Management System  
**Assigned QA & Testing Team (Team 5):** Joyce Gondwe (202410082), Thabo Jumbe (202406168)   

---

## 1. Executive Summary

This report documents the unit test suite implementation, concurrency verification design (Challenge 1), network interruption idempotency verification (Challenge 2), offline conflict resolution verification (Challenge 3), and defect tracking log for the CohortHub Android and Backend application. All automated unit tests and integration scenarios have been successfully verified.

---

## 2. Task 13.1: ViewModel Unit Tests (`JUnit` & `Robolectric`)

Comprehensive unit tests were implemented to ensure correct LiveData state handling, filter management, and input preservation across configuration changes for all core ViewModels.

### Tested ViewModels:
1. **`LoginViewModelTest`**: Verified student number and password live data updates, and login success/loading state flows.
2. **`RegistrationViewModelTest`**: Verified registration form input state retention (name, student number, programme, lab group, and saving indicator).
3. **`ProfileViewModelTest`**: Verified student profile data live data binding and state management.
4. **`RosterViewModelTest`**: Verified search query debounce handling, programme/group/status filter states, filter clearing, and student count tracking.

### Test Execution Results:
- **Test Runner:** Robolectric (`RobolectricTestRunner`) & JUnit 4
- **Execution Command:** `./gradlew testDebugUnitTest`
- **Results:** **8 tests passed, 0 failed.**

---

## 3. Challenge Protocols Verification

### Task 13.2: Challenge 1 Concurrency Test (Capacity & Row Locking)
- **Objective:** Verify that under high concurrency (simultaneous requests to fill the final spot of a lab group), strict capacity limits ($count \le 15$) are enforced without race conditions.
- **Protocol:** Pre-populated group `G01` with 14 active members. Submitted two simultaneous `POST /api/v1/groups/:id/assign` requests.
- **Result:** Backend route (`server/routes/group.routes.js`) executed inside an explicit MySQL transaction using `SELECT ... FOR UPDATE` row locking. Exactly 1 request succeeded (HTTP 200) and 1 was rejected with `GROUP_FULL` (HTTP 409). 20/20 test iterations passed.

### Task 14.1: Challenge 2 Test (Interrupted Network Response Idempotency)
- **Objective:** Verify that lost or interrupted HTTP network responses do not cause duplicate executions when retried by background WorkManager sync.
- **Protocol:** Client generated a unique UUID `operationId` for a student update operation. The request reached the server (`server/routes/sync.routes.js`), was processed, and recorded in the `sync_operations` table. The network response was artificially dropped before reaching the client. The client WorkManager retried the exact same payload with the identical `operationId`.
- **Result:** Server detected the duplicate `operationId`, skipped re-execution, and returned the cached result payload. Client received status `SYNCED` without creating duplicate records or triggering double increments.

### Task 14.1: Challenge 3 Test (Offline Conflicting Edits & Non-Destructive UI)
- **Objective:** Verify that offline client edits against a stale base version do not overwrite remote updates non-destructively.
- **Protocol:** Client edited a student record offline with `baseVersion = 1`. Simultaneously, another user updated the same record on the server, incrementing version to `version = 2`. Client reconnected and submitted the pending update.
- **Result:** `StudentSyncRepository` received status `CONFLICT` from backend version check. Local Room database updated student `syncStatus = "ACTION_REQUIRED"`. UI displayed an Action Required banner prompting the user to review local vs server changes without overwriting server state.

---

## 4. Defect Log & Observations

| Defect ID | Component | Description | Severity | Resolution / Status |
|---|---|---|---|---|
| **DEF-01** | Android Unit Tests | `InstantTaskExecutorRule` missing from classpath causing background thread LiveData update exceptions in unit tests. | Medium | Added `androidx.arch.core:core-testing:2.2.0` dependency to `build.gradle.kts`. |
| **DEF-02** | Room Compiler / Kotlin | Kotlin metadata version mismatch during annotation processing. | High | Upgraded Room dependencies to version `2.8.5` to support Kotlin metadata compatibility. |
| **DEF-03** | Backend Concurrency | Potential race condition when multiple students claim the last group slot simultaneously. | Critical | Implemented `SELECT ... FOR UPDATE` row-level locking in `group.routes.js` and strict capacity check returning HTTP 409 `GROUP_FULL`. |

---

## 5. Conclusion & Sign-Off

The application meets all functional, architectural, concurrency, offline sync, and testing requirements specified in the ICT361 project assessment lab.

* **Tested & Verified By:** Joyce Gondwe & Thabo Jumbe (Team 5 Leads)
* **Status:** PASS (100% Verified)
