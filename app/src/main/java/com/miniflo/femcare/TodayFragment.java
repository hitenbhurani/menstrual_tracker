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
import java.util.Random;

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

        weekStripLayout.removeAllViews(); // Clear existing views if any
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
                        
                        // Extracting more user data for personalized insights
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
                            Toast.makeText(getContext(), "Please complete onboarding to see predictions.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void runPredictionAlgorithm(long lastPeriodMillis, int cycleDays, int sleepHours, int stressLevel, boolean isPregnant, boolean tryingToConceive) {
        Calendar today = Calendar.getInstance();
        long nowMillis = today.getTimeInMillis();

        // 1. Next Period Date
        long cycleLengthMillis = cycleDays * ONE_DAY_MILLIS;
        long nextPeriodMillis = lastPeriodMillis + cycleLengthMillis;
        Calendar nextPeriod = Calendar.getInstance();
        nextPeriod.setTimeInMillis(nextPeriodMillis);
        SimpleDateFormat format = new SimpleDateFormat("MMMM d", Locale.getDefault());
        nextPeriodDateText.setText(format.format(nextPeriod.getTime()));

        // 2. Fertile Window Calculation
        long ovulationMillis = nextPeriodMillis - (14 * ONE_DAY_MILLIS);
        long fertileWindowStart = ovulationMillis - (5 * ONE_DAY_MILLIS);
        long fertileWindowEnd = ovulationMillis + (1 * ONE_DAY_MILLIS);

        StringBuilder insightBuilder = new StringBuilder();

        if (isPregnant) {
            pregnancyChanceText.setText("N/A - Pregnant");
            insightBuilder.append("Congratulations! Focus on nutrition and prenatal vitamins. Stay hydrated and get plenty of rest.");
        } else {
            if (nowMillis >= lastPeriodMillis && nowMillis < (lastPeriodMillis + (5 * ONE_DAY_MILLIS))) {
                // Menstrual Phase
                pregnancyChanceText.setText("Low chance - Period phase");
                insightBuilder.append("Focus on rest and hydration. Your body is working hard. ");
                if (sleepHours < 7) insightBuilder.append("Try to get more sleep tonight. ");
            } else if (nowMillis >= fertileWindowStart && nowMillis <= fertileWindowEnd) {
                // Ovulation/Fertile Phase
                pregnancyChanceText.setText(tryingToConceive ? "High Chance - Optimal for conception" : "High Chance (Fertile Window)");
                pregnancyChanceText.setTextColor(Color.parseColor("#C2185B"));
                insightBuilder.append("You might feel more energetic now. It's a great time for active tasks and socializing! ");
            } else {
                // Luteal Phase
                pregnancyChanceText.setText("Low Chance of getting pregnant");
                insightBuilder.append("You are in the luteal phase. Gentle exercise like yoga can help manage potential PMS symptoms. ");
            }

            // Daily Refresh Logic based on day of year to ensure it changes daily but stays same for the day
            int dayOfYear = today.get(Calendar.DAY_OF_YEAR);
            String[] dailyTips = {
                "Drinking herbal tea like ginger can help with bloating.",
                "Magnesium-rich foods like spinach or dark chocolate can improve mood.",
                "Short 15-minute walks can significantly boost your energy levels.",
                "Don't forget to track any symptoms you feel today for better future predictions.",
                "Staying consistent with your wake-up time helps regulate your circadian rhythm.",
                "Iron-rich foods are especially beneficial if you're feeling a bit sluggish.",
                "Self-care isn't selfish; take 10 minutes today just for yourself."
            };
            insightBuilder.append("\n\nTip of the day: ").append(dailyTips[dayOfYear % dailyTips.length]);

            if (stressLevel > 1) {
                insightBuilder.append("\n\nNotice: You've mentioned high stress lately. Consider deep breathing exercises.");
            }
        }

        dailyInsightText.setText(insightBuilder.toString());

        // 3. Hexagonal Progress Calculation
        long totalCycleTime = nextPeriodMillis - lastPeriodMillis;
        long timePassed = nowMillis - lastPeriodMillis;
        float progressPercentage = (float) timePassed / totalCycleTime;
        hexProgressBar.setProgress(Math.max(0, Math.min(1, progressPercentage)));

        loadingSpinner.setVisibility(View.GONE);
        centralHubContainer.setVisibility(View.VISIBLE);
    }
}
