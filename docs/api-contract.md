# CohortHub API Contract v1

**Course:** ICT361 Mobile Application Development  
**Backend:** Node.js + Express  
**Database:** MySQL/InnoDB  
**API:** REST/JSON  
**Base path:** `/api/v1`

## 1. Purpose

This is the shared contract between Backend-1 (schema/database), Backend-2 (authentication + student CRUD), and Backend-3 (groups + search/filter + synchronization). Android and Testing/Integration should use it as the source of truth.

The contract is aligned with the lab requirements: registration/login, student CRUD, group assignment, search/filter, sync, role/ownership checks, unique student numbers, versions, deletion metadata, and a maximum of 15 active students per group. The lab requires Android to communicate with the API rather than directly with MySQL. fileciteturn4file0L30-L36

---

# 2. Reviewed Decisions

## Project
**CohortHub**

## Programmes
Only:

```text
CS
IT
DS
```

The lab specifies these stored programme choices. fileciteturn5file0L60-L63

## Groups
Only:

```text
G01
G02
G03
G04
```

Each group has a maximum of **15 active students**. Groups may mix programmes. fileciteturn5file0L60-L67

## Student number
A required **nine-digit string**. Leading zeroes must be preserved; outer whitespace may be trimmed; embedded spaces are invalid; MySQL must enforce uniqueness. fileciteturn5file0L57-L61

## Student identity
`student_id` is immutable and independent of `student_number`. Correcting a student number must not create a new student. fileciteturn5file0L63-L65

## Deletion
Use soft deletion. The server must hide the student, release the group place once, disable the account, retain a deletion marker, and keep the student number reserved. fileciteturn5file0L68-L69

## Registration
Students verify ownership with lecturer-seeded claim codes. If a lecturer already created the student, registration links the account to that profile instead of creating a duplicate. fileciteturn5file0L70-L76

---

# 3. Authentication

Protected endpoints use:

```http
Authorization: Bearer <access_token>
```

The server determines the role from the authenticated account. Never trust a client-supplied role.

Roles:

```text
STUDENT
LECTURER
```

The server must check identity, role, and record ownership on protected requests. Students must not access another student's record or the full roster. fileciteturn5file0L70-L76

---

# 4. Standard Responses

Success:

```json
{
  "success": true,
  "data": {}
}
```

Collection:

```json
{
  "success": true,
  "data": [],
  "meta": {
    "page": 1,
    "pageSize": 20,
    "total": 42
  }
}
```

Error:

```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable explanation"
  }
}
```

HTTP statuses:

| Status | Meaning |
|---|---|
| 200 | Success |
| 201 | Created |
| 204 | Success, no body |
| 400 | Bad request |
| 401 | Not authenticated |
| 403 | Not authorized |
| 404 | Not found |
| 409 | Conflict |
| 422 | Validation failure |
| 500 | Server error |

Dates use ISO-8601 UTC.

Student numbers are always JSON strings, e.g. `"012345678"`.

---

# 5. Authentication — Backend-2

## POST `/auth/register`

**Role:** Public

Creates or links a student account after claim-code verification.

Request:

```json
{
  "claimCode": "ABC123",
  "studentNumber": "012345678",
  "password": "student-password"
}
```

The server must verify the claim code, verify the student number, link an existing lecturer-created profile when present, otherwise create the profile, create/link exactly one account, and hash the password.

Response `201`:

```json
{
  "success": true,
  "data": {
    "accessToken": "token",
    "expiresIn": 3600,
    "account": {
      "accountId": 12,
      "username": "012345678",
      "role": "STUDENT"
    },
    "student": {
      "studentId": 25,
      "studentNumber": "012345678",
      "firstName": "John",
      "lastName": "Banda",
      "programme": {
        "programmeId": 1,
        "code": "CS",
        "name": "Computer Science"
      },
      "group": null,
      "version": 1
    }
  }
}
```

Registration does not reserve a group place.

Possible errors:

```text
INVALID_CLAIM_CODE
CLAIM_CODE_EXPIRED
CLAIM_CODE_USED
REGISTRATION_MISMATCH
STUDENT_ALREADY_REGISTERED
VALIDATION_ERROR
```

## POST `/auth/login`

**Role:** Public

Request:

```json
{
  "username": "012345678",
  "password": "student-password"
}
```

Response:

