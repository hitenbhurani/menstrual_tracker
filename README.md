# FemCare

FemCare is an Android menstrual health tracker built with a hybrid data model:
- Firebase (cloud backup + auth + real-time sync), and
- Room/SQLite (fast local cache + offline continuity).

The project currently includes:
- Android app (`app/`), and
- Node.js backend (`backend/`) used for modular API experiments.

Public repository:
- https://github.com/hitenbhurani/menstrual_tracker

Maintainer profile:
- https://github.com/hitenbhurani

This project is open for anyone to clone and use for learning, experimentation, and portfolio work.

## Project status

This repository is actively updated and currently includes:
- cycle tracking and prediction flow,
- daily symptom and water logging,
- calendar notes,
- in-app + system notifications,
- WorkManager-based background engine,
- local medical report capture/upload/save flow,
- Room database tables for offline persistence,
- Firebase integration for cloud persistence and authentication.

## Quick setup for anyone (clone and run)

### Prerequisites
- Git
- Android Studio Ladybug or newer
- JDK 11
- Android SDK (compile/target 36)
- Node.js 18+ and npm
- A Firebase project (Auth + Firestore enabled)
- Google Maps API key

### 1) Clone the repository
```bash
git clone https://github.com/hitenbhurani/menstrual_tracker.git
cd menstrual_tracker
```

### 2) Android Firebase setup
- Place your Firebase config file at:
  - `app/google-services.json`
- In your Firebase console, make sure these are enabled:
  - Authentication providers you need (email/password, Google)
  - Firestore database

### 3) Local Android properties
Add these values in `local.properties` (do not commit secrets):
- `sdk.dir=YOUR_ANDROID_SDK_PATH`
- `MAPS_API_KEY=YOUR_GOOGLE_MAPS_KEY`

### 4) Backend setup
```bash
cd backend
npm install
```

Create a `.env` file in `backend/` with at least:
```env
PORT=5000
```

### 5) Run backend
```bash
cd backend
npm run dev
```

### 6) Build and run Android app
```bash
./gradlew :app:assembleDebug
```

Then run the app from Android Studio on emulator/device.

### 7) First validation checklist
- Register/login works
- Dashboard opens
- Track + Calendar save flows work
- Notifications appear
- Room database entries are visible in Database Inspector

## Core features implemented

### Authentication and account flow
- Email/password login with Firebase Auth.
- Google sign-in support.
- On login, Firestore user profile is synced into local Room cache.

### Hybrid storage model (Firebase + SQLite)
- Firestore acts as source of cloud truth for user and cycle-related data.
- Room (`femcare_database`) keeps local user/cycle data for offline use and fast reads.
- Save paths use cloud-first semantics where required, then update local state.
- Room warm-up is triggered on app startup so Database Inspector can attach reliably.

### Cycle tracking and predictions
- Onboarding flow captures baseline cycle details.
- Calendar marks expected period, fertile window, and ovulation phases.
- Track screen supports daily symptom/mood/discharge logging and hydration count.
- Historical logs and trend visualization are available.

### Notes and history
- Date-specific notes can be attached from calendar.
- Recent notes section shows latest entries quickly.

### Notifications and background scheduler
- Central scheduler with periodic and immediate modes.
- Daily log reminders, cycle alerts, and weekly wellness checks.
- Debounced immediate sync to reduce churn.
- Auth-failure-aware fallback behavior to avoid retry storms.

### Medical report module (capture/upload/save)
- Users can capture report image via camera or pick image/PDF from storage.
- Selected report is saved to app external files directory.
- Metadata history is tracked in SharedPreferences.
- Existing UI currently shows saved entry history, not a file gallery view.

### PDF export
- Detailed report PDF generation and secure sharing via FileProvider.

### Nearby help module
- Find doctor/clinic style utility using maps/location stack.

## Technical architecture

### Android app
- Language: Java
- UI: XML + Material Components
- App structure: Activity + Fragment modular flow
- Persistence:
  - Room (`user_table`, `cycle_table`)
  - SharedPreferences for lightweight flags/history
