# CohortHub — Run on Your Phone and Test Everything

Work through the parts in order. Don't start Part C until Part B passes; each part depends on the one before it. Record Pass/Fail in the table at the end as you go, since that table doubles as your test evidence.

---

## Part A — Pre-flight (laptop)

### A1. Pull the latest code and resolve the Gradle conflict
```bash
git checkout main
git pull origin main
```
Make sure `build.gradle.kts` has no `<<<<<<<` / `=======` / `>>>>>>>` markers left, then in Android Studio: **File → Sync Project with Gradle Files**. It must finish with no errors.

### A2. Server dependencies and config
```bash
cd server
npm install
```
Confirm `server/.env` exists (not just `.env.example`) with:
```env
DB_HOST=localhost
DB_USER=root
DB_PASSWORD=<your mysql password>
DB_NAME=cohorthub
JWT_SECRET=<generated random string>
PORT=3000
```

### A3. Confirm every route is mounted in `server/server.js`
You should see all four, uncommented:
```javascript
app.use('/api/v1/auth', authRoutes);
app.use('/api/v1/students', studentRoutes);
app.use('/api/v1/groups', groupRoutes);
app.use('/api/v1/sync', syncRoutes);
```

### A4. Reset the database and load test data
From the repo root:
```bash
mysql -u root -p < database/schema.sql
mysql -u root -p cohorthub < database/dev_seed.sql
```
Put `create-lecturer.js` in `server/scripts/`, then from `server/`:
```bash
node scripts/create-lecturer.js 900000001 Lecturer@123
```
Verify in MySQL:
```sql
SELECT g.group_code, COUNT(s.student_id) AS occupied
FROM lab_groups g LEFT JOIN students s
  ON s.group_id = g.group_id AND s.deleted_at IS NULL
GROUP BY g.group_code;
```
Expected: G01 = 14, G02 = 5, G03 = 3, G04 = 0.

*(Everything in `schema.sql` is dropped and recreated each time, so repeat all three steps whenever you want a clean slate.)*

### A5. Start the server and check it's alive
```bash
cd server
npm run dev
```
Expected log: `CohortHub backend running on port 3000`. Then in another terminal:
```bash
curl -X POST http://localhost:3000/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"900000001","password":"Lecturer@123"}'
```
Expected: `200` with an `accessToken` and `"role":"LECTURER"`.

---

## Part B — Phone connection

Your laptop's IP changes when you switch networks, so redo this whenever you change Wi-Fi.

1. **Find your IP:** `ipconfig` → IPv4 Address (e.g. `192.168.1.42`).
2. **`ApiClient.java`:** set `BASE_URL = "http://192.168.1.42:3000/";`
3. **`res/xml/network_security_config.xml`:** the `<domain>` must be that same IP.
4. **`AndroidManifest.xml`:** `<application android:networkSecurityConfig="@xml/network_security_config" ...>` and **every Activity declared**: `RegistrationActivity`, `SignInActivity`, `ProfileActivity`, `RosterActivity`, `UnassignedActivity`, `EditStudentActivity`, `DeleteStudentActivity`, `SyncActivity`, `ConnectionErrorActivity`, `MainActivity`. An undeclared Activity crashes the app the moment something navigates to it.
5. **Firewall (once, as Administrator):**
   ```
   netsh advfirewall firewall add rule name="Node Server 3000" dir=in action=allow protocol=TCP localport=3000
   ```
