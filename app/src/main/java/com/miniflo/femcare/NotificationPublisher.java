package com.miniflo.femcare;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public final class NotificationPublisher {

    private static final String PREFS_NAME = "FemCarePrefs";
    private static final String PREF_SYSTEM_PREFIX = "notif_sys_shown_";

    private NotificationPublisher() {
    }

    public static void publishForCurrentUser(
            @NonNull Context context,
            @NonNull String notificationId,
            @NonNull String title,
            @NonNull String message,
            @NonNull String type,
            boolean showSystemNotification
    ) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getEmail() == null || currentUser.getEmail().trim().isEmpty()) {
            Context appContext = context.getApplicationContext();
            LocalNotificationStore.upsert(appContext, notificationId, title, message, type, false, System.currentTimeMillis());
            if (showSystemNotification) {
                showSystemNotificationOnce(appContext, notificationId, title, message);
            }
            return;
        }

        publishAsync(
                context,
                currentUser.getEmail().trim(),
                notificationId,
                title,
                message,
                type,
                showSystemNotification
        );
    }

    public static void publishAsync(
            @NonNull Context context,
            @NonNull String email,
            @NonNull String notificationId,
            @NonNull String title,
            @NonNull String message,
            @NonNull String type,
            boolean showSystemNotification
    ) {
        Context appContext = context.getApplicationContext();
        DocumentReference docRef = FirebaseFirestore.getInstance()
                .collection("users")
                .document(email)
                .collection("notifications")
                .document(notificationId);

        docRef.get()
                .addOnSuccessListener(existingDoc -> {
                    FirebaseAuthState.clearAuthError(appContext);

                    if (existingDoc.exists()) {
                        return;
                    }

                    LocalNotificationStore.upsert(appContext, notificationId, title, message, type, false, System.currentTimeMillis());

                    docRef.set(buildPayload(title, message, type), SetOptions.merge())
                            .addOnSuccessListener(unused -> {
                                FirebaseAuthState.clearAuthError(appContext);
                                if (showSystemNotification) {
                                    showSystemNotificationOnce(appContext, notificationId, title, message);
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (FirebaseAuthState.isAuthTokenError(e)) {
                                    FirebaseAuthState.markAuthError(appContext);
                                }
                                if (showSystemNotification) {
                                    showSystemNotificationOnce(appContext, notificationId, title, message);
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    if (FirebaseAuthState.isAuthTokenError(e)) {
                        FirebaseAuthState.markAuthError(appContext);
                    }
                    LocalNotificationStore.upsert(appContext, notificationId, title, message, type, false, System.currentTimeMillis());
                    if (showSystemNotification) {
                        showSystemNotificationOnce(appContext, notificationId, title, message);
                    }
                });
    }

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
        DocumentReference docRef = FirebaseFirestore.getInstance()
                .collection("users")
                .document(email)
                .collection("notifications")
                .document(notificationId);

        boolean exists;
        try {
            exists = Tasks.await(docRef.get()).exists();
        } catch (Exception e) {
            if (FirebaseAuthState.isAuthTokenError(e)) {
                FirebaseAuthState.markAuthError(appContext);
            }
            LocalNotificationStore.upsert(appContext, notificationId, title, message, type, false, System.currentTimeMillis());
            if (showSystemNotification) {
                showSystemNotificationOnce(appContext, notificationId, title, message);
            }

            if (FirebaseAuthState.isAuthTokenError(e)) {
                return;
            }
            throw e;
        }

        FirebaseAuthState.clearAuthError(appContext);

        if (exists) {
            return;
        }

        LocalNotificationStore.upsert(appContext, notificationId, title, message, type, false, System.currentTimeMillis());
        if (showSystemNotification) {
            showSystemNotificationOnce(appContext, notificationId, title, message);
        }

        try {
            Tasks.await(docRef.set(buildPayload(title, message, type), SetOptions.merge()));
            FirebaseAuthState.clearAuthError(appContext);
        } catch (Exception e) {
            if (FirebaseAuthState.isAuthTokenError(e)) {
                FirebaseAuthState.markAuthError(appContext);
                return;
            }
            throw e;
        }
    }

    private static Map<String, Object> buildPayload(String title, String message, String type) {
        Map<String, Object> notif = new HashMap<>();
        notif.put("title", title);
        notif.put("message", message);
        notif.put("type", type);
        notif.put("isRead", false);
        notif.put("timestamp", System.currentTimeMillis());
        return notif;
    }

    private static void showSystemNotificationOnce(
            @NonNull Context context,
            @NonNull String notificationId,
            @NonNull String title,
            @NonNull String message
    ) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String key = PREF_SYSTEM_PREFIX + notificationId;

        if (prefs.getBoolean(key, false)) {
            return;
        }

        NotificationHelper.showNotification(context, notificationId, title, message);
        prefs.edit().putBoolean(key, true).apply();
    }

}
