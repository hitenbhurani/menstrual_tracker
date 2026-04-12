package com.miniflo.femcare;

import android.app.Application;
import com.miniflo.femcare.data.AppDatabase;

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
    }
}