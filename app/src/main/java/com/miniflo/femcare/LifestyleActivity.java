package com.miniflo.femcare;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.miniflo.femcare.viewmodel.AuthViewModel;

public class LifestyleActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;
    private Button nextButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lifestyle);

        if (getSupportActionBar() != null) { getSupportActionBar().hide(); }

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        String[] goalOptions = {"Track my cycle", "Trying to conceive", "Track pregnancy"};
        String[] exerciseOptions = {"Rarely", "Light (1-2x a week)", "Moderate (3-4x a week)", "Active (5+ a week)"};

        ArrayAdapter<String> goalAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, goalOptions);
        ArrayAdapter<String> exerciseAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, exerciseOptions);

        AutoCompleteTextView dropdownGoal = findViewById(R.id.dropdownGoal);
        AutoCompleteTextView dropdownExercise = findViewById(R.id.dropdownExercise);
        TextInputEditText sleepInput = findViewById(R.id.sleepInput);
        nextButton = findViewById(R.id.nextButton);

        dropdownGoal.setAdapter(goalAdapter);
        dropdownExercise.setAdapter(exerciseAdapter);

        nextButton.setOnClickListener(v -> {
            String goalStr = dropdownGoal.getText().toString();
            String exerciseStr = dropdownExercise.getText().toString();
            String sleepStr = sleepInput.getText() != null ? sleepInput.getText().toString().trim() : "";

            if (goalStr.isEmpty() || exerciseStr.isEmpty() || sleepStr.isEmpty()) {
                Toast.makeText(this, "Please fill out all fields.", Toast.LENGTH_SHORT).show();
                return;
            }

            int sleepHours;
            try {
                sleepHours = Integer.parseInt(sleepStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid sleep hours.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (sleepHours < 1 || sleepHours > 20) {
                Toast.makeText(this, "Please enter valid sleep hours.", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean tryingToConceive = goalStr.equals("Trying to conceive");
            boolean isPregnant = goalStr.equals("Track pregnancy");

            int exerciseFrequency = 0;
            switch (exerciseStr) {
                case "Light (1-2x a week)": exerciseFrequency = 1; break;
                case "Moderate (3-4x a week)": exerciseFrequency = 2; break;
                case "Active (5+ a week)": exerciseFrequency = 3; break;
            }

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && user.getEmail() != null) {
                nextButton.setEnabled(false);
                authViewModel.updateLifestyleData(user.getEmail(), isPregnant, tryingToConceive, sleepHours, exerciseFrequency, success -> {
                    if (success) {
                        Intent intent = new Intent(LifestyleActivity.this, UneasinessActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        nextButton.setEnabled(true);
                        Toast.makeText(LifestyleActivity.this, "Failed to save lifestyle data. Try again.", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                startActivity(new Intent(LifestyleActivity.this, LoginActivity.class));
                finish();
            }
        });
    }
}
