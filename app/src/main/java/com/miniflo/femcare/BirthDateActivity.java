package com.miniflo.femcare;

import android.content.Intent;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.miniflo.femcare.viewmodel.AuthViewModel;
import java.util.Calendar;

public class BirthDateActivity extends AppCompatActivity {

    String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
    int currentMonthIndex = 0;
    int currentDay = 4;
    int currentYear = 2000;

    private AuthViewModel authViewModel;
    private TextView monthText, dayText, yearText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_birth_date);

        if (getSupportActionBar() != null) { getSupportActionBar().hide(); }

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        monthText = findViewById(R.id.monthText);
        dayText = findViewById(R.id.dayText);
        yearText = findViewById(R.id.yearText);
        Button nextButton = findViewById(R.id.nextButton);

        // Map Click events
        findViewById(R.id.monthUp).setOnClickListener(v -> changeMonth(-1));
        findViewById(R.id.monthDown).setOnClickListener(v -> changeMonth(1));
        findViewById(R.id.dayUp).setOnClickListener(v -> changeDay(-1));
        findViewById(R.id.dayDown).setOnClickListener(v -> changeDay(1));
        findViewById(R.id.yearUp).setOnClickListener(v -> changeYear(-1));
        findViewById(R.id.yearDown).setOnClickListener(v -> changeYear(1));

        // --- GESTURES: VERTICAL SWIPE ENGINE ---
        setupVerticalSwipe(findViewById(R.id.monthColumn), () -> changeMonth(-1), () -> changeMonth(1));
        setupVerticalSwipe(findViewById(R.id.dayColumn), () -> changeDay(-1), () -> changeDay(1));
        setupVerticalSwipe(findViewById(R.id.yearColumn), () -> changeYear(-1), () -> changeYear(1));

        // --- MATH & DATABASE ENGINE ---
        nextButton.setOnClickListener(v -> {
            Calendar today = Calendar.getInstance();
            int age = today.get(Calendar.YEAR) - currentYear;

            if (today.get(Calendar.MONTH) < currentMonthIndex ||
                    (today.get(Calendar.MONTH) == currentMonthIndex && today.get(Calendar.DAY_OF_MONTH) < currentDay)) {
                age--;
            }

            if (age < 10 || age > 100) {
                Toast.makeText(this, "Please select a valid birth date.", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && user.getEmail() != null) {
                authViewModel.updateAge(user.getEmail(), age);
            }

            startActivity(new Intent(BirthDateActivity.this, UserInfoActivity.class));
            finish();
        });
    }

    private void changeMonth(int change) {
        currentMonthIndex += change;
        if (currentMonthIndex < 0) currentMonthIndex = 11;
        if (currentMonthIndex > 11) currentMonthIndex = 0;
        monthText.setText(months[currentMonthIndex]);
    }

    private void changeDay(int change) {
        currentDay += change;
        if (currentDay < 1) currentDay = 31;
        if (currentDay > 31) currentDay = 1;
        dayText.setText(String.valueOf(currentDay));
    }

    private void changeYear(int change) {
        currentYear += change;
        if (currentYear < 1900) currentYear = 1900;
        yearText.setText(String.valueOf(currentYear));
    }

    // --- HYPER-SENSITIVE SWIPE GESTURE DETECTOR ---
    private void setupVerticalSwipe(View columnView, Runnable onSwipeUp, Runnable onSwipeDown) {
        GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {

            // SECRET FIX: Makes the emulator notice the mouse press immediately!
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;
                float diffY = e2.getY() - e1.getY();

                // Lowered threshold to 10
                if (Math.abs(diffY) > 10 && Math.abs(velocityY) > 10) {
                    if (diffY > 0) onSwipeDown.run(); // Swiped Down
                    else onSwipeUp.run();             // Swiped Up
                    return true;
                }
                return false;
            }
        });

        columnView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true;
        });
    }
}