package com.miniflo.femcare;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        String title = null;
        String body = null;

        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
        }

        if ((title == null || title.trim().isEmpty()) && remoteMessage.getData().containsKey("title")) {
            title = remoteMessage.getData().get("title");
        }
        if ((body == null || body.trim().isEmpty()) && remoteMessage.getData().containsKey("body")) {
            body = remoteMessage.getData().get("body");
        }

        if (title == null || title.trim().isEmpty()) {
            title = "FemCare Update";
        }
        if (body == null || body.trim().isEmpty()) {
            body = "You have a new notification.";
        }

        String messageId = remoteMessage.getMessageId();
        if (messageId == null || messageId.trim().isEmpty()) {
            messageId = String.valueOf(System.currentTimeMillis());
        }

        NotificationPublisher.publishForCurrentUser(
                getApplicationContext(),
                "fcm_" + messageId,
                title,
                body,
                "fcm",
                true
        );
    }

    @Override
    public void onNewToken(String token) {
        Log.d("FCM", "Refreshed token: " + token);
        
        // 1. Store token in SharedPreferences for local access
        SharedPreferences prefs = getSharedPreferences("FemCarePrefs", Context.MODE_PRIVATE);
        prefs.edit()
                .putString("fcm_token", token)
                .putLong("fcm_token_updated_at", System.currentTimeMillis())
                .apply();
        
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
}