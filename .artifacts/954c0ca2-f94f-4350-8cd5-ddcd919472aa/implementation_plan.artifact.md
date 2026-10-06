# Backend Role-Based Access Control and Student Authorization (Tasks 11.1 & 11.2)

Implement robust role-based middleware (`verifyRole`) and strict ownership validation to protect all backend endpoints and secure student data access.

## User Review Required

> [!IMPORTANT]
> - **Task 11.1**: Create a centralized middleware module (`server/middleware/auth.js`) providing `authenticateToken` and `verifyRole(role)` (supporting `'LECTURER'` and `'STUDENT'`).
> - **Task 11.2**: Ensure students are strictly restricted to reading and editing their own profile/records (`WHERE account_id = req.user.id`), preventing unauthorized access to other student records.

## Open Questions
- None. The API contract and current route definitions fully support centralized middleware.

## Proposed Changes

### Backend Middleware & Routes

#### [NEW] [auth.js](file:///C:/Users/El%20CUNTO/AndroidStudioProjects/MobileAppDev/server/middleware/auth.js)
- Create a dedicated authentication and authorization middleware file exporting:
  - `authenticateToken`: Verifies JWT token and attaches `req.user`.
  - `verifyRole(allowedRole)` (or `verifyRole(...roles)`): Verifies that `req.user.role` matches the required role (e.g. `'LECTURER'`, `'STUDENT'`).

#### [MODIFY] [student.routes.js](file:///C:/Users/El%20CUNTO/AndroidStudioProjects/MobileAppDev/server/routes/student.routes.js)
- Import `authenticateToken` and `verifyRole` from `../middleware/auth`.
- Update endpoints (roster GET, profile GET, update PUT, delete DELETE) to use `verifyRole('LECTURER')` where appropriate and enforce `WHERE account_id = req.user.id` for students.

#### [MODIFY] [group.routes.js](file:///C:/Users/El%20CUNTO/AndroidStudioProjects/MobileAppDev/server/routes/group.routes.js)
- Import `authenticateToken` and `verifyRole` from `../middleware/auth`.
- Protect group assignment and transfer routes.

#### [MODIFY] [sync.routes.js](file:///C:/Users/El%20CUNTO/AndroidStudioProjects/MobileAppDev/server/routes/sync.routes.js)
- Import `authenticateToken` and `verifyRole` from `../middleware/auth`.
- Protect sync endpoints with appropriate role checks.

## Verification Plan

### Automated Tests
- Start server and run automated verification queries or test scripts.
- Verify that non-lecturers cannot access lecturer-only endpoints (e.g., student roster listing or batch deletions) and receive `403 FORBIDDEN`.
- Verify that students can only access/modify their own records (`account_id = req.user.id`).

### Manual Verification
- Test login with lecturer and student accounts in the Android app and verify role-based navigation and access permissions.
