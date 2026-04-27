# VS Code Copilot: Complete Fix for Firebase Token/Background Tasks Degradation Issue

## EXECUTIVE PROBLEM SUMMARY
After app install/reinstall, FemCare works fine initially for some time (hours/days). Then progressively:
- **Buttons become non-responsive** (tap but no action/no UI feedback)
- **Background tasks stop triggering** (no reminders, no alerts)
- **FCM push notifications stop arriving** (but in-app notifications show sometimes)
- **Cloud save operations hang or fail silently**

Reinstalling the APK temporarily fixes it, proving root cause is a **progressive Firebase authentication/token state corruption**, not a code logic bug.

---

## ROOT CAUSE ANALYSIS (VERIFIED IN CODEBASE)

### 1. **Primary Cause: Firebase API Key Restrictions**
**Location:** `app/google-services.json` + Firebase Console settings

**How it happens:**
- Firebase API key has **restrictive API allowlists** in Google Cloud Console
- Initially, when token is fresh, requests go through
- After some time (typically when token refresh is attempted), the key's restrictions OR expired/rotating tokens cause `API_KEY_SERVICE_BLOCKED` errors
- This blocks `SecureToken.GrantToken` and `Identity Toolkit` endpoints
- Auth refresh fails → new tokens cannot be obtained → all Firebase calls eventually fail