- Cloud:
  - Firebase Auth
  - Firebase Firestore
  - Firebase Messaging (FCM service registered)
- Background execution:
  - WorkManager (`BackgroundTaskScheduler`, `ScheduledNotificationWorker`)
- Networking:
  - Retrofit + Gson
  - Volley (present)
- Visualization:
  - MPAndroidChart
- Image loading:
  - Glide

### Backend module (`backend/`)
- Runtime: Node.js + Express
- Dependencies: express, cors, dotenv
- Current route groups:
  - `/health`
  - `/api/logs`
  - `/api/tips`
  - `/api/posts`
  - `/api/requests`
  - `/api/saved`
- Note: current backend controllers use in-memory arrays (non-persistent) for experiment/demo behavior.

## Build and runtime configuration

### Android prerequisites
- Android Studio Ladybug or newer recommended
- JDK 11
- Android SDK compile/target set to 36
- `google-services.json` in `app/`

### Local properties
Add values in `local.properties` (not committed):
- `MAPS_API_KEY=your_key`

### Firebase
Ensure your Firebase project has:
- Authentication providers enabled (email/password, Google as needed)
- Firestore database configured
- Valid API key and matching app config

### Build command
```bash
./gradlew :app:assembleDebug
```

### Run backend
```bash
cd backend
npm install
npm run dev
```

## Data storage details

### Cloud data
- User profile, cycle/log/notes, notifications are stored in Firestore collections under `users/{email}` paths.

### Local database
- Room database name: `femcare_database`
- Tables currently used:
  - `user_table`
  - `cycle_table`

### Local medical report files
- Saved under app external files pictures directory in `FemCareReports`.
- Temporary captures are created in `FemCareReportsTemp` before save.

## Experiments coverage snapshot

This project demonstrates practical coverage across:
- activities + navigation,
- fragments,
- local storage (SharedPreferences + Room/SQLite),
- cloud integration (Firebase),
- background tasks (WorkManager),
- notifications + FCM,
- media capture/pick + secure file sharing,
- API integration through a Node backend module.

## Future implementations roadmap

The following are planned and currently missing or partially implemented:

### 1) Cloudinary-backed report storage (no UI redesign)
- Keep existing Medical Reports screen flow unchanged.
- On save, retain local file copy (current behavior) and upload in background to Cloudinary.
- Persist cloud metadata (`secure_url`, `public_id`, `status`) alongside local metadata.
- Use backend-signed uploads for security (no API secret in Android app).

### 2) In-app report viewer gallery
- Add view layer to open saved image/PDF files directly from history.
- Support open/share/delete actions for saved reports.

### 3) Durable backend persistence
- Replace in-memory backend arrays with persistent storage (e.g., MongoDB/PostgreSQL/Firebase Admin).
- Add auth/ownership checks for backend routes.

### 4) Offline sync queue and conflict handling
- Queue mutations while offline.
- Replay and reconcile when connectivity returns.
- Add deterministic conflict rules per data type.

### 5) Security hardening
- Encrypt sensitive local report references or move to scoped-private internal storage where needed.
- Tighten Firestore Security Rules for user-scoped read/write guarantees.
- Add backend request validation and rate limiting.

### 6) Test coverage and CI
- Unit tests for repositories/DAOs.
- Instrumentation tests for core flows.
- CI pipeline for build + lint + tests.

### 7) Observability and reliability
- Structured logging for background worker outcomes.
- Crash and performance monitoring integration.
- Notification delivery diagnostics.

### 8) UX polish backlog (without functional regressions)
- Optional incremental UX improvements after stability locks:
  - report history search/filter,
  - richer trends dashboard,
  - accessibility and localization improvements.

## Known limitations

- Medical report history currently shows metadata entries, not direct embedded file previews in a list.
- Backend module is not yet production-grade persistent by default.
- Some features are optimized for emulator/development workflow and require environment keys.

## Contribution

This repository is primarily a project/learning build, but structured improvements are welcome.

## Author

Hiten Bhurani
- GitHub Profile: https://github.com/hitenbhurani

