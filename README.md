# CohortHub

Android student registration and lab group management app, built for ICT361 Mobile Application Development.

CohortHub lets students register, view their lab group, and request changes to their profile, while lecturers manage the full student roster — adding, editing, searching, filtering, and assigning students to lab groups, all synced between the Android app and a MySQL-backed server.

---

## Features

- Student registration with claim-code linking to lecturer pre-entered records
- Student profile view: name, programme, lab group, and sync status
- Lecturer roster with combined search, programme, and group filters
- Role-based access control (student vs. lecturer) enforced on every request
- Lab group capacity enforcement (max 15 students per group), safe under concurrent requests
- Offline-first local storage with background sync and conflict detection
- Soft deletion with reserved student numbers

---

## Tech stack

| Layer | Technology |
|---|---|
| Android | Java, XML Views, ViewModel/LiveData, Room, Retrofit/OkHttp, WorkManager |
| Backend | Node.js, Express |
| Database | MySQL (InnoDB) |

---

## Project structure

```
CohortHub/
├── android/          Android Studio project (Java/XML Views)
├── server/           Node.js/Express backend
├── database/         Schema and seed data
├── docs/             Wireframes, ER diagram, API contract
├── CONTRIBUTIONS.md
```

---

## Getting started

### Prerequisites

- Android Studio (latest stable)
- Node.js (LTS)
- MySQL 8.0+

### Backend setup

```bash
cd server
npm install
cp .env.example .env   # fill in your local DB credentials and a JWT secret
```

Load the schema:
```bash
mysql -u root -p < ../database/schema.sql
```

Start the server:
```bash
npm run dev
```

### Android setup

1. Open the `android/` folder in Android Studio
2. Let Gradle sync
3. Point the app's API base URL at your local server (see `RetrofitClient`)
4. Run on an emulator or device

---

## Team

Built by a group of 15, organized into five teams of three:

| Team | Responsibility |
|---|---|
| UI/UX | Screens, layouts, accessibility |
| Android Architecture | ViewModels, navigation, app structure |
| Local Storage/Sync | Room database, offline queue, WorkManager |
| Backend/Database | REST API, MySQL schema, transactions |
| Testing/Integration | Retrofit integration, test coverage, API testing |


---