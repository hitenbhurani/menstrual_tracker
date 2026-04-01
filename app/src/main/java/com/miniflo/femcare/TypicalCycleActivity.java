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
    private Button nextButton, notSureButton;

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
        nextButton = findViewById(R.id.nextButton);
        notSureButton = findViewById(R.id.notSureButton);

        arrowLeft.setOnClickListener(v -> updateDays(-1));
        arrowRight.setOnClickListener(v -> updateDays(1));

        notSureButton.setOnLongClickListener(v -> {
            Toast.makeText(this, "If not sure, we use a 28-day average for now!", Toast.LENGTH_LONG).show();
            return true;
        });

        GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) { return true; }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > 20 && Math.abs(velocityX) > 20) {
                    if (diffX > 0) updateDays(1);
                    else updateDays(-1);
                    return true;
                }
                return false;
            }
        });

        swipeArea.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true;
        });

        View.OnClickListener moveForward = v -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && user.getEmail() != null) {
                setButtonsEnabled(false);
                authViewModel.updateCycleLength(user.getEmail(), currentDays, success -> {
                    if (success) {
                        Intent intent = new Intent(TypicalCycleActivity.this, LastPeriodActivity.class);
                        startActivity(intent);
                        overridePendingTransition(0, 0); // Forces immediate transition
                        finish();
                    } else {
                        setButtonsEnabled(true);
                        Toast.makeText(TypicalCycleActivity.this, "Failed to save cycle length. Try again.", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                startActivity(new Intent(TypicalCycleActivity.this, LoginActivity.class));
                finish();
            }
        };

        nextButton.setOnClickListener(moveForward);
        notSureButton.setOnClickListener(moveForward);
    }

    private void setButtonsEnabled(boolean enabled) {
        nextButton.setEnabled(enabled);
        notSureButton.setEnabled(enabled);
    }

    private void updateDays(int change) {
        currentDays += change;
        if (currentDays < 15) currentDays = 15;
        if (currentDays > 50) currentDays = 50;
        daysText.setText(currentDays + " days");
    }
}
