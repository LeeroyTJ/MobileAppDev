# Task List - Local Storage & Background Sync (Member 8: Ezekiel)

- [x] **Task 8.1:** Refactor `SyncWorker.java` to inject active `accountId` from `SessionManager` and pass operation UUIDs for idempotent execution.
- [x] **Task 8.2:** Implement durable sync result storage in `PendingOperationDao`: store server responses (`APPLIED`, `CONFLICT`, `REJECTED`) so lost network responses do not cause duplicate server executions on retry.
- [x] **Task 8.3:** Connect `SyncViewModel.java` to expose LiveData sync status (`Saved locally`, `Pending`, `Syncing`, `Synced`, `Action required`) to the UI
