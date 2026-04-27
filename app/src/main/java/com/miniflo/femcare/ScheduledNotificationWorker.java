package com.miniflo.femcare;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Tasks;
import java.util.concurrent.TimeUnit;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ScheduledNotificationWorker extends Worker {

    public static final String KEY_MODE = "mode";
    public static final String KEY_REASON = "reason";

    public static final String MODE_DAILY = "daily";
    public static final String MODE_CYCLE = "cycle";
    public static final String MODE_WEEKLY = "weekly";
    public static final String MODE_IMMEDIATE = "immediate";

    private static final long DAY_MILLIS = 24L * 60L * 60L * 1000L;

    public ScheduledNotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        try {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null || user.getEmail() == null || user.getEmail().trim().isEmpty()) {
                return Result.success();
            }

            String email = user.getEmail().trim();
            String mode = getInputData().getString(KEY_MODE);
            if (mode == null || mode.trim().isEmpty()) {
                mode = MODE_IMMEDIATE;
            }

            if (MODE_DAILY.equals(mode) || MODE_IMMEDIATE.equals(mode)) {
                runDailyLogCheck(context, email);
            }

            if (MODE_CYCLE.equals(mode) || MODE_IMMEDIATE.equals(mode)) {
                runCycleChecks(context, email);
            }

            if (MODE_WEEKLY.equals(mode) || MODE_IMMEDIATE.equals(mode)) {
                runWeeklyWellnessChecks(context, email);
            }

            FirebaseAuthState.clearAuthError(context);
            return Result.success();
        } catch (Exception e) {
            if (FirebaseAuthState.isAuthTokenError(e)) {
                FirebaseAuthState.markAuthError(context);
                return Result.success();
            }
            return Result.retry();
        }
    }

    private void runDailyLogCheck(@NonNull Context context, @NonNull String email) throws Exception {
        SharedPreferences prefs = context.getSharedPreferences("FemCarePrefs", Context.MODE_PRIVATE);
        boolean logReminderEnabled = prefs.getBoolean("pref_alert_log", true);

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        String todayKey = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime());

        DocumentSnapshot todayLog = Tasks.await(
            firestore.collection("users")
                .document(email)
                .collection("daily_logs")
                .document(todayKey)
                .get(),
            10,
            TimeUnit.SECONDS
        );

        if (!logReminderEnabled || todayLog.exists()) {
                Tasks.await(
                    firestore.collection("users")
                        .document(email)
                        .collection("notifications")
                        .document("missed_log_" + todayKey)
                        .delete(),
                    10,
                    TimeUnit.SECONDS
                );
            } else {
                NotificationPublisher.publishSync(
                    context,
                    email,
                    "missed_log_" + todayKey,
                    "Missing Daily Log",
                    "You have not tracked your symptoms today. Tap to keep predictions accurate.",
                    "reminder",
                    true
                );
            }

            NotificationPublisher.publishSync(
                context,
                email,
                "quote_" + todayKey,
                "Daily Motivation",
                "Listen to your body; consistent tracking leads to smarter predictions.",
                "quote",
                true
            );

            DocumentSnapshot userDoc = Tasks.await(
                firestore.collection("users").document(email).get(),
                10,
                TimeUnit.SECONDS
            );
            Double bmi = userDoc.getDouble("bmi");
            if (bmi != null && (bmi < 18.5 || bmi > 25.0)) {
                NotificationPublisher.publishSync(
                    context,
                    email,
                    "bmi_insight_" + todayKey,
                    "Health Insight",
                    "Balanced meals, hydration, and sleep consistency can support cycle regularity.",
                    "insight",
                    true
                );
            }
    }

    private void runCycleChecks(@NonNull Context context, @NonNull String email) throws Exception {
        SharedPreferences prefs = context.getSharedPreferences("FemCarePrefs", Context.MODE_PRIVATE);
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        DocumentSnapshot userDoc = Tasks.await(
            firestore.collection("users").document(email).get(),
            10,
            TimeUnit.SECONDS
        );

        if (!userDoc.exists()) {
            return;
        }

        Long lastPeriodStart = userDoc.getLong("lastPeriodStartMillis");
        Long cycleLengthValue = userDoc.getLong("averageCycleLength");
        if (lastPeriodStart == null || cycleLengthValue == null) {
            return;
        }

        int cycleLength = cycleLengthValue.intValue();
        if (cycleLength < 20 || cycleLength > 90) {
            cycleLength = 28;
        }

        long todayMidnight = toMidnight(System.currentTimeMillis());
        long lastPeriodMidnight = toMidnight(lastPeriodStart);

        long daysSinceLast = (todayMidnight - lastPeriodMidnight) / DAY_MILLIS;
        int dayInCycle = (int) (daysSinceLast % cycleLength);
        if (dayInCycle < 0) {
            dayInCycle += cycleLength;
        }

        String todayKey = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(todayMidnight);

        if (prefs.getBoolean("pref_alert_period", true) && dayInCycle == cycleLength - 2) {
            NotificationPublisher.publishSync(
                    context,
                    email,
                    "period_soon_" + todayKey,
                    "Period Reminder",
                    "Your next period is expected in about 2 days. Stay prepared.",
                    "reminder",
                    true
            );
        }

        if (prefs.getBoolean("pref_alert_period", true) && daysSinceLast > 0 && dayInCycle == 0) {
            NotificationPublisher.publishSync(
                    context,
                    email,
                    "period_today_" + todayKey,
                    "Cycle Update",
                    "Your expected period window starts today.",
                    "cycle",
                    true
            );
        }

        if (cycleLength >= 25 && dayInCycle == cycleLength - 19) {
            NotificationPublisher.publishSync(
                    context,
                    email,
                    "fertile_window_" + todayKey,
                    "Fertile Window",
                    "Your fertile window likely starts today.",
                    "cycle",
                    true
            );
        }

        if (prefs.getBoolean("pref_alert_ovulation", false) && dayInCycle == cycleLength - 14) {
            NotificationPublisher.publishSync(
                    context,
                    email,
                    "ovulation_day_" + todayKey,
                    "Ovulation Day",
                    "Today is likely your ovulation day.",
                    "cycle",
                    true
            );
        }
    }

    private void runWeeklyWellnessChecks(@NonNull Context context, @NonNull String email) throws Exception {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        DocumentSnapshot userDoc = Tasks.await(
            firestore.collection("users").document(email).get(),
            10,
            TimeUnit.SECONDS
        );

        if (!userDoc.exists()) {
            return;
        }

        String weekKey = getWeekKey();

        NotificationPublisher.publishSync(
                context,
                email,
                "weekly_motivation_" + weekKey,
                "Daily Motivation",
                "Small, consistent tracking habits lead to better cycle insights. Keep going.",
                "quote",
                true
        );

        Double bmi = userDoc.getDouble("bmi");
        if (bmi != null && (bmi < 18.5 || bmi > 25.0)) {
            NotificationPublisher.publishSync(
                    context,
                    email,
                    "weekly_bmi_insight_" + weekKey,
                    "Health Insight",
                    "Hydration, balanced meals, and sleep consistency can improve cycle comfort.",
                    "insight",
                    true
            );
        }
    }

    private long toMidnight(long timeInMillis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timeInMillis);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private String getWeekKey() {
        Calendar calendar = Calendar.getInstance();
        int week = calendar.get(Calendar.WEEK_OF_YEAR);
        int year = calendar.getWeekYear();
        return year + "_W" + String.format(Locale.US, "%02d", week);
    }

}
