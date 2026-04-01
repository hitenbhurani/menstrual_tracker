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

public class UserInfoActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;
    private Button continueButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);

        if (getSupportActionBar() != null) { getSupportActionBar().hide(); }

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        String[] yesNoOptions = {"Yes", "No"};
        String[] regularOptions = {"Yes", "No", "I don't know"};
        String[] stressOptions = {"Low", "Medium", "High", "Very High"};

        ArrayAdapter<String> yesNoAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, yesNoOptions);
        ArrayAdapter<String> regularAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, regularOptions);
        ArrayAdapter<String> stressAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, stressOptions);

        TextInputEditText heightInput = findViewById(R.id.heightInput);
        TextInputEditText weightInput = findViewById(R.id.weightInput);
        AutoCompleteTextView dropdownRegular = findViewById(R.id.dropdownRegular);
        AutoCompleteTextView dropdownMedication = findViewById(R.id.dropdownMedication);
        AutoCompleteTextView dropdownStress = findViewById(R.id.dropdownStress);

        dropdownRegular.setAdapter(regularAdapter);
        dropdownMedication.setAdapter(yesNoAdapter);
        dropdownStress.setAdapter(stressAdapter);

        continueButton = findViewById(R.id.continueButton);

        continueButton.setOnClickListener(v -> {
            String heightStr = heightInput.getText() != null ? heightInput.getText().toString().trim() : "";
            String weightStr = weightInput.getText() != null ? weightInput.getText().toString().trim() : "";
            String regularAnswer = dropdownRegular.getText().toString();
            String medAnswer = dropdownMedication.getText().toString();
            String stressAnswer = dropdownStress.getText().toString();

            if (heightStr.isEmpty() || weightStr.isEmpty() || regularAnswer.isEmpty() || medAnswer.isEmpty() || stressAnswer.isEmpty()) {
                Toast.makeText(this, "Please answer all questions.", Toast.LENGTH_SHORT).show();
                return;
            }

            int heightCm, weightKg;
            try {
                heightCm = Integer.parseInt(heightStr);
                weightKg = Integer.parseInt(weightStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid numbers entered.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (heightCm < 100 || heightCm > 250 || weightKg < 30 || weightKg > 300) {
                Toast.makeText(this, "Please enter valid height and weight.", Toast.LENGTH_SHORT).show();
                return;
            }

            double heightMeters = heightCm / 100.0;
            double rawBmi = weightKg / (heightMeters * heightMeters);
            double finalBmi = Math.round(rawBmi * 10.0) / 10.0;

            boolean isRegular = regularAnswer.equals("Yes");
            boolean onBirthControl = medAnswer.equals("Yes");

            int stressLevel = 0;
            switch (stressAnswer) {
                case "Medium": stressLevel = 1; break;
                case "High": stressLevel = 2; break;
                case "Very High": stressLevel = 3; break;
            }

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && user.getEmail() != null) {
                continueButton.setEnabled(false);
                authViewModel.updateUserInfo(user.getEmail(), isRegular, onBirthControl, stressLevel, heightCm, weightKg, finalBmi, success -> {
                    if (success) {
                        Intent intent = new Intent(UserInfoActivity.this, TypicalCycleActivity.class);
                        startActivity(intent);
                        overridePendingTransition(0, 0); // Forces immediate transition
                        finish();
                    } else {
                        continueButton.setEnabled(true);
                        Toast.makeText(UserInfoActivity.this, "Failed to save info. Try again.", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                startActivity(new Intent(UserInfoActivity.this, LoginActivity.class));
                finish();
            }
        });
    }
}