6. **Same network:** phone and laptop on the same Wi-Fi (or the phone on your laptop's hotspot).
7. **Phone browser check:** open `http://192.168.1.42:3000/api/v1/groups` (type `http://` explicitly). A JSON `Token missing` error means the network path works. A "can't reach this page" error means stop and fix the network first.
8. **Install:** USB debugging on, select the phone in Android Studio, press Run. Or build `./gradlew assembleDebug` and copy `android/app/build/outputs/apk/debug/app-debug.apk` to the phone.

**Debugging tools for the rest of this guide:**
- **Server side:** watch the `npm run dev` terminal. Every error is printed there.
- **App side:** Android Studio → Logcat → filter by your package name `com.example.mobileappdev`.

---

## Part C — Student flow (on the phone)

| # | Test | Steps | Expected |
|---|---|---|---|
| C1 | Valid registration | Register: name "Test One", number `202412345`, programme CS, password `Password1` | Success message, goes to Sign-in. In MySQL: 1 row in `students`, 1 in `accounts` |
| C2 | Duplicate number | Register again with `202412345` | Rejected with a clear message. Still exactly 1 row in MySQL |
| C3 | Bad number format | Register with `12345`, then with `2024 12346` (embedded space) | Rejected both times, typed fields are kept |
| C4 | Bad name | Register with name `A` | Rejected (2–100 chars) |
| C5 | Wrong password | Sign in with `202412345` + wrong password | "Invalid" message, no crash |
| C6 | Sign in | Correct credentials | Profile shows the real name, number and programme (not placeholder text) |
| C7 | Session persists | Force-close the app and reopen | Goes straight to Profile |
| C8 | Rotation | On Registration, type a name, rotate the phone | Typed text is still there |
| C9 | Claim-code flow | Register number `202401099` **without** a claim code | Rejected: claim code required |
| C10 | Claim-code link | Register `202401099` with code `TESTCODE123` | Success. In MySQL: still **1** row for `202401099`, now with an `account_id` |
| C11 | Code reuse | Register a different student trying code `TESTCODE123` again | Rejected |
| C12 | Student can't see roster | While signed in as a student, try to open the roster | Blocked (see D2 for the server-side check) |

Rotation check in MySQL (C10):
```sql
SELECT student_id, student_number, account_id FROM students WHERE student_number = '202401099';
```

---

## Part D — Permissions (curl from the laptop, this is the security part)

Get tokens first:
```bash
# lecturer
curl -s -X POST http://localhost:3000/api/v1/auth/login -H "Content-Type: application/json" \
  -d '{"username":"900000001","password":"Lecturer@123"}'
# student
curl -s -X POST http://localhost:3000/api/v1/auth/login -H "Content-Type: application/json" \
  -d '{"username":"202412345","password":"Password1"}'
```
Copy the tokens into `LECT` and `STUD` (PowerShell: `$env:LECT="..."`).

| # | Test | Command | Expected |
|---|---|---|---|
| D1 | Lecturer sees roster | `curl http://localhost:3000/api/v1/students -H "Authorization: Bearer $LECT"` | `200`, list of 26+ students |
| D2 | **Student must NOT see the roster** | same command with `$STUD` | **`403`**. If this returns `200`, that is a real security bug |
| D3 | Student can't edit someone else | `PUT /api/v1/students/1` with `$STUD`, body `{"studentName":"Hijack","baseVersion":1}` | `403` |
| D4 | Student can't delete | `DELETE /api/v1/students/1` with `$STUD` | `403` |
| D5 | No token | any protected route without a header | `401` |

---

## Part E — Lecturer flow (on the phone)

Sign in with `900000001` / `Lecturer@123`.

| # | Test | Expected |
|---|---|---|
| E1 | Roster loads | 26+ students with names, numbers, programme, group |
| E2 | Search by name | Typing "Faith" shows both Faith Sikaonga records after ~300 ms |
| E3 | Search by number | `202401004` shows exactly one student |
| E4 | Combined filters | Programme CS + Group G01 shows only CS students in G01. Result count matches |
| E5 | Unassigned filter/screen | Shows `202401023`, `024`, `025`, `099` |
| E6 | Edit a student | Change a name, save. Roster shows the new name. MySQL `version` incremented by 1 |
| E7 | Delete with cancel | Open delete confirmation, press Cancel. Student still present |
| E8 | Delete confirmed | Delete a student. Disappears from roster. MySQL `deleted_at` is set and the group count drops by exactly 1 |
| E9 | Deleted number stays reserved | Try registering the deleted student's number | Rejected. Soft-deleted numbers stay blocked |
| E10 | Share sheet | Use the roster's share action | Preview shows only group labels and counts. **No names or student numbers** |
| E11 | Pagination | Only checkable once you have more students than one page | Next page loads, no duplicates |

---

## Part F — Group capacity (the Challenge 1 evidence)

### F1. From the phone
As lecturer, assign Unassigned student `202401023` to G01 (now 15/15). Then try assigning `202401024` to G01.
**Expected:** first succeeds, second shows a "group full" message, G01 stays at 15.

### F2. The real race test (two simultaneous requests)
Reset the DB (A4) so G01 is back at 14. Get two unassigned student IDs:
```sql
SELECT student_id FROM students WHERE student_number IN ('202401024','202401025');
```
Save `test-concurrent-assign.js` in `server/`, then:
```bash
node test-concurrent-assign.js <G01_group_id> <studentIdA> <studentIdB> $LECT
```
**Expected:** `PASS — exactly one succeeded, one got GROUP_FULL`.

### F3. Repeat 20 times from a reset fixture
Reset (A4) before each run. Save the terminal output as Challenge 1 evidence.
```sql
SELECT COUNT(*) FROM students WHERE group_id = <G01_id> AND deleted_at IS NULL;
```
**Expected:** exactly 15 after every run, never 16.

### F4. Failed transfer keeps the old group
Fill G01 to 15, then transfer a G02 student into G01.
**Expected:** rejected with `GROUP_FULL`, and the student's `group_id` is unchanged.

### F5. Repeated deletion releases one place only
Delete the same student twice (second via curl). **Expected:** second returns `404`, group count dropped by 1 total.

---

## Part G — Offline and sync

| # | Test | Steps | Expected |
|---|---|---|---|
| G1 | Save offline | Turn on airplane mode, edit a student's name as lecturer | Saved locally, status **Pending**. Room has a pending operation |
| G2 | Survives restart | Force-close the app (still offline), reopen | The edit and the pending status are still there |
| G3 | Sync on reconnect | Turn airplane mode off, press **Sync** | Status goes Syncing then **Synced**. MySQL has the new name, `version` +1 |
| G4 | Exactly-once retry | Send the same operation twice (curl `POST /api/v1/sync` with the same `operationId`) | Second returns `APPLIED` but `version` only went up once |
| G5 | Offline create + delete | Offline: create a student then delete it before syncing | Nothing reaches the server; no stray record |
| G6 | Conflict | Two devices (or phone + curl): cache a student, edit it via curl, then edit it offline on the phone and sync | Phone shows **Action required**; the server value is not overwritten |
| G7 | Remote deletion | Delete a student via curl, then edit the same student offline on the phone and sync | Edit rejected, student is **not** resurrected |
| G8 | Session expiry/logout | Log out, sign in as another user | No previous user's data or queue is visible |

---

## Part H — Accessibility

| # | Test | Expected |
|---|---|---|
| H1 | Font at 200% | Settings → Display → Font size: largest. Open every screen: no cut-off text or unreachable buttons |
| H2 | TalkBack | Turn on TalkBack, move through Registration and Sign-in: every field and button is announced with a meaningful label |
| H3 | Touch targets | Buttons and list rows are comfortably tappable (about 48dp or more) |
| H4 | Error text | Validation errors appear next to the field, not only as a disappearing Toast |
| H5 | Not colour-only | Sync/capacity states show text or icons as well as colour |

---

## Results Table

| Test | Pass/Fail | Notes / Evidence |
|---|---|---|
| C1–C12 Student flow | | |
| D1–D5 Permissions | | |
| E1–E11 Lecturer flow | | |
| F1–F5 Group capacity | | |
| G1–G8 Offline/sync | | |
| H1–H5 Accessibility | | |
