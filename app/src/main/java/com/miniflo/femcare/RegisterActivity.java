package com.miniflo.femcare;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.textfield.TextInputEditText;
import com.miniflo.femcare.viewmodel.AuthViewModel;

public class RegisterActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        TextInputEditText nameInput = findViewById(R.id.regNameInput);
        TextInputEditText emailInput = findViewById(R.id.regEmailInput);
        TextInputEditText passwordInput = findViewById(R.id.regPasswordInput);
        TextInputEditText confirmInput = findViewById(R.id.regConfirmInput);
        Button registerButton = findViewById(R.id.registerButton);
        ProgressBar progressBar = findViewById(R.id.progressBar);

        registerButton.setOnClickListener(v -> {
            String name = getTrimmedText(nameInput);
            String email = getTrimmedText(emailInput);
            String password = getTrimmedText(passwordInput);
            String confirmPass = getTrimmedText(confirmInput);

            // --- STRICT VALIDATION RULES (Age Removed) ---
            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "All fields are required!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailInput.setError("Invalid email format");
                return;
            }

            if (password.length() < 6) {
                passwordInput.setError("Password must be at least 6 characters");
                return;
            }

            if (!password.equals(confirmPass)) {
                confirmInput.setError("Passwords do not match!");
                return;
            }

            // --- MVVM FIREBASE REGISTRATION ---
            progressBar.setVisibility(View.VISIBLE);
            registerButton.setEnabled(false);

            authViewModel.register(email, password, name).observe(this, isSuccess -> {
                progressBar.setVisibility(View.GONE);
                registerButton.setEnabled(true);

                if (isSuccess) {
                    Toast.makeText(this, "Registration Successful!", Toast.LENGTH_SHORT).show();

                    // Route to Onboarding!
                    Intent intent = new Intent(RegisterActivity.this, BirthDateActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, "Registration Failed. Check your network or try another email.", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private String getTrimmedText(TextInputEditText inputEditText) {
        if (inputEditText == null || inputEditText.getText() == null) {
            return "";
        }
        return inputEditText.getText().toString().trim();
    }
}