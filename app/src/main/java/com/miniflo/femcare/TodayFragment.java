package com.miniflo.femcare;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class TodayFragment extends Fragment {

    private void setMidnight(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_today, container, false);

        TextView todayDateText = view.findViewById(R.id.todayDateText);
        TextView predictedPeriodText = view.findViewById(R.id.predictedPeriodText);
        TextView chanceText = view.findViewById(R.id.chanceText);

        TextView[] dateViews = {
                view.findViewById(R.id.date1), view.findViewById(R.id.date2),
                view.findViewById(R.id.date3), view.findViewById(R.id.date4),
                view.findViewById(R.id.date5), view.findViewById(R.id.date6),
                view.findViewById(R.id.date7)
        };

        // --- 1. CURRENT CLOCK ---
        Calendar today = Calendar.getInstance();
        setMidnight(today);
        SimpleDateFormat topDateFormat = new SimpleDateFormat("MMMM d", Locale.getDefault());
        todayDateText.setText("Today, " + topDateFormat.format(today.getTime()));

        // --- 2. READ REAL USER DATA FROM DATABASE ---
        SharedPreferences prefs = requireActivity().getSharedPreferences("FemCarePrefs", Context.MODE_PRIVATE);
        long startMillis = prefs.getLong("lastPeriodStartMillis", 0);
        int durationDays = prefs.getInt("periodDuration", 5);

        // Fallback just in case testing data is empty
        if (startMillis == 0) {
            Calendar fallback = Calendar.getInstance();
            fallback.add(Calendar.DAY_OF_YEAR, -10);
            startMillis = fallback.getTimeInMillis();
        }

        // Setup the Past Period
        Calendar lastPeriodStart = Calendar.getInstance();
        lastPeriodStart.setTimeInMillis(startMillis);
        setMidnight(lastPeriodStart);

        Calendar lastPeriodEnd = (Calendar) lastPeriodStart.clone();
        lastPeriodEnd.add(Calendar.DAY_OF_YEAR, durationDays - 1); // Exact End Date

        // Setup the Next Predicted Period (Assume 28 Day Cycle)
        Calendar nextPeriodStart = (Calendar) lastPeriodStart.clone();
        nextPeriodStart.add(Calendar.DAY_OF_YEAR, 28);

        // If the 28 days have already passed, calculate the next one in the future!
        while (nextPeriodStart.before(today)) {
            nextPeriodStart.add(Calendar.DAY_OF_YEAR, 28);
        }

        Calendar nextPeriodEnd = (Calendar) nextPeriodStart.clone();
        nextPeriodEnd.add(Calendar.DAY_OF_YEAR, durationDays - 1);

        // Calculate Pregnancy Chances based on Ovulation (Day 14 of Cycle)
        Calendar ovulationDate = (Calendar) nextPeriodStart.clone();
        ovulationDate.add(Calendar.DAY_OF_YEAR, -14); // Ovulation is usually 14 days before NEXT period

        // Fertile Window is 5 days before ovulation to 1 day after
        Calendar fertileStart = (Calendar) ovulationDate.clone();
        fertileStart.add(Calendar.DAY_OF_YEAR, -5);
        Calendar fertileEnd = (Calendar) ovulationDate.clone();
        fertileEnd.add(Calendar.DAY_OF_YEAR, 1);

        // Update Hexagon Analytics Text dynamically!
        if (!today.before(fertileStart) && !today.after(fertileEnd)) {
            chanceText.setText("High Chance of\ngetting pregnant");
            chanceText.setTextColor(Color.parseColor("#E91E63")); // Make it pop!
        } else if (!today.before(lastPeriodStart) && !today.after(lastPeriodEnd)) {
            chanceText.setText("Low Chance of\ngetting pregnant");
        } else {
            chanceText.setText("Medium Chance of\ngetting pregnant");
        }

        SimpleDateFormat predictedFormat = new SimpleDateFormat("MMMM d", Locale.getDefault());
        predictedPeriodText.setText(predictedFormat.format(nextPeriodStart.getTime()));

        // --- 3. DYNAMIC CALENDAR HIGHLIGHTING ---
        int currentDayOfWeek = today.get(Calendar.DAY_OF_WEEK);
        Calendar weekCalendar = Calendar.getInstance();
        weekCalendar.add(Calendar.DAY_OF_YEAR, -(currentDayOfWeek - 1));
        setMidnight(weekCalendar);

        for (int i = 0; i < 7; i++) {
            int dayNumber = weekCalendar.get(Calendar.DAY_OF_MONTH);
            dateViews[i].setText(String.valueOf(dayNumber));

            // Logic 1: Is this specific box TODAY? (Always Dark Pink Circle)
            if (weekCalendar.equals(today)) {
                dateViews[i].setBackgroundResource(R.drawable.bg_date_selected);
                dateViews[i].setTextColor(Color.WHITE);
            }
            // Logic 2: Is it in the ACTIVE PREDICTED period window? (Light Pink Pill)
            else if (!weekCalendar.before(nextPeriodStart) && !weekCalendar.after(nextPeriodEnd)) {
                dateViews[i].setBackgroundResource(R.drawable.bg_date_dim);
                dateViews[i].setTextColor(Color.BLACK);
            }
            // Logic 3: Is it in the PAST ACTIVE period window? (Light Pink Pill)
            else if (!weekCalendar.before(lastPeriodStart) && !weekCalendar.after(lastPeriodEnd)) {
                dateViews[i].setBackgroundResource(R.drawable.bg_date_dim);
                dateViews[i].setTextColor(Color.BLACK);
            }
            // Normal Day (Transparent)
            else {
                dateViews[i].setBackgroundColor(Color.TRANSPARENT);
                dateViews[i].setTextColor(Color.parseColor("#424242"));
            }

            weekCalendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        return view;
    }
}