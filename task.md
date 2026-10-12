# CohortHub - Project Task Matrix & Team Implementation Roadmap

**Course:** ICT361 Mobile Application Development (Mulungushi University)  
**Project:** CohortHub - Student Registration & Lab Group Management System  
**Reference Document:** `ICT361_Android_Group_Lab.pdf`

---

## 1. Executive Status Overview

- **Completed Progress:** **38 out of 38 Sub-Tasks (100% COMPLETE)**
- **Remaining Leftover Tasks:** **0 Sub-Tasks (0%)**
- **Build Status:** **BUILD SUCCESSFUL** (`:app:assembleDebug` & `:app:testDebugUnitTest`)

---

## 3. Individual Task Matrix by Teammate

### Team 1: UI/UX & Accessibility (100% COMPLETE)

#### Member 1: Henry Mapulanga (202402976)
- `[x]` **Task 1.3:** Test screen layouts (`activity_registration`, `activity_signin`, `activity_profile`, `activity_roster`, `activity_unassigned`) at 200% font size scaling to ensure no text clipping occurs.

#### Member 2: Nathan Kamfwa (202408447)
- `[x]` **Task 2.1:** Implement `FilterBottomSheetDialogFragment.java` inflating `bottom_sheet_filters.xml` for advanced roster filtering.

#### Member 3: Emmanuel Sikubeka
- `[x]` **Task 3.2:** Wire dropdown adapters for Programme (`All`, `CS`, `IT`, `DS`), Lab Group (`All`, `G01`-`G04`, `Unassigned`), and Sync Status (`All`, `Synced`, `Pending`, `Action Required`).

#### Member 4: Ezekiel Judge
- `[x]` **Task 4.3:** Add clear and apply button click listeners to update `RosterViewModel` filter properties and dismiss the bottom sheet.

#### Member 5: Neo Maseba (202407198)
- `[x]` **Task 5.3:** Conduct TalkBack screen reader accessibility testing across all activities and verify form input retention.

---

### Team 2: Android Architecture & Navigation (100% COMPLETE)

#### Member 6: Foster Namukanzye (202407636)
- `[x]` **Task 6.2:** Implement Android Share Sheet feature in `RosterActivity`: export a sanitized lecturer group summary (e.g., `"Lab Group G01: 12/15 places filled"`) excluding all personal student numbers/names per Activity D requirements.

---

### Team 4: Backend API, MySQL & Concurrency (100% COMPLETE)

---

### Team 5: Testing, Integration & Quality Assurance (100% COMPLETE)

#### Team Lead / Co-Tester: Thabo Jumbe (202406168)
- `[x]` **Task 14.1:** Conduct **Challenge 2 & 3 Tests** (interrupted network response idempotency and offline conflicting edit handling).
- `[x]` **Task 14.2:** Prepare final submission package: Android app source, Node.js backend source, MySQL `schema.sql`, compiled `app-debug.apk`, API contract, setup `README.md`, and AI contribution log.
- `[x]` **Task 14.3:** Coordinate 5-8 minute group video demonstration covering end-to-end user flows, offline sync, and database assertions.

---

## 4. Final Submission Checklist

| Task Description | Target File / Component | Status |
|---|---|---|
| Implement `FilterBottomSheetDialogFragment.java` inflating `bottom_sheet_filters.xml` with programme, group, and status dropdown adapters | `FilterBottomSheetDialogFragment.java` | **COMPLETED** |
| Add Android Share Sheet export feature in `RosterActivity` for sanitized lecturer group summaries | `RosterActivity.java` | **COMPLETED** |
| Perform 200% text size scaling verification across all 5 screen layouts | Layout testing | **COMPLETED** |
| Conduct TalkBack accessibility screen reader audit across all screens | Accessibility testing | **COMPLETED** |
| Execute Challenge 2 & 3 test protocols and document results | `TEST_REPORT.md` | **COMPLETED** |
| Assemble final submission package (`app-debug.apk`, `schema.sql`, setup `README.md`, AI log) & record group video demo | Project submission | **COMPLETED** |
