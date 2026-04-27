package com.miniflo.femcare;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;

public final class FirebaseAuthState {

    private static final String PREFS_NAME = "FemCarePrefs";
    private static final String KEY_LAST_AUTH_ERROR_MS = "last_firebase_auth_error_ms";
    private static final long AUTH_ERROR_BACKOFF_MS = 120_000L;

    private FirebaseAuthState() {
    }

    public static boolean isAuthTokenError(@Nullable Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String lowered = message.toLowerCase(Locale.US);
                if (lowered.contains("securetoken")
                        || lowered.contains("granttoken")
                        || lowered.contains("unauthenticated")
                        || lowered.contains("api key")
                        || lowered.contains("permission_denied")
                        || lowered.contains("blocked")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    public static void markAuthError(@NonNull Context context) {
        prefs(context).edit().putLong(KEY_LAST_AUTH_ERROR_MS, System.currentTimeMillis()).apply();
    }

    public static void clearAuthError(@NonNull Context context) {
        prefs(context).edit().remove(KEY_LAST_AUTH_ERROR_MS).apply();
    }

    public static boolean shouldDeferBackgroundWork(@NonNull Context context) {
        long lastAuthError = prefs(context).getLong(KEY_LAST_AUTH_ERROR_MS, 0L);
        if (lastAuthError <= 0L) {
            return false;
        }
        return System.currentTimeMillis() - lastAuthError < AUTH_ERROR_BACKOFF_MS;
    }

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
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // Store last auth error for debugging
        prefs.edit()
                .putString("last_auth_error_detail", message)
                .putLong("last_auth_error_time", System.currentTimeMillis())
                .apply();
        
        Log.e("FIREBASE_AUTH_ERROR", "Detailed error: " + message, error);
    }

    private static SharedPreferences prefs(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}