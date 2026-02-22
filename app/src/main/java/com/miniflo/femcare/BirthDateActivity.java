package com.miniflo.femcare;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class BirthDateActivity extends AppCompatActivity {

    String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
    int currentMonthIndex = 0; // 0 = Jan
    int currentDay = 4;
    int currentYear = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_birth_date);

        if (getSupportActionBar() != null) { getSupportActionBar().hide(); }

        Button nextButton = findViewById(R.id.nextButton);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(BirthDateActivity.this, UserInfoActivity.class);
                startActivity(intent);
            }
        });

        TextView monthText = findViewById(R.id.monthText);
        TextView dayText = findViewById(R.id.dayText);
        TextView yearText = findViewById(R.id.yearText);

        TextView monthUp = findViewById(R.id.monthUp);
        TextView monthDown = findViewById(R.id.monthDown);
        TextView dayUp = findViewById(R.id.dayUp);
        TextView dayDown = findViewById(R.id.dayDown);
        TextView yearUp = findViewById(R.id.yearUp);
        TextView yearDown = findViewById(R.id.yearDown);

        // --- FLIPPED LOGIC ---
        monthUp.setOnClickListener(v -> {
            if (currentMonthIndex > 0) currentMonthIndex--; else currentMonthIndex = 11;
            monthText.setText(months[currentMonthIndex]);
        });
        monthDown.setOnClickListener(v -> {
            if (currentMonthIndex < 11) currentMonthIndex++; else currentMonthIndex = 0;
            monthText.setText(months[currentMonthIndex]);
        });

        dayUp.setOnClickListener(v -> {
            if (currentDay > 1) currentDay--; else currentDay = 31;
            dayText.setText(String.valueOf(currentDay));
        });
        dayDown.setOnClickListener(v -> {
            if (currentDay < 31) currentDay++; else currentDay = 1;
            dayText.setText(String.valueOf(currentDay));
        });

        yearUp.setOnClickListener(v -> {
            if (currentYear > 1900) currentYear--;
            yearText.setText(String.valueOf(currentYear));
        });

        yearDown.setOnClickListener(v -> {
            currentYear++;
            yearText.setText(String.valueOf(currentYear));
        });
    }
}
