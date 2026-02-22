package com.miniflo.femcare;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class ReproductiveProblemsActivity extends AppCompatActivity {

    // This stores the user's answer. It starts completely empty!
    String selectedAnswer = "";

    MaterialButton option1, option2, option3, option4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reproductive_problems);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        option1 = findViewById(R.id.option1);
        option2 = findViewById(R.id.option2);
        option3 = findViewById(R.id.option3);
        option4 = findViewById(R.id.option4);
        Button nextButton = findViewById(R.id.nextButton);

        // --- SELECTION LOGIC ---
        option1.setOnClickListener(v -> selectOption(option1, "Yes"));
        option2.setOnClickListener(v -> selectOption(option2, "No"));
        option3.setOnClickListener(v -> selectOption(option3, "No, yet I used to"));
        option4.setOnClickListener(v -> selectOption(option4, "Not sure"));

        // --- STRICT VALIDATION LOGIC ---
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check if the answer is still empty
                if (selectedAnswer.isEmpty()) {
                    // Block them and show an error
                    Toast.makeText(ReproductiveProblemsActivity.this, "Please select an option to continue.", Toast.LENGTH_LONG).show();
                } else {
                    // Allow them to pass
                    Intent intent = new Intent(ReproductiveProblemsActivity.this, UneasinessActivity.class);
                    startActivity(intent);
                }
            }
        });
    }

    // This custom function handles the visual color changing
    private void selectOption(MaterialButton selectedButton, String answer) {
        // 1. Save the answer
        selectedAnswer = answer;

        // 2. Reset ALL buttons to white with a gray outline
        resetButtonVisuals(option1);
        resetButtonVisuals(option2);
        resetButtonVisuals(option3);
        resetButtonVisuals(option4);

        // 3. Highlight ONLY the clicked button with Figma Pink
        selectedButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F8BBD0"))); // Light pink background
        selectedButton.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#F8BBD0"))); // Remove gray border
    }

    // Helper function to reset buttons back to normal
    private void resetButtonVisuals(MaterialButton button) {
        button.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFFFFF")));
        button.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#BDBDBD")));
    }
}