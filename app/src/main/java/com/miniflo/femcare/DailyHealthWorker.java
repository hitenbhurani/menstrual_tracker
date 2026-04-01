package com.miniflo.femcare;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DailyHealthWorker extends Worker {

    public DailyHealthWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        
        if (user == null) return Result.success();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        SharedPreferences prefs = context.getSharedPreferences("FemCarePrefs", Context.MODE_PRIVATE);

        // 1. Check for Daily Logging Reminder (at 8 PM)
        String todayKey = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime());
        db.collection("users").document(user.getEmail()).collection("daily_logs").document(todayKey).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        NotificationHelper.showNotification(context, 
                                "Daily Log Reminder", 
                                "Don\u0027t forget to log your symptoms today to keep your predictions accurate!");
                    }
                });

        // 2. Check for Period/Ovulation Alerts
        db.collection("users").document(user.getEmail()).get().addOnSuccessListener(doc -> {
            if (doc.exists() \u0026\u0026 doc.contains("lastPeriodStartMillis") \u0026\u0026 doc.contains("averageCycleLength")) {
                long lastPeriodMillis = doc.getLong("lastPeriodStartMillis");
                int cycleLength = doc.getLong("averageCycleLength").intValue();

                Calendar now = Calendar.getInstance();
                long diffMillis = now.getTimeInMillis() - lastPeriodMillis;
                int daysDiff = (int) Math.floor(diffMillis / (1000.0 * 60 * 60 * 24));
                int cycleDay = (daysDiff % cycleLength);
                if (cycleDay \u003c 0) cycleDay += cycleLength;

                // Alert 2 days before period
                if (cycleDay == cycleLength - 2 \u0026\u0026 prefs.getBoolean("pref_alert_period", true)) {
                    NotificationHelper.showNotification(context, "Period Alert", "Your period is expected to start in about 2 days.");
                }

                // Alert on ovulation day (approx day 14 in a 28 day cycle)
                if (cycleDay == (cycleLength / 2) \u0026\u0026 prefs.getBoolean("pref_alert_ovulation", false)) {
                    NotificationHelper.showNotification(context, "Ovulation Day", "Today is likely your ovulation day. Check your fertile window in the app!");
                }
            }
        });

        return Result.success();
    }
}