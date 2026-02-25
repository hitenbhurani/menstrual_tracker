package com.miniflo.femcare;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.miniflo.femcare.viewmodel.AuthViewModel;
import java.util.Calendar;

public class LastPeriodActivity extends AppCompatActivity {

    private long selectedDateMillis = 0;
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_last_period);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        CalendarView calendarView = findViewById(R.id.calendarView);
        TextInputEditText durationInput = findViewById(R.id.durationInput);
        Button nextButton = findViewById(R.id.nextButton);
        Button notSureButton = findViewById(R.id.notSureButton);

        // Block future dates (Cannot select a period that hasn't happened yet!)
        calendarView.setMaxDate(System.currentTimeMillis());

        // Listen for user calendar taps
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, dayOfMonth, 0, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            selectedDateMillis = cal.getTimeInMillis();
        });

        // --- MAIN BUTTON LOGIC ---
        nextButton.setOnClickListener(v -> {
            String durationStr = durationInput.getText() != null ? durationInput.getText().toString().trim() : "";

            if (selectedDateMillis == 0) {
                Toast.makeText(this, "Please select a start date on the calendar.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (durationStr.isEmpty()) {
                durationInput.setError("Required");
                return;
            }

            int duration = Integer.parseInt(durationStr);
            if (duration < 1 || duration > 15) {
                durationInput.setError("Please enter a valid duration (1-15)");
                return;
            }

            saveDataAndMoveOn(duration, selectedDateMillis);
        });

        // --- "NOT SURE" BUTTON LOGIC ---
        notSureButton.setOnClickListener(v -> {
            // Default to a 5-day period that started 14 days ago
            Calendar defaultCal = Calendar.getInstance();
            defaultCal.add(Calendar.DAY_OF_YEAR, -14);
            saveDataAndMoveOn(5, defaultCal.getTimeInMillis());
        });
    }

    private void saveDataAndMoveOn(int duration, long startMillis) {
        // 1. Save locally for fast Dashboard access
        SharedPreferences prefs = getSharedPreferences("FemCarePrefs", MODE_PRIVATE);
        prefs.edit()
                .putLong("lastPeriodStartMillis", startMillis)
                .putInt("periodDuration", duration)
                .apply();

        // 2. Save securely to Firebase and Room!
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            authViewModel.updatePeriodData(user.getEmail(), duration, startMillis);
        }

        // 3. Move to the next screen
        Intent intent = new Intent(LastPeriodActivity.this, ReproductiveProblemsActivity.class);
        startActivity(intent);
        finish();
    }
}