package com.miniflo.femcare;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public final class BackgroundTaskScheduler {

    private static final int DAILY_LOG_REMINDER_HOUR = 20;
    private static final int DAILY_LOG_REMINDER_MINUTE = 0;
    private static final int WEEKLY_WELLNESS_DAY = Calendar.MONDAY;
    private static final int WEEKLY_WELLNESS_HOUR = 9;
    private static final int WEEKLY_WELLNESS_MINUTE = 0;

    private static final String UNIQUE_DAILY_LOG_REMINDER = "work_daily_log_reminder";
    private static final String UNIQUE_CYCLE_ALERTS = "work_cycle_alerts";
    private static final String UNIQUE_WEEKLY_WELLNESS = "work_weekly_wellness";
    private static final String UNIQUE_IMMEDIATE_SYNC = "work_immediate_notification_sync";
    private static final String LEGACY_WORK_DAILY_PERIOD = "daily_period_reminder";

    private static final String PREFS_NAME = "FemCarePrefs";
    private static final String KEY_LAST_IMMEDIATE_SYNC_MS = "last_immediate_sync_ms";
    private static final long MIN_IMMEDIATE_SYNC_INTERVAL_MS = 20_000L;

    private BackgroundTaskScheduler() {
    }

    public static void scheduleAll(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        WorkManager workManager = WorkManager.getInstance(appContext);
        workManager.cancelUniqueWork(LEGACY_WORK_DAILY_PERIOD);

        PeriodicWorkRequest dailyLogReminder = new PeriodicWorkRequest.Builder(
                ScheduledNotificationWorker.class,
                24,
                TimeUnit.HOURS
        )
                .setInputData(buildInput(ScheduledNotificationWorker.MODE_DAILY, "periodic_daily_log"))
                .setConstraints(connectedConstraint())
                .setInitialDelay(delayToNextDailyLogReminder(), TimeUnit.MILLISECONDS)
                .build();

        PeriodicWorkRequest cycleAlerts = new PeriodicWorkRequest.Builder(
                ScheduledNotificationWorker.class,
                6,
                TimeUnit.HOURS
        )
                .setInputData(buildInput(ScheduledNotificationWorker.MODE_CYCLE, "periodic_cycle"))
                .setConstraints(connectedConstraint())
                .build();

        PeriodicWorkRequest weeklyWellness = new PeriodicWorkRequest.Builder(
                ScheduledNotificationWorker.class,
                7,
                TimeUnit.DAYS
        )
                .setInputData(buildInput(ScheduledNotificationWorker.MODE_WEEKLY, "periodic_weekly"))
                .setConstraints(connectedConstraint())
                .setInitialDelay(delayToNextWeeklyWellness(), TimeUnit.MILLISECONDS)
                .build();

        workManager.enqueueUniquePeriodicWork(
                UNIQUE_DAILY_LOG_REMINDER,
                ExistingPeriodicWorkPolicy.KEEP,
                dailyLogReminder
        );

        workManager.enqueueUniquePeriodicWork(
                UNIQUE_CYCLE_ALERTS,
                ExistingPeriodicWorkPolicy.KEEP,
                cycleAlerts
        );

        workManager.enqueueUniquePeriodicWork(
                UNIQUE_WEEKLY_WELLNESS,
                ExistingPeriodicWorkPolicy.KEEP,
                weeklyWellness
        );
    }

    public static void enqueueImmediateSync(@NonNull Context context, @Nullable String reason) {
        Context appContext = context.getApplicationContext();
        if (FirebaseAuthState.shouldDeferBackgroundWork(appContext)) {
            return;
        }

        SharedPreferences prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();
        long lastEnqueue = prefs.getLong(KEY_LAST_IMMEDIATE_SYNC_MS, 0L);

        if (now - lastEnqueue < MIN_IMMEDIATE_SYNC_INTERVAL_MS) {
            return;
        }

        prefs.edit().putLong(KEY_LAST_IMMEDIATE_SYNC_MS, now).apply();

        WorkManager workManager = WorkManager.getInstance(appContext);

        OneTimeWorkRequest immediateSync = new OneTimeWorkRequest.Builder(ScheduledNotificationWorker.class)
                .setInputData(buildInput(ScheduledNotificationWorker.MODE_IMMEDIATE, reason))
                .setConstraints(connectedConstraint())
                .build();

        workManager.enqueueUniqueWork(
                UNIQUE_IMMEDIATE_SYNC,
                ExistingWorkPolicy.REPLACE,
                immediateSync
        );
    }

    private static Constraints connectedConstraint() {
        return new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
    }

    private static Data buildInput(@NonNull String mode, @Nullable String reason) {
        Data.Builder builder = new Data.Builder().putString(ScheduledNotificationWorker.KEY_MODE, mode);

        if (reason != null && !reason.trim().isEmpty()) {
            builder.putString(ScheduledNotificationWorker.KEY_REASON, reason);
        }

        return builder.build();
    }

    private static long delayToNextDailyLogReminder() {
        Calendar now = Calendar.getInstance();
        Calendar next = (Calendar) now.clone();
        next.set(Calendar.HOUR_OF_DAY, DAILY_LOG_REMINDER_HOUR);
        next.set(Calendar.MINUTE, DAILY_LOG_REMINDER_MINUTE);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);

        if (!next.after(now)) {
            next.add(Calendar.DAY_OF_YEAR, 1);
        }

        return next.getTimeInMillis() - now.getTimeInMillis();
    }

    private static long delayToNextWeeklyWellness() {
        Calendar now = Calendar.getInstance();
        Calendar next = (Calendar) now.clone();
        next.set(Calendar.DAY_OF_WEEK, WEEKLY_WELLNESS_DAY);
        next.set(Calendar.HOUR_OF_DAY, WEEKLY_WELLNESS_HOUR);
        next.set(Calendar.MINUTE, WEEKLY_WELLNESS_MINUTE);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);

        if (!next.after(now)) {
            next.add(Calendar.WEEK_OF_YEAR, 1);
        }

        return next.getTimeInMillis() - now.getTimeInMillis();
    }
}
