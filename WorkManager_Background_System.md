# FemCare Background WorkManager System

## Purpose
This project uses WorkManager to run reliable background checks for:
- daily log reminders,
- cycle and ovulation alerts,
- weekly wellness insights,
- and immediate sync after important user actions.

The system is designed to be cloud-first, resilient, and low-noise when Firebase auth is temporarily broken.

## Main Components

### 1) BackgroundTaskScheduler
File: app/src/main/java/com/miniflo/femcare/BackgroundTaskScheduler.java

Responsibilities:
- Defines all unique work names.
- Schedules periodic work with network constraints.
- Enqueues one-time immediate sync work.
- Debounces immediate sync calls (20 seconds).
- Defers immediate sync for 2 minutes after known Firebase auth-token failures.

Periodic jobs:
- Daily log reminder: every 24h, aligned to 20:00.
- Cycle alerts: every 6h.
- Weekly wellness: every 7 days, aligned to Monday 09:00.

### 2) ScheduledNotificationWorker
File: app/src/main/java/com/miniflo/femcare/ScheduledNotificationWorker.java

Responsibilities:
- Executes work in one of four modes: daily, cycle, weekly, immediate.
- Reads user data from Firestore.
- Publishes notifications via NotificationPublisher.
- Uses shared auth-error handling:
  - non-recoverable auth/token issues do not retry forever,
  - auth failure state is marked for scheduler backoff,
  - successful runs clear auth failure state.

### 3) NotificationPublisher
File: app/src/main/java/com/miniflo/femcare/NotificationPublisher.java

Responsibilities:
- Writes notification documents to:
  users/{email}/notifications/{notificationId}
- Optionally shows Android system notifications.
- Uses local fallback storage if cloud is unreachable.
- Prevents duplicate system notifications using SharedPreferences flags.

### 4) LocalNotificationStore
File: app/src/main/java/com/miniflo/femcare/LocalNotificationStore.java

Responsibilities:
- Keeps a local JSON cache of notifications for offline visibility.
- Supports upsert, mark-read, mark-all-read, and delete.

### 5) FirebaseAuthState
File: app/src/main/java/com/miniflo/femcare/FirebaseAuthState.java

Responsibilities:
- Detects auth/token-related Firebase failures by exception message patterns.
- Persists last auth failure timestamp.
- Exposes short-term backoff decision (`shouldDeferBackgroundWork`).

## Work Modes

### MODE_DAILY
- Checks if user missed daily log (respects pref_alert_log).
- Removes stale "missed log" alert when log exists.
- Publishes daily motivation quote.
- Publishes BMI insight when outside normal range.

### MODE_CYCLE
- Computes day-in-cycle from saved cycle data.
- Publishes period-soon, period-start, fertile window, and ovulation alerts based on preferences and cycle day.

### MODE_WEEKLY
- Publishes weekly motivation.
- Publishes weekly BMI insight when relevant.

### MODE_IMMEDIATE
- Runs daily + cycle + weekly checks in one pass.
- Triggered by user events (login, dashboard open, saving cycle/log updates, settings toggles, etc.).

## Scheduling and Trigger Points

Scheduler entry points in app flow:
- FemCareApplication.onCreate -> scheduleAll()
- DashboardActivity.onCreate -> scheduleAll() and enqueueImmediateSync("dashboard_open")
- LoginActivity (when routing to Dashboard) -> scheduleAll() and enqueueImmediateSync("login_success")
- LastPeriodActivity (after period data save) -> scheduleAll() and enqueueImmediateSync("period_data_saved")
- CalendarFragment (after cycle update success) -> scheduleAll() and enqueueImmediateSync("cycle_data_updated")
- TrackFragment (after daily log cloud-ack success) -> enqueueImmediateSync("daily_log_saved" or "daily_log_updated")
- SettingsFragment (when reminder toggles change) -> enqueueImmediateSync(...) for each preference

## Cloud-First Save Contract

Track and Calendar saves follow this sequence:
1. Write to Firestore.
2. Wait for `waitForPendingWrites()` acknowledgment.
3. Show success toast.
4. Publish in-app/system notification.
5. Trigger immediate background sync (where applicable).

If cloud acknowledgment fails, success UI is not shown.

## Auth-Failure Behavior (Lag Protection)

When errors indicate securetoken/granttoken/unauthenticated/api-key/permission blocked:
- failure is marked in FirebaseAuthState,
- immediate sync requests are deferred for 2 minutes,
- worker returns success for non-recoverable auth errors (avoids retry storms),
- notifications UI falls back to local cache instead of constant failing stream churn.

This reduces background churn and perceived lag while auth/config issues are being fixed.

## Notification Read Path

NotificationsFragment:
- loads local notifications first,
- attaches Firestore snapshot listener when user/email is valid,
- merges cloud + local notifications,
- on auth-token listener failures, removes listener and keeps local view.

## Operational Notes
- All work requests require network connectivity.
- Immediate work uses `ExistingWorkPolicy.REPLACE` for latest-intent behavior.
- Periodic work uses `ExistingPeriodicWorkPolicy.KEEP` to avoid duplicate schedules.
- Legacy daily period reminder work is cancelled by scheduler to avoid duplicate old behavior.
