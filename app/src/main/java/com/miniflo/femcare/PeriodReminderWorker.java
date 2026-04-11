package com.miniflo.femcare;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.miniflo.femcare.data.AppDatabase;
import com.miniflo.femcare.data.CycleEntity;
import java.util.Calendar;
import java.util.List;

public class PeriodReminderWorker extends Worker {

    public PeriodReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        List<CycleEntity> cycles = db.cycleDao().getRecentCyclesForMath();

        if (cycles != null && !cycles.isEmpty()) {
            CycleEntity lastCycle = cycles.get(0);
            
            // Logic: Predict next period
            // Next Start = Last Start + Average Cycle Length (defaulting to 28 if not enough data)
            int avgLength = lastCycle.cycleLength > 0 ? lastCycle.cycleLength : 28;
            
            Calendar nextPeriod = Calendar.getInstance();
            nextPeriod.setTimeInMillis(lastCycle.startDateMillis);
            nextPeriod.add(Calendar.DAY_OF_YEAR, avgLength);

            Calendar today = Calendar.getInstance();
            
            // Reminder 1 day before
            nextPeriod.add(Calendar.DAY_OF_YEAR, -1);
            
            if (isSameDay(today, nextPeriod)) {
                NotificationHelper.showNotification(
                    getApplicationContext(),
                    "Period Reminder",
                    "Your cycle is expected to start tomorrow. Stay prepared!"
                );
            }
        }

        return Result.success();
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }
}
