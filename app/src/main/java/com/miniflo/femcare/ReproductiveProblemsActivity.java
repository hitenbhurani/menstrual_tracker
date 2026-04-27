package com.miniflo.femcare;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.miniflo.femcare.viewmodel.AuthViewModel;

public class ReproductiveProblemsActivity extends AppCompatActivity {
    
    // Timeout protection for cloud saves
    private Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private static final long CLOUD_SAVE_TIMEOUT_MS = 15_000; // 15 seconds

    private String selectedAnswer = "";
    private MaterialButton option1, option2, option3, option4;
    private AuthViewModel authViewModel;
    private Button nextButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reproductive_problems);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        option1 = findViewById(R.id.option1);
        option2 = findViewById(R.id.option2);
        option3 = findViewById(R.id.option3);
        option4 = findViewById(R.id.option4);
        nextButton = findViewById(R.id.nextButton);

        option1.setOnClickListener(v -> selectOption(option1, "Yes"));
        option2.setOnClickListener(v -> selectOption(option2, "No"));
        option3.setOnClickListener(v -> selectOption(option3, "No, yet I used to"));
        option4.setOnClickListener(v -> selectOption(option4, "Not sure"));

        nextButton.setOnClickListener(v -> {
            if (selectedAnswer.isEmpty()) {
                Toast.makeText(ReproductiveProblemsActivity.this, "Please select an option to continue.", Toast.LENGTH_LONG).show();
                return;
            }

            boolean hasProblem = selectedAnswer.equals("Yes") || selectedAnswer.equals("No, yet I used to");

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && user.getEmail() != null) {
                wrapCloudSaveWithTimeout(nextButton, () -> {
                    authViewModel.updateReproductiveHealth(user.getEmail(), hasProblem, success -> {
                        if (success) {
                            Intent intent = new Intent(ReproductiveProblemsActivity.this, LifestyleActivity.class);
                            startActivity(intent);
                            overridePendingTransition(0, 0); // Forces immediate transition
                            finish();
                        } else {
                            nextButton.setEnabled(true);
                            Toast.makeText(ReproductiveProblemsActivity.this, "Failed to save data. Try again.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }, "Reproductive health save");
            } else {
                startActivity(new Intent(ReproductiveProblemsActivity.this, LoginActivity.class));
                finish();
            }
        });
    }

    private void selectOption(MaterialButton selectedButton, String answer) {
        selectedAnswer = answer;
        resetButtonVisuals(option1);
        resetButtonVisuals(option2);
        resetButtonVisuals(option3);
        resetButtonVisuals(option4);
        selectedButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F8BBD0")));
        selectedButton.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#F8BBD0")));
    }

    private void resetButtonVisuals(MaterialButton button) {
        button.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFFFFF")));
        button.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#BDBDBD")));
    }

    private void wrapCloudSaveWithTimeout(View triggerView, Runnable saveLogic, String operationName) {
        triggerView.setEnabled(false);
        
        // Schedule timeout safety net
        timeoutHandler.postDelayed(() -> {
            if (!triggerView.isEnabled()) {
                // Safety timeout triggered - re-enable button
                triggerView.setEnabled(true);
                Toast.makeText(
                        this,
                        operationName + " taking longer than expected. Tap again to retry.",
                        Toast.LENGTH_LONG
                ).show();
            }
        }, CLOUD_SAVE_TIMEOUT_MS);
        
        // Execute the actual save logic
        saveLogic.run();
    }
}
