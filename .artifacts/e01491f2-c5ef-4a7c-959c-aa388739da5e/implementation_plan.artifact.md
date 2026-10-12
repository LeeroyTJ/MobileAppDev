# Room Database LiveData Reactive Observer Queries Implementation Plan

Provide comprehensive LiveData reactive observer queries in Room DAOs (`StudentDao.java`, `PendingOperationDao.java`) and wire them through viewmodels (`RosterViewModel.java`, `SyncViewModel.java`, etc.) to support asynchronous reactive data streaming without blocking the main UI thread.

## User Review Required

> [!IMPORTANT]
> This plan adds filtered and reactive Room queries for students and pending operations, enabling non-blocking background queries.

## Proposed Changes

### Room Data Access Objects (DAOs)

#### [MODIFY] [StudentDao.java](file:///C:/Users/El%20CUNTO/AndroidStudioProjects/MobileAppDev/android/app/src/main/java/com/example/mobileappdev/data/local/dao/StudentDao.java)
- Add reactive filtered queries:
  - `observeFilteredForAccount(long accountId, String search, String programme, String group)` returning `LiveData<List<StudentEntity>>`.

#### [MODIFY] [PendingOperationDao.java](file:///C:/Users/El%20CUNTO/AndroidStudioProjects/MobileAppDev/android/app/src/main/java/com/example/mobileappdev/data/local/dao/PendingOperationDao.java)
- Verify `observeQueueForAccount(long accountId)` is fully optimized for reactive observers.

### ViewModels

#### [MODIFY] [RosterViewModel.java](file:///C:/Users/El%20CUNTO/AndroidStudioProjects/MobileAppDev/android/app/src/main/java/com/example/mobileappdev/viewmodel/RosterViewModel.java)
- Use `Transformations.switchMap` to bind filter parameters and search queries to Room's reactive `observeFilteredForAccount` query.

## Verification Plan

### Automated Tests
- Run unit tests with `./gradlew testDebugUnitTest`.