```json
{
  "success": true,
  "data": {
    "accessToken": "token",
    "expiresIn": 3600,
    "account": {
      "accountId": 12,
      "username": "012345678",
      "role": "STUDENT"
    }
  }
}
```

## GET `/auth/me`

**Role:** Student/Lecturer

Returns the authenticated account.

---

# 6. Student CRUD — Backend-2

## POST `/students`

**Role:** Lecturer

Request:

```json
{
  "studentNumber": "012345678",
  "firstName": "John",
  "lastName": "Banda",
  "programmeId": 1
}
```

Response `201` contains the created student with `studentId`, programme, group, `version`, `deletedAt`, `createdAt`, and `updatedAt`.

Errors:

```text
STUDENT_NUMBER_EXISTS
PROGRAMME_NOT_FOUND
VALIDATION_ERROR
```

## GET `/students/:studentId`

**Role:** Student (own record only), Lecturer

Returns the student profile, programme, confirmed group, version and timestamps.

## PUT `/students/:studentId`

**Role:** Student (own permitted fields), Lecturer

Request:

```json
{
  "firstName": "John",
  "lastName": "Banda",
  "programmeId": 1,
  "baseVersion": 4,
  "operationId": "uuid"
}
```

Success:

```json
{
  "success": true,
  "data": {
    "studentId": 25,
    "version": 5,
    "updatedAt": "2026-09-24T14:10:00Z"
  }
}
```

If `baseVersion` is stale:

```text
409 VERSION_CONFLICT
```

The server must not silently overwrite newer work.

## DELETE `/students/:studentId`

**Role:** Lecturer

Request:

```json
{
  "baseVersion": 5,
  "operationId": "uuid"
}
```

Deletion is soft deletion and must also disable the account and release the group place exactly once.

## PATCH `/students/:studentId/student-number`

**Role:** Lecturer

Request:

```json
{
  "studentNumber": "012345679",
  "baseVersion": 5,
  "operationId": "uuid"
}
```

`studentId` never changes.

---

# 7. Student Group-Change Request

## POST `/students/:studentId/group-requests`

**Role:** Student (own record only)

Request:

```json
{
  "groupId": 2,
  "operationId": "uuid"
}
```

Response:

```json
{
  "success": true,
  "data": {
    "status": "PENDING",
    "studentId": 25,
    "requestedGroupId": 2
  }
}
```

`PENDING` does **not** reserve a group place. The server decides whether the request can be accepted.

---

# 8. Lecturer Roster/Search — Backend-3

## GET `/students`

**Role:** Lecturer only

Query parameters:

```text
search
programmeId
groupId
unassigned
page
pageSize
```

Example:

```http
GET /students?search=John&programmeId=1&groupId=2&page=1&pageSize=20
```

All supplied filters use AND semantics.

`search` searches at least:

```text
first_name
last_name
student_number
```

The lab requires search by name/student number and combined programme/group filtering. fileciteturn5file0L51-L56

Response:

```json
{
  "success": true,
  "data": [
    {
      "studentId": 25,
      "studentNumber": "012345678",
      "firstName": "John",
      "lastName": "Banda",
      "programme": {
        "programmeId": 1,
        "code": "CS",
        "name": "Computer Science"
      },
      "group": {
        "groupId": 2,
        "groupCode": "G02"
      },
      "version": 4
    }
  ],
  "meta": {
    "page": 1,
    "pageSize": 20,
    "total": 1
  }
}
```

Unassigned:

```http
GET /students?unassigned=true
```

Only active students are returned.

---

# 9. Reference Data — Backend-3

## GET `/programmes`

Response contains:

```text
CS — Computer Science
IT — Information Technology
DS — Data Science
```

## GET `/groups`

Lecturer response:

```json
{
  "success": true,
  "data": [
    {
      "groupId": 1,
      "groupCode": "G01",
      "name": "Lab Group 01",
      "capacity": 15,
      "studentCount": 13,
      "remainingCapacity": 2
    }
  ]
}
```

## GET `/groups/:groupId`

Returns group capacity information. Lecturer may receive the roster; a student receives only safe information about their own confirmed group.

---

# 10. Group Assignment — Backend-3

## POST `/groups/:groupId/assign`

**Role:** Lecturer

Request:

```json
{
  "studentId": 25,
  "baseVersion": 4,
  "operationId": "uuid"
}
```