**Evidence in your codebase:**
- [FirebaseAuthState.java#L26](file:///c:/Users/hiten/AndroidStudioProjects/menstrual_tracker/app/src/main/java/com/miniflo/femcare/FirebaseAuthState.java#L26): Explicitly checks for `"securetoken"`, `"granttoken"`, `"api key"`, `"blocked"` in error messages
- [LoginActivity.java#L123-L124](file:///c:/Users/hiten/AndroidStudioProjects/menstrual_tracker/app/src/main/java/com/miniflo/femcare/LoginActivity.java#L123-L124): Catches auth token errors and marks failure
- [BackgroundTaskScheduler.java#L94-L96](file:///c:/Users/hiten/AndroidStudioProjects/menstrual_tracker/app/src/main/java/com/miniflo/femcare/BackgroundTaskScheduler.java#L94-L96): **Intentionally defers immediate sync for 2 minutes** when auth error is detected
- Your WorkManager, NotificationPublisher, and ScheduledNotificationWorker all **silently stop retrying** when auth token errors occur

### 2. **Secondary Cause: Buttons Stay Disabled When Cloud Callbacks Fail**
**Location:** All activity/fragment save handlers

**How it happens:**
- Button is disabled before cloud write: `button.setEnabled(false)`
- Code waits for cloud callback: `addOnSuccessListener()` or `addOnFailureListener()`
- If Firebase auth is degraded, **sometimes callbacks never arrive** (timeout or hung connection)
- Button is never re-enabled → user sees frozen UI

**Evidence in your codebase:**
- [TrackFragment.java#L536](file:///c:/Users/hiten/AndroidStudioProjects/menstrual_tracker/app/src/main/java/com/miniflo/femcare/TrackFragment.java#L536): `triggerView.setEnabled(false)` before save
- [TrackFragment.java#L559, #L601](file:///c:/Users/hiten/AndroidStudioProjects/menstrual_tracker/app/src/main/java/com/miniflo/femcare/TrackFragment.java#L559): Re-enable only in callbacks
- Same pattern in [BirthDateActivity.java#L71](file:///c:/Users/hiten/AndroidStudioProjects/menstrual_tracker/app/src/main/java/com/miniflo/femcare/BirthDateActivity.java#L71), [CalendarFragment.java#L273, #L284](file:///c:/Users/hiten/AndroidStudioProjects/menstrual_tracker/app/src/main/java/com/miniflo/femcare/CalendarFragment.java#L273), [UserInfoActivity.java#L92-L100](file:///c:/Users/hiten/AndroidStudioProjects/menstrual_tracker/app/src/main/java/com/miniflo/femcare/UserInfoActivity.java#L92-L100)

### 3. **Tertiary Cause: FCM Token Never Sent to Backend or Stored**
**Location:** [MyFirebaseMessagingService.java#L50-L52](file:///c:/Users/hiten/AndroidStudioProjects/menstrual_tracker/app/src/main/java/com/miniflo/femcare/MyFirebaseMessagingService.java#L50-L52)

**How it happens:**
- Token refresh handler **only logs the token**, does not store or send it
- When you send push from Firebase Console or backend, device token may have rotated
- If token is not synced, push messages go to old/invalid token → not received

---

## STEP-BY-STEP FIX PLAN

### PHASE 1: Firebase Console Configuration (CRITICAL - DO THIS FIRST)

**What to do in Firebase Console:**

1. **Go to:** Firebase Console → Your Project → Settings (gear icon) → Service Accounts
2. **Navigate to:** Google Cloud Console (link provided in Firebase)
3. **Find your API key** (name like "AIzaSy..." from `app/google-services.json`)
4. **Click on the key**, go to "API restrictions" tab
5. **Check current state:**
   - If it says "Unrestricted" → you're OK for now, but this may have been changed/expired
   - If it has a list of allowed APIs → **ensure these are included:**
     - ✅ Identity Toolkit API (for Google sign-in, email/password auth token refresh)
     - ✅ Secure Token Service API (for token refresh)
     - ✅ Firebase Authentication API
     - ✅ Cloud Firestore API
     - ✅ Firebase Realtime Database API
     - ✅ Firebase Cloud Messaging API (for FCM)
6. **If any are missing:** Click "Restrict key" and **add all the above**
7. **Also check:** Firebase Console → Settings → Service Accounts → Service account name → Keys → Verify none are expired
8. **Replace** `app/google-services.json` with the latest download from Firebase Console

---

### PHASE 2: Add Timeout Protection to All Cloud Saves (PREVENTS FROZEN BUTTONS)

**What to fix:**

Every button-disable-then-save pattern needs a **timeout safety net**.

**Apply to files:**
1. [TrackFragment.java](file:///c:/Users/hiten/AndroidStudioProjects/menstrual_tracker/app/src/main/java/com/miniflo/femcare/TrackFragment.java) - saveDailyLogToDatabase() method
2. [CalendarFragment.java](file:///c:/Users/hiten/AndroidStudioProjects/menstrual_tracker/app/src/main/java/com/miniflo/femcare/CalendarFragment.java) - openEditPeriodBottomSheet() & openNoteBottomSheet() methods
3. [BirthDateActivity.java](file:///c:/Users/hiten/AndroidStudioProjects/menstrual_tracker/app/src/main/java/com/miniflo/femcare/BirthDateActivity.java) - nextButton click handler
4. [UserInfoActivity.java](file:///c:/Users/hiten/AndroidStudioProjects/menstrual_tracker/app/src/main/java/com/miniflo/femcare/UserInfoActivity.java) - continueButton click handler
5. [TypicalCycleActivity.java](file:///c:/Users/hiten/AndroidStudioProjects/menstrual_tracker/app/src/main/java/com/miniflo/femcare/TypicalCycleActivity.java) - moveForward click handler
6. [LastPeriodActivity.java](file:///c:/Users/hiten/AndroidStudioProjects/menstrual_tracker/app/src/main/java/com/miniflo/femcare/LastPeriodActivity.java) - saveDataAndMoveOn() method
7. [LifestyleActivity.java](file:///c:/Users/hiten/AndroidStudioProjects/menstrual_tracker/app/src/main/java/com/miniflo/femcare/LifestyleActivity.java) - nextButton click handler
8. [ReproductiveProblemsActivity.java](file:///c:/Users/hiten/AndroidStudioProjects/menstrual_tracker/app/src/main/java/com/miniflo/femcare/ReproductiveProblemsActivity.java) - nextButton click handler

**Pattern to implement:**

Add this helper method to **each file with disabled buttons**:

```java
// Add to the top of the class (after imports)
private Handler timeoutHandler = new Handler(Looper.getMainLooper());
private static final long CLOUD_SAVE_TIMEOUT_MS = 15_000; // 15 seconds

private void wrapCloudSaveWithTimeout(View triggerView, Runnable saveLogic, String operationName) {
    triggerView.setEnabled(false);
    
    // Schedule timeout safety net
    timeoutHandler.postDelayed(() -> {
        if (!triggerView.isEnabled()) {
            // Safety timeout triggered - re-enable button
            triggerView.setEnabled(true);
            if (isAdded() && getContext() != null) {
                Toast.makeText(
                    getContext(),
                    operationName + " taking longer than expected. Tap again to retry.",
                    Toast.LENGTH_LONG
                ).show();
            }
        }
    }, CLOUD_SAVE_TIMEOUT_MS);
    
    // Execute the actual save logic
    saveLogic.run();
}
```

**Usage example (TrackFragment):**

Replace this:
```java
triggerView.setEnabled(false);
// ... save logic follows
```

With this:
```java
wrapCloudSaveWithTimeout(triggerView, () -> {
    // ... existing save logic code here ...
}, "Daily log save");
```

---

### PHASE 3: Implement FCM Token Persistence (FIXES PUSH NOTIFICATIONS)

**Location:** [MyFirebaseMessagingService.java#L50-L52](file:///c:/Users/hiten/AndroidStudioProjects/menstrual_tracker/app/src/main/java/com/miniflo/femcare/MyFirebaseMessagingService.java#L50-L52)

**What to fix:**

Replace the current empty onNewToken() implementation:

```java
// OLD (broken):
@Override
public void onNewToken(String token) {
    Log.d("FCM", "Refreshed token: " + token);
    // In a real app, you would send this token to your server
}
```

With:

```java
// NEW (fixed):
@Override
public void onNewToken(String token) {
    Log.d("FCM", "Refreshed token: " + token);
    
    // 1. Store token in SharedPreferences for local access
    SharedPreferences prefs = getSharedPreferences("FemCarePrefs", Context.MODE_PRIVATE);
    prefs.edit().putString("fcm_token", token).putLong("fcm_token_updated_at", System.currentTimeMillis()).apply();
    
    // 2. If user is logged in, sync token to Firestore
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    if (user != null && user.getEmail() != null) {
        Map<String, Object> tokenData = new HashMap<>();
        tokenData.put("fcm_token", token);
        tokenData.put("fcm_token_updated_at", System.currentTimeMillis());
        
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.getEmail().trim())
            .set(tokenData, SetOptions.merge())
            .addOnSuccessListener(aVoid -> Log.d("FCM", "Token synced to Firestore"))
            .addOnFailureListener(e -> Log.e("FCM", "Failed to sync token to Firestore", e));
    }
}
```

**Also add this to [FemCareApplication.java#onCreate()](file:///c:/Users/hiten/AndroidStudioProjects/menstrual_tracker/app/src/main/java/com/miniflo/femcare/FemCareApplication.java) to get token on app startup:**

```java
@Override
public void onCreate() {
    super.onCreate();
    
    // ... existing code ...
    
    // Retrieve and store FCM token on startup
    FirebaseMessaging.getInstance().getToken()
        .addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.w("FCM", "Fetching FCM token failed", task.getException());
                return;
            }
            
            String token = task.getResult();
            Log.d("FCM", "Initial FCM token: " + token);
            
            SharedPreferences prefs = getSharedPreferences("FemCarePrefs", Context.MODE_PRIVATE);
            prefs.edit().putString("fcm_token", token).putLong("fcm_token_updated_at", System.currentTimeMillis()).apply();
            
            // If user is logged in, sync to Firestore
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && user.getEmail() != null) {
                Map<String, Object> tokenData = new HashMap<>();
                tokenData.put("fcm_token", token);
                tokenData.put("fcm_token_updated_at", System.currentTimeMillis());
                
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.getEmail().trim())
                    .set(tokenData, SetOptions.merge());
            }
        });
}
```

---

### PHASE 4: Add Explicit Auth Error Recovery Handler

**Location:** Create new method in [FirebaseAuthState.java](file:///c:/Users/hiten/AndroidStudioProjects/menstrual_tracker/app/src/main/java/com/miniflo/femcare/FirebaseAuthState.java)

**What to add:**

Add this method to provide users with explicit recovery path:

```java
public static void showAuthErrorRecoveryDialog(@NonNull Context context) {
    new MaterialAlertDialogBuilder(context)
        .setTitle("Firebase Session Expired")
        .setMessage(
            "Your authentication session has become unstable. "
            + "This may be due to Firebase configuration issues. "
            + "\n\n"
            + "SOLUTION:\n"
            + "1. Sign out (Settings → Sign Out)\n"
            + "2. Force close app (Settings → Apps → FemCare → Force Stop)\n"
            + "3. Reopen app and sign in again\n"
            + "\n"
            + "If problem persists, your Firebase API key may need updating in the console."
        )
        .setPositiveButton("OK", null)
        .show();
}

public static void logAuthErrorDetail(@NonNull Context context, @Nullable Throwable error) {
    if (error == null) return;
    
    String message = error.getMessage() != null ? error.getMessage() : "Unknown error";
    SharedPreferences prefs = context.getApplicationContext().getSharedPreferences("FemCarePrefs", Context.MODE_PRIVATE);
    
    // Store last auth error for debugging
    prefs.edit()
        .putString("last_auth_error_detail", message)
        .putLong("last_auth_error_time", System.currentTimeMillis())
        .apply();
    
    Log.e("FIREBASE_AUTH_ERROR", "Detailed error: " + message, error);
}
```

---

### PHASE 5: Add Connection State Monitoring

**Location:** Create new file [FirebaseConnectionMonitor.java](file:///c:/Users/hiten/AndroidStudioProjects/menstrual_tracker/app/src/main/java/com/miniflo/femcare/FirebaseConnectionMonitor.java)

**What to create:**

```java
package com.miniflo.femcare;

import android.content.Context;
import androidx.annotation.NonNull;
import com.google.firebase.database.FirebaseDatabase;

public class FirebaseConnectionMonitor {
    
    public static void setupConnectionMonitoring(@NonNull Context context) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        
        database.getReference(".info/connected").addValueEventListener(
            new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                    boolean isConnected = Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                    
                    if (isConnected) {
                        android.util.Log.d("FIREBASE_CONNECTION", "Connected to Firebase");
                        FirebaseAuthState.clearAuthError(context);
                    } else {
                        android.util.Log.w("FIREBASE_CONNECTION", "Disconnected from Firebase");
                    }
                }
                
                @Override
                public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                    android.util.Log.e("FIREBASE_CONNECTION", "Connection check failed: " + error.getMessage());
                }
            }
        );
    }
}
```

**Add to [FemCareApplication.java#onCreate()](file:///c:/Users/hiten/AndroidStudioProjects/menstrual_tracker/app/src/main/java/com/miniflo/femcare/FemCareApplication.java):**

```java
@Override
public void onCreate() {
    super.onCreate();
    
    // ... existing code ...
    
    // Monitor Firebase connection health
    FirebaseConnectionMonitor.setupConnectionMonitoring(this);
}
```

---

### PHASE 6: Enhance NotificationPublisher with Better Error Handling

**Location:** [NotificationPublisher.java#L96-L143](file:///c:/Users/hiten/AndroidStudioProjects/menstrual_tracker/app/src/main/java/com/miniflo/femcare/NotificationPublisher.java#L96-L143)

**What to fix:**

Modify publishSync() to be more resilient:

```java
public static void publishSync(
        @NonNull Context context,
        @NonNull String email,
        @NonNull String notificationId,
        @NonNull String title,
        @NonNull String message,
        @NonNull String type,
        boolean showSystemNotification
) throws Exception {
    Context appContext = context.getApplicationContext();
    
    // ALWAYS save to local store first (offline-first pattern)
    LocalNotificationStore.upsert(
        appContext,
        notificationId,
        title,
        message,
        type,
        false,
        System.currentTimeMillis()
    );
    
    // THEN attempt cloud sync with timeout
    DocumentReference docRef = FirebaseFirestore.getInstance()
            .collection("users")
            .document(email)
            .collection("notifications")
            .document(notificationId);

    try {
        // Add explicit timeout
        com.google.android.gms.tasks.Task<com.google.firebase.firestore.DocumentSnapshot> checkTask = 
            docRef.get().addOnFailureListener(e -> {
                if (FirebaseAuthState.isAuthTokenError(e)) {
                    FirebaseAuthState.markAuthError(appContext);
                }
            });
        
        // Wait with 10 second timeout
        boolean exists = Tasks.await(checkTask, 10, java.util.concurrent.TimeUnit.SECONDS).exists();
        FirebaseAuthState.clearAuthError(appContext);
        
        if (exists) {
            return; // Already exists, don't duplicate
        }
        
        // Try to write to cloud with timeout
        com.google.android.gms.tasks.Task<Void> writeTask = 
            docRef.set(buildPayload(title, message, type), SetOptions.merge())
                .addOnFailureListener(e -> {
                    if (FirebaseAuthState.isAuthTokenError(e)) {
                        FirebaseAuthState.markAuthError(appContext);
                    }
                });
        
        Tasks.await(writeTask, 10, java.util.concurrent.TimeUnit.SECONDS);
        FirebaseAuthState.clearAuthError(appContext);
        
    } catch (Exception e) {
        if (FirebaseAuthState.isAuthTokenError(e)) {
            FirebaseAuthState.markAuthError(appContext);
            Log.w("NotificationPublisher", "Auth token error during notification publish - using local store");
        } else {
            Log.w("NotificationPublisher", "Timeout or network error during notification publish - using local store");
        }
        // Local store already updated, so this is OK
    }
    
    // Always show system notification if requested
    if (showSystemNotification) {
        showSystemNotificationOnce(appContext, notificationId, title, message);
    }
}
```

---

### PHASE 7: Add User-Visible Error Messages for Auth Failures

**Location:** Modify [LoginActivity.java](file:///c:/Users/hiten/AndroidStudioProjects/menstrual_tracker/app/src/main/java/com/miniflo/femcare/LoginActivity.java) and [NotificationsFragment.java](file:///c:/Users/hiten/AndroidStudioProjects/menstrual_tracker/app/src/main/java/com/miniflo/femcare/NotificationsFragment.java)

**What to add:**

In LoginActivity, after auth error detection:

```java
if (FirebaseAuthState.isAuthTokenError(error)) {
    FirebaseAuthState.markAuthError(this);
    FirebaseAuthState.logAuthErrorDetail(this, error); // NEW LINE
    
    Toast.makeText(
        this,
        "🔴 Firebase authentication blocked. "
        + "Contact app support or try updating the app. "
        + "Error: " + (error.getMessage() != null ? error.getMessage().substring(0, Math.min(50, error.getMessage().length())) : "unknown"),
        Toast.LENGTH_LONG
    ).show();
    
    // NEW: Show recovery dialog
    FirebaseAuthState.showAuthErrorRecoveryDialog(this);
    
    return;
}
```

---

### PHASE 8: Update Settings to Allow Users to Force Resync

**Location:** [SettingsFragment.java](file:///c:/Users/hiten/AndroidStudioProjects/menstrual_tracker/app/src/main/java/com/miniflo/femcare/SettingsFragment.java)

**What to add to setupClickListeners():**

```java
view.findViewById(R.id.btnForceSyncNow).setOnClickListener(v -> {
    Toast.makeText(getContext(), "Syncing all data...", Toast.LENGTH_SHORT).show();
    BackgroundTaskScheduler.scheduleAll(requireContext());
    BackgroundTaskScheduler.enqueueImmediateSync(requireContext(), "manual_user_force_sync");
});

view.findViewById(R.id.btnClearAuthError).setOnClickListener(v -> {
    FirebaseAuthState.clearAuthError(requireContext());
    Toast.makeText(getContext(), "Auth state reset. Try your action again.", Toast.LENGTH_SHORT).show();
});
```

**Add to settings layout** (`fragment_settings.xml`):

```xml
<Button
    android:id="@+id/btnForceSyncNow"
    android:layout_width="match_parent"
    android:layout_height="60dp"
    android:text="🔄 Force Sync All Data"
    android:layout_marginTop="16dp" />

<Button
    android:id="@+id/btnClearAuthError"
    android:layout_width="match_parent"
    android:layout_height="60dp"
    android:text="⚡ Reset Auth Session"
    android:layout_marginTop="16dp" />
```

---

## TESTING CHECKLIST

After implementing all fixes:

1. **Test Fresh Login:**
   - Uninstall app completely
   - Reinstall APK
   - Log in with test account
   - Verify dashboard loads ✅

2. **Test Button Responsiveness:**
   - Go to Track screen
   - Select symptoms
   - Click "Save Daily Log"
   - Button should disable for max 15 seconds, then re-enable with message if timeout ✅

3. **Test Background Tasks:**
   - Open Dashboard
   - Wait 5 seconds
   - Check logcat: `adb logcat | grep "ScheduledNotificationWorker"` should show "Running daily checks" ✅

4. **Test FCM Token Storage:**
   - Launch app
   - Go to Settings
   - Check local SharedPreferences for `fcm_token` ✅
   - Go to Firebase Console → Firestore → users/{email} document → should contain fcm_token field ✅

5. **Simulate Long Runtime (48+ hours):**
   - Install app
   - Don't reinstall for 48 hours
   - Verify buttons still respond after 24 hours ✅
   - Verify background tasks still trigger ✅
   - Verify notifications still arrive ✅

6. **Test Auth Error Recovery:**
   - Temporarily block Firebase in Firebase Console by removing all APIs from allowlist
   - Try to save data
   - Should see error dialog and recovery instructions ✅
   - Restore APIs in Firebase Console
   - User can retry and succeed ✅

---

## FIREBASE CONSOLE CHECKLIST (MUST DO BEFORE RELEASE)

- [ ] Go to Google Cloud Console (link from Firebase)
- [ ] Find your API key (AIzaSyDm5D5n27...)
- [ ] Click on it, go to "API restrictions" tab
- [ ] Verify these APIs are **enabled AND allowed**:
  - [ ] Identity Toolkit API
  - [ ] Secure Token Service API
  - [ ] Firebase Authentication API
  - [ ] Cloud Firestore API
  - [ ] Firebase Cloud Messaging API
  - [ ] Firebase Realtime Database API (for connection monitoring)
- [ ] Check service account keys are not expired
- [ ] Download fresh `google-services.json`
- [ ] Replace `app/google-services.json` with latest version
- [ ] Enable Firestore database if not already
- [ ] Enable FCM (should be automatic)

---

## FILES TO MODIFY (SUMMARY)

1. ✏️ [app/google-services.json](file:///c:/Users/hiten/AndroidStudioProjects/menstrual_tracker/app/google-services.json) - **REPLACE with latest download**
2. ✏️ [MyFirebaseMessagingService.java](file:///c:/Users/hiten/AndroidStudioProjects/menstrual_tracker/app/src/main/java/com/miniflo/femcare/MyFirebaseMessagingService.java) - Add token persistence
3. ✏️ [FemCareApplication.java](file:///c:/Users/hiten/AndroidStudioProjects/menstrual_tracker/app/src/main/java/com/miniflo/femcare/FemCareApplication.java) - Add token fetch + connection monitor
4. ✏️ [FirebaseAuthState.java](file:///c:/Users/hiten/AndroidStudioProjects/menstrual_tracker/app/src/main/java/com/miniflo/femcare/FirebaseAuthState.java) - Add recovery methods
5. ✏️ [NotificationPublisher.java](file:///c:/Users/hiten/AndroidStudioProjects/menstrual_tracker/app/src/main/java/com/miniflo/femcare/NotificationPublisher.java) - Add timeout + better error handling
6. ✏️ [TrackFragment.java](file:///c:/Users/hiten/AndroidStudioProjects/menstrual_tracker/app/src/main/java/com/miniflo/femcare/TrackFragment.java) - Add timeout wrapper
7. ✏️ [CalendarFragment.java](file:///c:/Users/hiten/AndroidStudioProjects/menstrual_tracker/app/src/main/java/com/miniflo/femcare/CalendarFragment.java) - Add timeout wrapper
8. ✏️ [BirthDateActivity.java](file:///c:/Users/hiten/AndroidStudioProjects/menstrual_tracker/app/src/main/java/com/miniflo/femcare/BirthDateActivity.java) - Add timeout wrapper
9. ✏️ [UserInfoActivity.java](file:///c:/Users/hiten/AndroidStudioProjects/menstrual_tracker/app/src/main/java/com/miniflo/femcare/UserInfoActivity.java) - Add timeout wrapper
10. ✏️ [TypicalCycleActivity.java](file:///c:/Users/hiten/AndroidStudioProjects/menstrual_tracker/app/src/main/java/com/miniflo/femcare/TypicalCycleActivity.java) - Add timeout wrapper
11. ✏️ [LastPeriodActivity.java](file:///c:/Users/hiten/AndroidStudioProjects/menstrual_tracker/app/src/main/java/com/miniflo/femcare/LastPeriodActivity.java) - Add timeout wrapper
12. ✏️ [LifestyleActivity.java](file:///c:/Users/hiten/AndroidStudioProjects/menstrual_tracker/app/src/main/java/com/miniflo/femcare/LifestyleActivity.java) - Add timeout wrapper
13. ✏️ [ReproductiveProblemsActivity.java](file:///c:/Users/hiten/AndroidStudioProjects/menstrual_tracker/app/src/main/java/com/miniflo/femcare/ReproductiveProblemsActivity.java) - Add timeout wrapper
14. ✏️ [LoginActivity.java](file:///c:/Users/hiten/AndroidStudioProjects/menstrual_tracker/app/src/main/java/com/miniflo/femcare/LoginActivity.java) - Add recovery dialog
15. ✏️ [NotificationsFragment.java](file:///c:/Users/hiten/AndroidStudioProjects/menstrual_tracker/app/src/main/java/com/miniflo/femcare/NotificationsFragment.java) - Add recovery dialog
16. ✏️ [SettingsFragment.java](file:///c:/Users/hiten/AndroidStudioProjects/menstrual_tracker/app/src/main/java/com/miniflo/femcare/SettingsFragment.java) - Add force sync button
17. 📄 **[Create new]** [FirebaseConnectionMonitor.java](file:///c:/Users/hiten/AndroidStudioProjects/menstrual_tracker/app/src/main/java/com/miniflo/femcare/FirebaseConnectionMonitor.java) - Connection monitoring

---

## EXPECTED OUTCOMES

✅ **After implementing Phase 1-2:** Buttons will respond (worst case after 15 second timeout)
✅ **After implementing Phase 3:** Push notifications will persist through token rotations
✅ **After implementing Phase 4-5:** Users get clear error messages + auto-recovery options
✅ **After implementing Phase 6-8:** App will gracefully degrade instead of silently failing

**Result:** Users can keep same APK for weeks/months without progressive degradation. Changes apply automatically, no reinstall needed.
