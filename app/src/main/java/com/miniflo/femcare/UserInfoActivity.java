package com.miniflo.femcare;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import android.view.View;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.miniflo.femcare.viewmodel.AuthViewModel;

public class UserInfoActivity extends AppCompatActivity {
    
    // Timeout protection for cloud saves
    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private static final long CLOUD_SAVE_TIMEOUT_MS = 15_000; // 15 seconds

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

            int h, w;
            try {
                h = Integer.parseInt(heightStr);
                w = Integer.parseInt(weightStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid numbers entered.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (h < 100 || h > 250 || w < 30 || w > 300) {
                Toast.makeText(this, "Please enter valid height and weight.", Toast.LENGTH_SHORT).show();
                return;
            }

            final int heightCm = h;
            final int weightKg = w;
            double heightMeters = heightCm / 100.0;
            double rawBmi = weightKg / (heightMeters * heightMeters);
            final double finalBmi = Math.round(rawBmi * 10.0) / 10.0;

            final boolean isRegular = regularAnswer.equals("Yes");
            final boolean onBirthControl = medAnswer.equals("Yes");

            final int stressLevel;
            switch (stressAnswer) {
                case "Medium": stressLevel = 1; break;
                case "High": stressLevel = 2; break;
                case "Very High": stressLevel = 3; break;
                default: stressLevel = 0; break;
            }

            final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && user.getEmail() != null) {
                final String userEmail = user.getEmail();
                wrapCloudSaveWithTimeout(continueButton, () -> {
                    authViewModel.updateUserInfo(userEmail, isRegular, onBirthControl, stressLevel, heightCm, weightKg, finalBmi, success -> {
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
                });
            } else {
                startActivity(new Intent(UserInfoActivity.this, LoginActivity.class));
                finish();
            }
        });
    }

    private void wrapCloudSaveWithTimeout(View triggerView, Runnable saveLogic) {
        triggerView.setEnabled(false);
        
        // Schedule timeout safety net
        timeoutHandler.postDelayed(() -> {
            if (!triggerView.isEnabled()) {
                // Safety timeout triggered - re-enable button
                triggerView.setEnabled(true);
                Toast.makeText(
                        this,
                        "User info save taking longer than expected. Tap again to retry.",
                        Toast.LENGTH_LONG
                ).show();
            }
        }, CLOUD_SAVE_TIMEOUT_MS);
        
        // Execute the actual save logic
        saveLogic.run();
    }
}