Success:

```json
{
  "success": true,
  "data": {
    "studentId": 25,
    "groupId": 1,
    "version": 5
  }
}
```

If full:

```text
409 GROUP_FULL
```

## POST `/groups/:groupId/transfer`

**Role:** Lecturer

Request:

```json
{
  "studentId": 25,
  "baseVersion": 5,
  "operationId": "uuid"
}
```

Success:

```json
{
  "success": true,
  "data": {
    "studentId": 25,
    "previousGroupId": 1,
    "groupId": 2,
    "version": 6
  }
}
```

A failed transfer must leave the old group unchanged. fileciteturn5file0L63-L67

---

# 11. Group Capacity Transaction Rule

This applies to assignment, transfer, group requests and equivalent sync mutations.

Starting state:

```text
G01 = 14/15
```

Two clients request the final place.

Required:

```text
Client A → SUCCESS
Client B → GROUP_FULL
```

Never allow 16 active students.

The server must use a MySQL transaction with appropriate locking (or an equivalently safe mechanism). Counting and inserting as separate unprotected operations is not sufficient. The lab requires repeating this test 20 times. fileciteturn4file0L62-L67

---

# 12. Synchronization — Backend-3

## POST `/sync`

**Role:** Authenticated user

Request:

```json
{
  "operations": [
    {
      "operationId": "uuid",
      "operationType": "UPDATE_STUDENT",
      "entityType": "STUDENT",
      "entityId": 25,
      "accountId": 12,
      "baseVersion": 4,
      "payload": {
        "firstName": "John",
        "lastName": "Banda",
        "programmeId": 1
      }
    }
  ]
}
```

The authenticated token, not the request's `accountId`, is the authority for identity.

Allowed operation types:

```text
CREATE_STUDENT
UPDATE_STUDENT
DELETE_STUDENT
CORRECT_STUDENT_NUMBER
ASSIGN_GROUP
TRANSFER_GROUP
REQUEST_GROUP_CHANGE
```

Response:

```json
{
  "success": true,
  "data": {
    "results": [
      {
        "operationId": "uuid",
        "status": "APPLIED",
        "entityType": "STUDENT",
        "entityId": 25,
        "version": 5
      }
    ]
  }
}
```

Statuses:

```text
APPLIED
CONFLICT
REJECTED
```

---

# 13. Idempotency

Every retriable mutation has a unique:

```text
operationId
```

The server stores an operation receipt/result durably.

Same operation ID + same content:

```text
return the stored result
```

Same operation ID + different content:

```text
OPERATION_CONTENT_MISMATCH
```

This is required because the lab explicitly tests a lost response followed by retry, including after a server restart. Only one logical effect may exist. fileciteturn4file0L68-L71

---

# 14. Version Conflicts

Every synchronizable student has:

```text
version
```

Every mutation carries:

```text
baseVersion
```

Example:

```text
client = 4
server = 4
→ accept
→ server becomes 5
```

But:

```text
client = 4
server = 5
→ 409 VERSION_CONFLICT
```

Response:

```json
{
  "success": false,
  "error": {
    "code": "VERSION_CONFLICT",
    "message": "The record has changed on the server."
  },
  "data": {
    "entityType": "STUDENT",
    "entityId": 25,
    "clientVersion": 4,
    "serverVersion": 5,
    "serverRecord": {}
  }
}
```

Android maps this to:

```text
Action required
```

The local proposal must remain available for review rather than being silently overwritten. fileciteturn4file0L57-L58

---

# 15. Pull Changes

## GET `/sync/changes`

**Role:** Authenticated user

Example:

```http
GET /sync/changes?since=2026-09-24T13:00:00Z
```

Response:

```json
{
  "success": true,
  "data": {
    "students": [],
    "deletions": [],
    "nextCursor": "cursor-value"
  }
}
```

Only authorized data is returned.

Deletion markers are included so an offline device cannot resurrect a remotely deleted student. fileciteturn4file0L59-L61

---

# 16. Account Scoping

Synchronization is scoped to the authenticated account.

When a session expires:

```text
pause synchronization
```

Never replay Account A's pending operations under Account B.

This is required by the lab's account-switching/session rules. fileciteturn4file0L60-L61

---

# 17. Validation

### Name

```text
required
2–100 trimmed characters
ordinary punctuation allowed
accented letters allowed
not unique
```

