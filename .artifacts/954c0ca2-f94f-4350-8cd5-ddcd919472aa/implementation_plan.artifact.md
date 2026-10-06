# Refactor SyncWorker and Add Sync Service (Task 8.1)

Refactor `SyncWorker.java` to inject active `accountId` from `SessionManager`, add `getSyncService` to `ApiClient.java`, and execute background synchronization with operation UUIDs for idempotent execution.

## User Review Required

> [!IMPORTANT]
> - **Task 8.1**: Connect `SyncWorker.java` to `SessionManager` (`getAccountId()`) and `ApiClient` (`getSyncService()`) to execute background sync via Retrofit and Room.

## Proposed Changes

### Android Local Storage / Sync

#### [MODIFY] [ApiClient.java](file:///C:/Users/El%20CUNTO/AndroidStudioProjects/MobileAppDev/android/app/src/main/java/com/example/mobileappdev/data/remote/ApiClient.java)
- Add `getSyncService(Context context)` method returning `SyncApiService`.

#### [MODIFY] [SyncWorker.java](file:///C:/Users/El%20CUNTO/AndroidStudioProjects/MobileAppDev/android/app/src/main/java/com/example/mobileappdev/sync/SyncWorker.java)
- Inject active `accountId` using `SessionManager.getInstance(getApplicationContext()).getAccountId()`.
- Uncomment and implement Retrofit API call via `ApiClient.getSyncService(getApplicationContext())`.
- Pass operation UUIDs (`operationId`) in `SyncOperation` for idempotent execution.

## Verification Plan

### Automated Tests
- Build Android project (`app:assembleDebug`) to verify compilation.
