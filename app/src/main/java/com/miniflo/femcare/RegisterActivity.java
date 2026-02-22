package com.miniflo.femcare;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText nameInput, emailInput, passwordInput, confirmInput;
    private TextInputLayout nameLayout, emailLayout, passwordLayout, confirmLayout;
    private MaterialButton registerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        nameInput = findViewById(R.id.regNameInput);
        emailInput = findViewById(R.id.regEmailInput);
        passwordInput = findViewById(R.id.regPasswordInput);
        confirmInput = findViewById(R.id.regConfirmInput);

        nameLayout = findViewById(R.id.nameLayout);
        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        confirmLayout = findViewById(R.id.confirmLayout);

        registerButton = findViewById(R.id.registerButton);

        registerButton.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {

        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirm = confirmInput.getText().toString().trim();

        nameLayout.setError(null);
        emailLayout.setError(null);
        passwordLayout.setError(null);
        confirmLayout.setError(null);

        if (TextUtils.isEmpty(name)) {
            nameLayout.setError("Name required");
            return;
        }

        if (TextUtils.isEmpty(email)) {
            emailLayout.setError("Email required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError("Password required");
            return;
        }

        if (password.length() < 6) {
            passwordLayout.setError("Minimum 6 characters required");
            return;
        }

        if (!password.equals(confirm)) {
            confirmLayout.setError("Passwords do not match");
            return;
        }

        SharedPreferences prefs = getSharedPreferences("FemCarePrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("name", name);
        editor.putString("email", email);
        editor.putString("password", password);

        editor.putBoolean("is_logged_in", true);
        editor.putBoolean("onboarding_complete", false);

        editor.apply();

        Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(RegisterActivity.this, BirthDateActivity.class);
        startActivity(intent);
        finish();
    }
}