package com.miniflo.femcare;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import java.util.Calendar;

public class LastPeriodActivity extends AppCompatActivity {

    long selectedDateMillis = 0; // Stores the exact millisecond time of the chosen date

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_last_period);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        CalendarView calendarView = findViewById(R.id.calendarView);
        TextInputEditText durationInput = findViewById(R.id.durationInput);
        Button nextButton = findViewById(R.id.nextButton);
        Button notSureButton = findViewById(R.id.notSureButton);

        calendarView.setMaxDate(System.currentTimeMillis()); // Block future dates

        // Listen for user calendar taps
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, dayOfMonth, 0, 0, 0); // Zero out the clock!
            cal.set(Calendar.MILLISECOND, 0);
            selectedDateMillis = cal.getTimeInMillis();
        });

        nextButton.setOnClickListener(v -> {
            String durationStr = durationInput.getText().toString().trim();

            if (selectedDateMillis == 0) {
                Toast.makeText(this, "Please select a start date.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (durationStr.isEmpty()) {
                Toast.makeText(this, "Please enter how many days it lasted.", Toast.LENGTH_SHORT).show();
                return;
            }

            int duration = Integer.parseInt(durationStr);

            // --- SAVE REAL DATA TO DATABASE ---
            SharedPreferences prefs = getSharedPreferences("FemCarePrefs", MODE_PRIVATE);
            prefs.edit()
                    .putLong("lastPeriodStartMillis", selectedDateMillis)
                    .putInt("periodDuration", duration)
                    .apply();

            // Proceed to next screen
            startActivity(new Intent(LastPeriodActivity.this, ReproductiveProblemsActivity.class));
        });

        notSureButton.setOnClickListener(v -> {
            // If they aren't sure, set a default 28-day cycle starting 14 days ago so the app doesn't crash
            Calendar defaultCal = Calendar.getInstance();
            defaultCal.add(Calendar.DAY_OF_YEAR, -14);
            SharedPreferences prefs = getSharedPreferences("FemCarePrefs", MODE_PRIVATE);
            prefs.edit().putLong("lastPeriodStartMillis", defaultCal.getTimeInMillis()).putInt("periodDuration", 5).apply();

            startActivity(new Intent(LastPeriodActivity.this, ReproductiveProblemsActivity.class));
        });
    }
}