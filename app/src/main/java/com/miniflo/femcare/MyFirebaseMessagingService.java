package com.miniflo.femcare;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

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
        // In a real app, you would send this token to your server
    }
}