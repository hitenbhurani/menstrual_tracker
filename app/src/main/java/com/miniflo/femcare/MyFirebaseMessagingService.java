package com.miniflo.femcare;

import android.util.Log;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {    @Override
public void onMessageReceived(RemoteMessage remoteMessage) {
    // Check if message contains a notification payload.
    if (remoteMessage.getNotification() != null) {
        String title = remoteMessage.getNotification().getTitle();
        String body = remoteMessage.getNotification().getBody();

        // Use our existing NotificationHelper to show it
        NotificationHelper.showNotification(getApplicationContext(), title, body);
    }
}

    @Override
    public void onNewToken(String token) {
        Log.d("FCM", "Refreshed token: " + token);
        // In a real app, you would send this token to your server
    }
}