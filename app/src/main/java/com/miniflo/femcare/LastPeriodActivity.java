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
    private Button nextButton, notSureButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_last_period);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        CalendarView calendarView = findViewById(R.id.calendarView);
        TextInputEditText durationInput = findViewById(R.id.durationInput);
        nextButton = findViewById(R.id.nextButton);
        notSureButton = findViewById(R.id.notSureButton);

        calendarView.setMaxDate(System.currentTimeMillis());

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, dayOfMonth, 0, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            selectedDateMillis = cal.getTimeInMillis();
        });

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

            int duration;
            try {
                duration = Integer.parseInt(durationStr);
            } catch (NumberFormatException e) {
                durationInput.setError("Invalid number");
                return;
            }

            if (duration < 1 || duration > 15) {
                durationInput.setError("Please enter a valid duration (1-15)");
                return;
            }

            saveDataAndMoveOn(duration, selectedDateMillis);
        });

        notSureButton.setOnClickListener(v -> {
            Calendar defaultCal = Calendar.getInstance();
            defaultCal.add(Calendar.DAY_OF_YEAR, -14);
            saveDataAndMoveOn(5, defaultCal.getTimeInMillis());
        });
    }

    private void saveDataAndMoveOn(int duration, long startMillis) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            setButtonsEnabled(false);
            authViewModel.updatePeriodData(user.getEmail(), duration, startMillis, success -> {
                if (success) {
                    SharedPreferences prefs = getSharedPreferences("FemCarePrefs", MODE_PRIVATE);
                    prefs.edit()
                            .putLong("lastPeriodStartMillis", startMillis)
                            .putInt("periodDuration", duration)
                            .apply();

                    BackgroundTaskScheduler.scheduleAll(LastPeriodActivity.this);
                    BackgroundTaskScheduler.enqueueImmediateSync(LastPeriodActivity.this, "period_data_saved");

                    Intent intent = new Intent(LastPeriodActivity.this, ReproductiveProblemsActivity.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0); // Forces immediate transition
                    finish();
                } else {
                    setButtonsEnabled(true);
                    Toast.makeText(LastPeriodActivity.this, "Failed to save period data. Try again.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            startActivity(new Intent(LastPeriodActivity.this, LoginActivity.class));
            finish();
        }
    }

    private void setButtonsEnabled(boolean enabled) {
        nextButton.setEnabled(enabled);
        notSureButton.setEnabled(enabled);
    }
}
