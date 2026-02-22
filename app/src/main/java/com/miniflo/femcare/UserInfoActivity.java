package com.miniflo.femcare;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class UserInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        String[] yesNoOptions = {"Yes", "No"};
        String[] regularOptions = {"Yes", "No", "I don't know"};
        String[] stressOptions = {"Low", "Medium", "High", "Very High"};

        ArrayAdapter<String> yesNoAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, yesNoOptions);
        ArrayAdapter<String> regularAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, regularOptions);
        ArrayAdapter<String> stressAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, stressOptions);

        AutoCompleteTextView dropdownPill = findViewById(R.id.dropdownPill);
        AutoCompleteTextView dropdownRegular = findViewById(R.id.dropdownRegular);
        AutoCompleteTextView dropdownMedication = findViewById(R.id.dropdownMedication);
        AutoCompleteTextView dropdownStress = findViewById(R.id.dropdownStress);

        dropdownPill.setAdapter(yesNoAdapter);
        dropdownRegular.setAdapter(regularAdapter);
        dropdownMedication.setAdapter(yesNoAdapter);
        dropdownStress.setAdapter(stressAdapter);

        Button continueButton = findViewById(R.id.continueButton);

        // --- NEW STRICT VALIDATION LOGIC ---
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Get the current text from all 4 boxes
                String pillAnswer = dropdownPill.getText().toString();
                String regularAnswer = dropdownRegular.getText().toString();
                String medAnswer = dropdownMedication.getText().toString();
                String stressAnswer = dropdownStress.getText().toString();

                // Check if ANY of them are completely empty
                if (pillAnswer.isEmpty() || regularAnswer.isEmpty() || medAnswer.isEmpty() || stressAnswer.isEmpty()) {
                    // Block them and show an error message
                    Toast.makeText(UserInfoActivity.this, "Please answer all questions before continuing.", Toast.LENGTH_LONG).show();
                } else {
                    // All questions are answered, allow them to pass!
                    Intent intent = new Intent(UserInfoActivity.this, TypicalCycleActivity.class);
                    startActivity(intent);
                }
            }
        });
    }
}