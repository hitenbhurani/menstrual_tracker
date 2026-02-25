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

public class TypicalCycleActivity extends AppCompatActivity {

    private int currentDays = 28;
    private TextView daysText;
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_typical_cycle);

        if (getSupportActionBar() != null) { getSupportActionBar().hide(); }

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        daysText = findViewById(R.id.daysText);
        TextView arrowLeft = findViewById(R.id.arrowLeft);
        TextView arrowRight = findViewById(R.id.arrowRight);
        View swipeArea = findViewById(R.id.swipeArea);
        Button nextButton = findViewById(R.id.nextButton);
        Button notSureButton = findViewById(R.id.notSureButton);

        // 1. CLICK GESTURES
        arrowLeft.setOnClickListener(v -> updateDays(-1));
        arrowRight.setOnClickListener(v -> updateDays(1));

        // 2. LONG PRESS GESTURE
        notSureButton.setOnLongClickListener(v -> {
            Toast.makeText(this, "If not sure, we use a 28-day average for now!", Toast.LENGTH_LONG).show();
            return true;
        });

        // 3. HYPER-SENSITIVE SWIPE GESTURE
        GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {

            // THIS IS THE SECRET FIX: Forces Android to register the mouse click before the swipe!
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;
                float diffX = e2.getX() - e1.getX();

                // Lowered threshold to 20 for emulator mouse sensitivity
                if (Math.abs(diffX) > 20 && Math.abs(velocityX) > 20) {
                    if (diffX > 0) updateDays(1); // Swipe Right
                    else updateDays(-1);          // Swipe Left
                    return true;
                }
                return false;
            }
        });

        swipeArea.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true;
        });

        // --- MVVM DB SAVING ---
        View.OnClickListener moveForward = v -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && user.getEmail() != null) {
                authViewModel.updateCycleLength(user.getEmail(), currentDays);
            }
            startActivity(new Intent(TypicalCycleActivity.this, LastPeriodActivity.class));
            finish();
        };

        nextButton.setOnClickListener(moveForward);
        notSureButton.setOnClickListener(moveForward);
    }

    private void updateDays(int change) {
        currentDays += change;
        if (currentDays < 15) currentDays = 15;
        if (currentDays > 50) currentDays = 50;
        daysText.setText(currentDays + " days");
    }
}