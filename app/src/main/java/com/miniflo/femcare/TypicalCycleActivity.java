package com.miniflo.femcare;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class TypicalCycleActivity extends AppCompatActivity {

    // Default cycle is set to 28 days
    int currentDays = 28;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_typical_cycle);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Connect the Text and Arrows
        TextView daysText = findViewById(R.id.daysText);
        TextView arrowLeft = findViewById(R.id.arrowLeft);
        TextView arrowRight = findViewById(R.id.arrowRight);

        // --- MATH LOGIC ---
        // Left arrow decreases the number (Minimum 15)
        arrowLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentDays > 15) {
                    currentDays--;
                    daysText.setText(currentDays + " days");
                }
            }
        });

        // Right arrow increases the number (Maximum 50)
        arrowRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentDays < 50) {
                    currentDays++;
                    daysText.setText(currentDays + " days");
                }
            }
        });

        // --- BUTTON LOGIC ---
        Button nextButton = findViewById(R.id.nextButton);
        Button notSureButton = findViewById(R.id.notSureButton);

        // Both buttons will currently take the user to the next screen
        View.OnClickListener moveForward = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TypicalCycleActivity.this, LastPeriodActivity.class);
                startActivity(intent);
            }
        };

        nextButton.setOnClickListener(moveForward);
        notSureButton.setOnClickListener(moveForward);
    }
}