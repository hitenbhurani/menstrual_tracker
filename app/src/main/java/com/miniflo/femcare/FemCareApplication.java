package com.miniflo.femcare;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.app.PendingIntent;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.miniflo.femcare.data.AppDatabase;

import java.util.HashMap;
import java.util.Map;

public class FemCareApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Background Tasks (FCM/Reminders)
        BackgroundTaskScheduler.scheduleAll(this);

        // Force database initialization so the Inspector sees it as active.
        // Use a synchronous query instead of LiveData to guarantee DB open.
        new Thread(() -> {
            AppDatabase.getInstance(getApplicationContext())
                    .userDao()
                    .getUserCountForWarmup();
        }, "room-warmup").start();
        
        // Retrieve and store FCM token on startup
        retrieveAndStoreFCMToken();
        
        // Realtime Database is not a core dependency for current app flows.
        // Skip the connection monitor here to avoid unnecessary noisy failures when
        // the project does not have RTDB configured or available on the current device.

        // Install a global uncaught exception handler to capture unexpected crashes,
        // persist a short crash summary for diagnostics, and schedule a graceful restart.
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                Log.e("AppCrash", "Uncaught exception", throwable);
                SharedPreferences prefs = getSharedPreferences("FemCarePrefs", Context.MODE_PRIVATE);
                prefs.edit().putString("last_crash", throwable.toString()).putLong("last_crash_ts", System.currentTimeMillis()).apply();

                // Schedule a restart via AlarmManager to avoid starting an Activity while the process is unstable.
                Intent restartIntent = new Intent(getApplicationContext(), MainActivity.class);
                restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 42, restartIntent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
                AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                if (am != null) {
                    am.set(AlarmManager.RTC, System.currentTimeMillis() + 1000, pendingIntent);
                }
            } catch (Exception e) {
                Log.e("AppCrash", "Failed while handling uncaught exception", e);
            } finally {
                // Terminate process to ensure a clean restart
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(2);
            }
        });
    }
    
    private void retrieveAndStoreFCMToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("FCM", "Fetching FCM token failed", task.getException());
                        return;
                    }
                    
                    String token = task.getResult();
                    Log.d("FCM", "Initial FCM token: " + token);
                    
                    // Store in SharedPreferences
                    SharedPreferences prefs = getSharedPreferences("FemCarePrefs", Context.MODE_PRIVATE);
                    prefs.edit()
                            .putString("fcm_token", token)
                            .putLong("fcm_token_updated_at", System.currentTimeMillis())
                            .apply();
                    
                    // If user is logged in, sync to Firestore
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null && user.getEmail() != null) {
                        Map<String, Object> tokenData = new HashMap<>();
                        tokenData.put("fcm_token", token);
                        tokenData.put("fcm_token_updated_at", System.currentTimeMillis());
                        
                        FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(user.getEmail().trim())
                                .set(tokenData, SetOptions.merge())
                                .addOnSuccessListener(aVoid -> Log.d("FCM", "Initial token synced to Firestore"))
                                .addOnFailureListener(e -> Log.e("FCM", "Failed to sync initial token to Firestore", e));
                    }
                });
    }
}