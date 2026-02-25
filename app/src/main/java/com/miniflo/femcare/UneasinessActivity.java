package com.miniflo.femcare;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.miniflo.femcare.viewmodel.AuthViewModel;
import java.util.ArrayList;

public class UneasinessActivity extends AppCompatActivity {

    ArrayList<MaterialButton> symptomButtons = new ArrayList<>();
    TextInputLayout otherInputLayout;
    TextInputEditText otherInput;
    MaterialButton symp7; // "Other" button

    private AuthViewModel authViewModel;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uneasiness);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Finalizing onboarding...");
        progressDialog.setCancelable(false);

        MaterialButton symp1 = findViewById(R.id.symp1);
        MaterialButton symp2 = findViewById(R.id.symp2);
        MaterialButton symp3 = findViewById(R.id.symp3);
        MaterialButton symp4 = findViewById(R.id.symp4);
        MaterialButton symp5 = findViewById(R.id.symp5);
        MaterialButton symp6 = findViewById(R.id.symp6);
        symp7 = findViewById(R.id.symp7);

        otherInputLayout = findViewById(R.id.otherInputLayout);
        otherInput = findViewById(R.id.otherInput);
        Button nextButton = findViewById(R.id.nextButton);

        symptomButtons.add(symp1);
        symptomButtons.add(symp2);
        symptomButtons.add(symp3);
        symptomButtons.add(symp4);
        symptomButtons.add(symp5);
        symptomButtons.add(symp6);
        symptomButtons.add(symp7);

        for (MaterialButton button : symptomButtons) {
            button.setTag("unselected");
            button.setOnClickListener(v -> toggleButtonColor(button));
        }

        // --- FINAL SUBMISSION LOGIC ---
        nextButton.setOnClickListener(v -> {

            ArrayList<String> selectedSymptoms = new ArrayList<>();

            // Collect all selected symptoms
            for (MaterialButton button : symptomButtons) {
                if (button.getTag().equals("selected")) {
                    if (button == symp7) {
                        selectedSymptoms.add("Other: " + otherInput.getText().toString().trim());
                    } else {
                        selectedSymptoms.add(button.getText().toString());
                    }
                }
            }

            // Validation 1: Did they select anything?
            if (selectedSymptoms.isEmpty()) {
                Toast.makeText(UneasinessActivity.this, "Please select at least one option.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Validation 2: Did they fill out the "Other" text box?
            if (symp7.getTag().equals("selected") && otherInput.getText().toString().trim().isEmpty()) {
                otherInputLayout.setError("Please specify your symptom");
                return;
            } else {
                otherInputLayout.setError(null);
            }

            // --- 1. SAVE TO FIREBASE & ROOM ---
            // Convert list to a comma-separated string for easy database storage
            String symptomsString = String.join(", ", selectedSymptoms);

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && user.getEmail() != null) {
                progressDialog.show();
                authViewModel.finalizeOnboarding(user.getEmail(), symptomsString).observe(this, success -> {
                    progressDialog.dismiss();
                    if (success) {
                        // --- 2. MARK LOCAL STORAGE COMPLETE ---
                        SharedPreferences prefs = getSharedPreferences("FemCarePrefs", MODE_PRIVATE);
                        prefs.edit().putBoolean("onboarding_complete", true).apply();

                        // --- 3. GO TO DASHBOARD ---
                        Intent intent = new Intent(UneasinessActivity.this, DashboardActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, "Failed to save profile. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void toggleButtonColor(MaterialButton button) {
        if (button.getTag().equals("unselected")) {
            button.setTag("selected");
            button.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F8BBD0")));
            button.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#F8BBD0")));

            if (button == symp7) {
                otherInputLayout.setVisibility(View.VISIBLE);
            }
        } else {
            button.setTag("unselected");
            button.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFFFFF")));
            button.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#BDBDBD")));

            if (button == symp7) {
                otherInputLayout.setVisibility(View.GONE);
                otherInput.setText("");
            }
        }
    }
}