### Student number

```text
required
exactly 9 digits
unique
leading zeroes preserved
no embedded spaces
```

### Programme

```text
CS
IT
DS
```

### Groups

```text
G01
G02
G03
G04
```

or `NULL` for unassigned.

These rules are enforced on the server; Android may also validate locally for early feedback. fileciteturn5file0L57-L67

---

# 18. Error Codes

## Authentication

```text
INVALID_CREDENTIALS
ACCOUNT_DISABLED
TOKEN_EXPIRED
UNAUTHORIZED
FORBIDDEN
```

## Registration

```text
INVALID_CLAIM_CODE
CLAIM_CODE_EXPIRED
CLAIM_CODE_USED
REGISTRATION_MISMATCH
STUDENT_ALREADY_REGISTERED
```

## Validation

```text
VALIDATION_ERROR
INVALID_STUDENT_NUMBER
INVALID_STUDENT_NAME
INVALID_PROGRAMME
INVALID_GROUP
```

## Student

```text
STUDENT_NOT_FOUND
STUDENT_NUMBER_EXISTS
STUDENT_DELETED
```

## Group

```text
GROUP_NOT_FOUND
GROUP_FULL
STUDENT_ALREADY_ASSIGNED
INVALID_GROUP_TRANSFER
```

## Sync

```text
VERSION_CONFLICT
REMOTE_DELETED
DUPLICATE_OPERATION_ID
OPERATION_CONTENT_MISMATCH
SYNC_REJECTED
```

## General

```text
NOT_FOUND
INTERNAL_ERROR
```

---

# 19. Authorization Matrix

| Endpoint | Student | Lecturer |
|---|---:|---:|
| POST `/auth/register` | ✓ | — |
| POST `/auth/login` | ✓ | ✓ |
| GET `/auth/me` | ✓ | ✓ |
| POST `/students` | — | ✓ |
| GET `/students/:id` | Own | ✓ |
| PUT `/students/:id` | Own permitted fields | ✓ |
| DELETE `/students/:id` | — | ✓ |
| PATCH `/students/:id/student-number` | — | ✓ |
| POST `/students/:id/group-requests` | Own | — |
| GET `/students` | — | ✓ |
| GET `/programmes` | ✓ | ✓ |
| GET `/groups` | Own/limited | ✓ |
| GET `/groups/:id` | Own/limited | ✓ |
| POST `/groups/:id/assign` | — | ✓ |
| POST `/groups/:id/transfer` | — | ✓ |
| POST `/sync` | ✓ | ✓ |
| GET `/sync/changes` | Own authorized data | ✓ |

---

# 20. Database/API Mapping

| API concept | Database concept |
|---|---|
| Account | `accounts` |
| Student | `students` |
| Programme | `programmes` |
| Lab group | `lab_groups` |
| Claim code | `claim_codes` |
| Operation receipt | `sync_operations` |
| Conflict version | `students.version` |
| Remote deletion | `students.deleted_at` |
| Group membership | `students.group_id` |
| Ownership | `students.account_id` |
| Student uniqueness | unique `students.student_number` |

Use `lab_groups`, not a table called `groups`, for clarity and to avoid SQL keyword ambiguity.

---

# 21. Transaction Requirements

## Student deletion

Atomically:

```text
soft-delete student
+
disable account
+
release group membership
+
increment version
+
record deletion marker
```

## Assignment

Atomically:

```text
lock/check group
+
verify capacity
+
verify student
+
assign student
+
increment version
```

## Transfer

Atomically:

```text
lock/check destination
+
verify capacity
+
change membership
+
increment version
```

Any failure:

```text
ROLLBACK
```

The old state remains intact.

---

# 22. Parameterized SQL

Never concatenate user input into SQL.

Bad:

```js
`SELECT * FROM students WHERE student_number = '${studentNumber}'`
```

Good:

```js
connection.execute(
  'SELECT * FROM students WHERE student_number = ?',
  [studentNumber]
);
```

The lab requires parameterized SQL and server-side validation. fileciteturn4file0L30-L36

---

# 23. Pagination

`GET /students` must support:

```text
page
pageSize
```

Defaults:

```text
page = 1
pageSize = 20
```

Recommended maximum:

```text
100
```

Activity D explicitly requires lecturer lists to be paginated. fileciteturn4file0L37-L40

