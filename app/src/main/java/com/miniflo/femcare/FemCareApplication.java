package com.miniflo.femcare;

import android.app.Application;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;

public class FemCareApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        setupDailyPeriodReminder();
    }

    private void setupDailyPeriodReminder() {
        // Create a repeating task that runs every 24 hours
        PeriodicWorkRequest reminderRequest = new PeriodicWorkRequest.Builder(
                PeriodReminderWorker.class, 
                24, 
                TimeUnit.HOURS
        ).build();

        // Schedule it
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "daily_period_reminder",
                ExistingPeriodicWorkPolicy.KEEP, // Keep the existing schedule if it's already running
                reminderRequest
        );
    }
}
