# Copilot Fix Summary — Firebase token/background tasks fixes

Date: 2026-04-28

This file summarizes code edits I applied to mitigate progressive Firebase auth/token degradation and to add resilience and observability.

## Files added
- `app/src/main/java/com/miniflo/femcare/FirebaseConnectionMonitor.java`
  - New class monitoring Firebase Realtime Database `.info/connected` and clearing auth error state when connected.

## Files modified (summary)
- `app/src/main/java/com/miniflo/femcare/MyFirebaseMessagingService.java`
  - Persist FCM tokens to `SharedPreferences` and sync to Firestore in `onNewToken()`.

- `app/src/main/java/com/miniflo/femcare/FemCareApplication.java`
  - Retrieve and persist initial FCM token at startup and call `FirebaseConnectionMonitor.setupConnectionMonitoring()`.

- `app/src/main/java/com/miniflo/femcare/FirebaseAuthState.java`
  - Added `showAuthErrorRecoveryDialog()` and `logAuthErrorDetail()` to surface and record auth failures.

- `app/src/main/java/com/miniflo/femcare/NotificationPublisher.java`
  - Added explicit timeouts (10s) to Firestore checks/writes in `publishSync()` and moved to an offline-first pattern.

- `app/src/main/java/com/miniflo/femcare/TrackFragment.java`
  - Added a `wrapCloudSaveWithTimeout(...)` helper and applied it to daily log saves (15s safety timeout).

- `app/src/main/java/com/miniflo/femcare/CalendarFragment.java`
  - Added a `wrapCloudSaveWithTimeout(...)` helper and applied it to cycle edits and note saves (15s safety timeout).

- `app/src/main/java/com/miniflo/femcare/BirthDateActivity.java`
  - Added a timeout wrapper for the age save flow.

- `app/src/main/java/com/miniflo/femcare/UserInfoActivity.java`
  - Added a timeout wrapper for user info saves.

- `app/src/main/java/com/miniflo/femcare/LifestyleActivity.java`
  - Added a timeout wrapper for lifestyle saves and fixed a Java lambda capture compile error by making local variables effectively final.

- `app/src/main/java/com/miniflo/femcare/ReproductiveProblemsActivity.java`
  - Added a timeout wrapper for reproductive health saves.

- `app/src/main/java/com/miniflo/femcare/LoginActivity.java`
  - Surface auth errors to users with a recovery dialog and log detailed error info.

- `app/src/main/java/com/miniflo/femcare/NotificationsFragment.java`
  - Log and persist auth error details when listener failure appears; show cached notifications and notify the user.

- `app/src/main/java/com/miniflo/femcare/SettingsFragment.java`
  - Added two optional developer controls (Force Sync All Data, Reset Auth Session) and click handlers; later changed visibility behavior (see below).

- `app/src/main/res/layout/fragment_settings.xml`
  - Added two new buttons for force sync and auth reset (layout only).

## Rationale
- Progressive degradation was caused by Firebase auth/token refresh failures (API key or Secure Token errors). The changes address:
  - Storing/syncing FCM tokens so notifications survive rotation
  - Detecting and marking auth token errors and deferring/preserving local actions when cloud operations fail
  - Avoiding UI freeze when cloud callbacks never return by adding timeouts and re-enabling controls
  - Exposing recovery and diagnostic info for users and maintainers

## Next recommended steps (manual)
1. In Firebase / Google Cloud Console: verify API key restrictions include Identity Toolkit / Secure Token / Firestore / FCM / Realtime Database.
2. Replace `app/google-services.json` with the freshly downloaded one from Firebase Console if needed.
3. Test the flows listed in the original fix plan (token persistence, background tasks, timeout behaviour, error dialogs).


---

If you want, I can:
- Remove the developer controls from the UI permanently, or
- Make them visible only to admins (based on a whitelist), or
- Keep them visible only in debug builds (current change I applied next).

Tell me which policy you prefer and I'll apply it.
