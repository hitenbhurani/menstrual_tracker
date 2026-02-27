package com.miniflo.femcare;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class TodayFragment extends Fragment {

    private TextView todayDateText, nextPeriodDateText, pregnancyChanceText, dailyInsightText;
    private LinearLayout weekStripLayout;
    private View centralHubContainer;
    private ProgressBar loadingSpinner;
    private HexagonProgressBar hexProgressBar;

    private static final long ONE_DAY_MILLIS = 24L * 60L * 60L * 1000L;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_today, container, false);

        todayDateText = view.findViewById(R.id.todayDateText);
        nextPeriodDateText = view.findViewById(R.id.nextPeriodDateText);
        pregnancyChanceText = view.findViewById(R.id.pregnancyChanceText);
        dailyInsightText = view.findViewById(R.id.dailyInsightText);
        weekStripLayout = view.findViewById(R.id.weekStripLayout);
        centralHubContainer = view.findViewById(R.id.centralHubContainer);
        loadingSpinner = view.findViewById(R.id.loadingSpinner);
        hexProgressBar = view.findViewById(R.id.hexProgressBar);

        setupTodayHeaderAndWeekStrip();
        fetchDataAndCalculate();

        return view;
    }

    private void setupTodayHeaderAndWeekStrip() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat headerFormat = new SimpleDateFormat("MMMM d", Locale.getDefault());
        todayDateText.setText("Today, " + headerFormat.format(calendar.getTime()));

        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
        SimpleDateFormat dayLetterFormat = new SimpleDateFormat("E", Locale.getDefault());

        weekStripLayout.removeAllViews();
        for (int i = 0; i < 7; i++) {
            LinearLayout dayCol = new LinearLayout(getContext());
            dayCol.setOrientation(LinearLayout.VERTICAL);
            dayCol.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            dayCol.setLayoutParams(params);

            TextView letterText = new TextView(getContext());
            letterText.setText(dayLetterFormat.format(calendar.getTime()).substring(0, 1));
            letterText.setTextColor(Color.parseColor("#BDBDBD"));
            letterText.setGravity(Gravity.CENTER);

            TextView numberText = new TextView(getContext());
            numberText.setText(String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)));
            numberText.setTextSize(18f);
            numberText.setGravity(Gravity.CENTER);

            Calendar today = Calendar.getInstance();
            if (calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
                numberText.setTextColor(Color.WHITE);
                numberText.setBackgroundResource(R.drawable.circle_background_pink);
            } else {
                numberText.setTextColor(Color.BLACK);
            }

            dayCol.addView(letterText);
            dayCol.addView(numberText);
            weekStripLayout.addView(dayCol);

            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private void fetchDataAndCalculate() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) return;

        FirebaseFirestore.getInstance().collection("users").document(user.getEmail()).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot doc = task.getResult();
                        Long lastPeriodStart = doc.getLong("lastPeriodStartMillis");
                        Long avgCycleLength = doc.getLong("averageCycleLength");

                        Long sleepHours = doc.getLong("sleepHours");
                        Long stressLevel = doc.getLong("stressLevel");
                        Boolean isPregnant = doc.getBoolean("isPregnant");
                        Boolean tryingToConceive = doc.getBoolean("tryingToConceive");

                        if (lastPeriodStart != null && avgCycleLength != null) {
                            runPredictionAlgorithm(lastPeriodStart, avgCycleLength.intValue(),
                                    sleepHours != null ? sleepHours.intValue() : 8,
                                    stressLevel != null ? stressLevel.intValue() : 0,
                                    isPregnant != null ? isPregnant : false,
                                    tryingToConceive != null ? tryingToConceive : false);
                        } else {
                            Toast.makeText(getContext(), "Please complete onboarding.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void runPredictionAlgorithm(long originalLastPeriodMillis, int cycleDays, int sleepHours, int stressLevel, boolean isPregnant, boolean tryingToConceive) {
        Calendar today = Calendar.getInstance();
        long nowMillis = today.getTimeInMillis();
        long cycleLengthMillis = cycleDays * ONE_DAY_MILLIS;

        // --- THE ROLL-FORWARD FIX ---
        // If they missed a period, we roll the clock forward cycle-by-cycle until we find the NEXT future date
        long activeCycleStartMillis = originalLastPeriodMillis;
        long nextPeriodMillis = activeCycleStartMillis + cycleLengthMillis;

        while (nextPeriodMillis < nowMillis) {
            activeCycleStartMillis += cycleLengthMillis;
            nextPeriodMillis = activeCycleStartMillis + cycleLengthMillis;
        }

        // 1. Next Period Date
        Calendar nextPeriod = Calendar.getInstance();
        nextPeriod.setTimeInMillis(nextPeriodMillis);
        SimpleDateFormat format = new SimpleDateFormat("MMMM d", Locale.getDefault());
        nextPeriodDateText.setText(format.format(nextPeriod.getTime()));

        // 2. Fertile Window Calculation (Based on the newly rolled-forward date)
        long ovulationMillis = nextPeriodMillis - (14 * ONE_DAY_MILLIS);
        long fertileWindowStart = ovulationMillis - (5 * ONE_DAY_MILLIS);
        long fertileWindowEnd = ovulationMillis + (1 * ONE_DAY_MILLIS);

        StringBuilder insightBuilder = new StringBuilder();

        if (isPregnant) {
            pregnancyChanceText.setText("N/A - Pregnant");
            insightBuilder.append("Congratulations! Focus on nutrition and prenatal vitamins.");
        } else {
            // Check where they are in the CURRENT rolled-forward cycle
            if (nowMillis >= activeCycleStartMillis && nowMillis < (activeCycleStartMillis + (5 * ONE_DAY_MILLIS))) {
                pregnancyChanceText.setText("Low chance - Period phase");
                insightBuilder.append("Focus on rest and hydration. Your body is working hard. ");
            } else if (nowMillis >= fertileWindowStart && nowMillis <= fertileWindowEnd) {
                pregnancyChanceText.setText(tryingToConceive ? "High Chance - Optimal for conception" : "High Chance (Fertile Window)");
                pregnancyChanceText.setTextColor(Color.parseColor("#C2185B"));
                insightBuilder.append("You might feel more energetic now. It's a great time for active tasks! ");
            } else {
                pregnancyChanceText.setText("Low Chance of getting pregnant");
                insightBuilder.append("You are in the luteal phase. Gentle exercise can help manage PMS. ");

                // If they are officially LATE, tell them!
                if (nowMillis > originalLastPeriodMillis + cycleLengthMillis && nowMillis < nextPeriodMillis - (14 * ONE_DAY_MILLIS)) {
                    insightBuilder.insert(0, "⚠️ Your period is late. This can happen due to stress, hormonal shifts, or pregnancy.\n\n");
                }
            }

            int dayOfYear = today.get(Calendar.DAY_OF_YEAR);
            String[] dailyTips = {
                    "Drinking herbal tea like ginger can help with bloating.",
                    "Magnesium-rich foods like spinach or dark chocolate can improve mood.",
                    "Short 15-minute walks can significantly boost your energy levels.",
                    "Don't forget to track any symptoms you feel today!",
                    "Staying consistent with your wake-up time helps regulate your rhythm."
            };
            insightBuilder.append("\n\nTip of the day: ").append(dailyTips[dayOfYear % dailyTips.length]);
        }

        dailyInsightText.setText(insightBuilder.toString());

        // 3. Hexagonal Progress Calculation
        long totalCycleTime = nextPeriodMillis - activeCycleStartMillis;
        long timePassed = nowMillis - activeCycleStartMillis;
        float progressPercentage = (float) timePassed / totalCycleTime;
        hexProgressBar.setProgress(Math.max(0, Math.min(1, progressPercentage)));

        loadingSpinner.setVisibility(View.GONE);
        centralHubContainer.setVisibility(View.VISIBLE);
    }
}