---

# 24. Android Synchronization Mapping

The Android app should map:

```text
Room save
   ↓
Saved locally
   ↓
Pending
   ↓
POST /sync
   ↓
Syncing
   ↓
APPLIED
   ↓
Synced
```

Errors such as:

```text
VERSION_CONFLICT
GROUP_FULL
REMOTE_DELETED
SYNC_REJECTED
```

become:

```text
Action required
```

The lab requires the visible states Saved locally, Pending, Syncing, Synced and Action required. fileciteturn4file0L48-L58

---

# 25. Backend Ownership

## Backend-1

Owns:

- ERD
- `schema.sql`
- database constraints
- indexes
- foreign keys
- version/deletion model
- database/API consistency review

## Backend-2

Owns:

- `/auth/register`
- `/auth/login`
- `/auth/me`
- `POST /students`
- `GET /students/:id`
- `PUT /students/:id`
- `DELETE /students/:id`
- `PATCH /students/:id/student-number`
- authentication
- authorization middleware
- claim-code verification
- password hashing
- student validation

## Backend-3

Owns:

- `GET /students`
- `GET /programmes`
- `GET /groups`
- `GET /groups/:id`
- `POST /groups/:id/assign`
- `POST /groups/:id/transfer`
- `POST /students/:id/group-requests`
- `POST /sync`
- `GET /sync/changes`
- capacity transactions
- search/filter
- idempotency
- conflict handling

---

# 26. Testing Contract

Testing must cover:

### Authentication
- valid login
- invalid password
- disabled account
- expired token
- role enforcement

### Students
- registration
- lecturer CRUD
- duplicate number
- invalid number
- invalid programme
- ownership

### Groups
- assignment
- transfer
- full group
- simultaneous final-place requests
- failed transfer

### Search
- name
- student number
- programme
- group
- unassigned
- combined filters
- pagination

### Sync
- offline create
- offline edit
- offline delete
- retry
- lost response
- duplicate operation
- version conflict
- remote deletion
- group becoming full
- account switching

These correspond to the lab's minimum demonstration checklist. fileciteturn4file0L82-L94

---

# 27. Acceptance Tests

## Challenge 1 — Final group place

Start:

```text
G01 = 14 active students
```

Two clients request the final place simultaneously.

Required:

```text
one SUCCESS
one GROUP_FULL
never 16 students
```

Repeat 20 times and show final database counts. fileciteturn4file0L62-L67

## Challenge 2 — Lost response

```text
POST mutation
→ server commits
→ response interrupted
→ same operationId retried
→ server restarted
→ retry again
```

Required:

```text
one logical effect
```

fileciteturn4file0L68-L71

## Challenge 3 — Offline conflict

Two devices cache the same student.

One edits online; the other edits an old version offline.

Required:

```text
VERSION_CONFLICT
```

Also test a group becoming full while offline and remote student deletion.

No deleted student may be resurrected. fileciteturn4file0L72-L75

---

# 28. Contract Freeze Checklist

Before implementation:

- [ ] Backend-1 reviewed database/API mapping.
- [ ] Backend-2 reviewed authentication and CRUD.
- [ ] Backend-3 reviewed groups/search/sync.
- [ ] Testing reviewed endpoint/error behavior.
- [ ] Android team received the contract.
- [ ] All required lab functions have an endpoint.
- [ ] Request and response fields are agreed.
- [ ] Error codes are agreed.
- [ ] Version handling is agreed.
- [ ] Operation IDs are agreed.
- [ ] Capacity transaction behavior is agreed.
- [ ] Soft deletion is agreed.
- [ ] Authorization matrix is agreed.
- [ ] Pagination is agreed.
- [ ] Contract is marked **API v1 — FROZEN**.

After this point, breaking changes should be documented and communicated to Android and Testing.

---

# 29. What Happens Next

The recommended sequence is:

```text
API CONTRACT
     ↓
Backend-1 schema review
     ↓
Backend-2 authentication/CRUD
     +
Backend-3 groups/search/sync
     ↓
API tests
     ↓
Android Retrofit integration
     ↓
Room integration
     ↓
WorkManager sync
     ↓
Concurrency/conflict tests
     ↓
Full integration
```

Do not have Android call undocumented or improvised endpoints. The contract should be the shared agreement before implementation begins.