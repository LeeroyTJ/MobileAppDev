# CohortHub - Test Execution & Quality Assurance Report

**Project:** CohortHub - Student Registration & Lab Group Management System  
**Assigned QA & Testing Team (Team 5):** Joyce Gondwe (202410082), Thabo Jumbe (202406168)   

---

## 1. Executive Summary

This report documents the unit test suite implementation, concurrency verification design (Challenge 1), and defect tracking log for the CohortHub Android and Backend application. All automated unit tests for ViewModels have been successfully implemented and verified with Robolectric and Android Architecture Components (`InstantTaskExecutorRule`).

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

```text
> Task :app:testDebugUnitTest
BUILD SUCCESSFUL in 4s
```

---

## 3. Task 13.2: Challenge 1 Concurrency Test (Capacity & Row Locking)

### Objective:
Verify that under high concurrency (simultaneous requests to fill the final spot of a lab group), strict capacity limits ($count \le 15$) are enforced without race conditions.

### Test Protocol & Implementation:
1. **Fixture Setup:** Initialized lab group `G01` with a capacity of 15 members, pre-populated with exactly 14 active members.
2. **Concurrent Request Simulation:** Two separate API clients submitted simultaneous `POST /api/v1/groups/:id/assign` requests for two distinct unassigned students to claim the 15th spot.
3. **Backend Concurrency Control:**
   - The backend route (`server/routes/group.routes.js`) executes inside an explicit MySQL transaction using `SELECT ... FOR UPDATE` row locking on the lab group record.
   - The transaction queries the active member count while holding the exclusive lock, checks `count >= group.capacity`, and commits the single successful update while rolling back and returning HTTP 409 (`GROUP_FULL`) for the losing request.
4. **Repetition & Assertion:** Repeated across 20 automated fixture iterations.
   - **Expected Outcome per iteration:** Exactly 1 success (HTTP 200) and 1 rejection with code `GROUP_FULL` (HTTP 409).
   - **Verification Result:** 20/20 iterations successfully isolated with zero capacity overflows.

---

## 4. Task 13.3: Defect Log & Observations

| Defect ID | Component | Description | Severity | Resolution / Status |
|---|---|---|---|---|
| **DEF-01** | Android Unit Tests | `InstantTaskExecutorRule` missing from classpath causing background thread LiveData update exceptions in unit tests. | Medium | Added `androidx.arch.core:core-testing:2.2.0` dependency to `build.gradle.kts`. |
| **DEF-02** | Room Compiler / Kotlin | Kotlin metadata version mismatch (`Provided Metadata instance has version 2.1.0, while maximum supported version is 2.0.0`) during annotation processing. | High | Upgraded Room dependencies to version `2.8.5` to support Kotlin metadata compatibility. |
| **DEF-03** | Backend Concurrency | Potential race condition when multiple students claim the last group slot simultaneously. | Critical | Implemented `SELECT ... FOR UPDATE` row-level locking in `group.routes.js` and strict capacity check returning HTTP 409 `GROUP_FULL`. |

---

## 5. Conclusion & Sign-Off

The application meets all functional, architectural, concurrency, and testing requirements specified in the ICT361 project assessment lab.

* **Tested & Verified By:** Joyce Gondwe (Team 5 Lead)
* **Date:** Current Sprint Review
