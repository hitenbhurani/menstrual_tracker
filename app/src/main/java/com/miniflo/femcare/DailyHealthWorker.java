package com.miniflo.femcare;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.concurrent.TimeUnit;
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
        if (user == null || user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            return Result.success();
        }

        String email = user.getEmail().trim();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        SharedPreferences prefs = context.getSharedPreferences("FemCarePrefs", Context.MODE_PRIVATE);

        try {
            String todayKey = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime());
                DocumentSnapshot dailyLogDoc = Tasks.await(
                    db.collection("users")
                        .document(email)
                        .collection("daily_logs")
                        .document(todayKey)
                        .get(),
                    10,
                    TimeUnit.SECONDS
                );

            if (!dailyLogDoc.exists() && prefs.getBoolean("pref_alert_log", true)) {
                NotificationHelper.showNotification(
                        context,
                        "Daily Log Reminder",
                        "Don\u0027t forget to log your symptoms today to keep your predictions accurate!"
                );
            }

                DocumentSnapshot doc = Tasks.await(
                    db.collection("users")
                        .document(email)
                        .get(),
                    10,
                    TimeUnit.SECONDS
                );

            if (doc.exists() && doc.contains("lastPeriodStartMillis") && doc.contains("averageCycleLength")) {
                Long lastPeriodMillisValue = doc.getLong("lastPeriodStartMillis");
                Long cycleLengthValue = doc.getLong("averageCycleLength");

                if (lastPeriodMillisValue != null && cycleLengthValue != null && cycleLengthValue > 0) {
                    int cycleLength = cycleLengthValue.intValue();
                    Calendar now = Calendar.getInstance();
                    long diffMillis = now.getTimeInMillis() - lastPeriodMillisValue;
                    int daysDiff = (int) Math.floor(diffMillis / (1000.0 * 60 * 60 * 24));
                    int cycleDay = daysDiff % cycleLength;
                    if (cycleDay < 0) {
                        cycleDay += cycleLength;
                    }

                    if (cycleDay == cycleLength - 2 && prefs.getBoolean("pref_alert_period", true)) {
                        NotificationHelper.showNotification(context, "Period Alert", "Your period is expected to start in about 2 days.");
                    }

                    if (cycleDay == (cycleLength / 2) && prefs.getBoolean("pref_alert_ovulation", false)) {
                        NotificationHelper.showNotification(context, "Ovulation Day", "Today is likely your ovulation day. Check your fertile window in the app!");
                    }
                }
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
}
