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

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText emailInput, passwordInput;
    private TextInputLayout emailLayout, passwordLayout;
    private MaterialButton loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        loginButton = findViewById(R.id.loginButton);

        loginButton.setOnClickListener(v -> loginUser());

        findViewById(R.id.registerText).setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class))
        );
    }

    private void loginUser() {

        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        emailLayout.setError(null);
        passwordLayout.setError(null);

        if (TextUtils.isEmpty(email)) {
            emailLayout.setError("Email required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError("Password required");
            return;
        }

        SharedPreferences prefs = getSharedPreferences("FemCarePrefs", MODE_PRIVATE);

        String savedEmail = prefs.getString("email", null);
        String savedPassword = prefs.getString("password", null);

        if (savedEmail == null) {
            Toast.makeText(this, "User not registered", Toast.LENGTH_LONG).show();
            return;
        }

        if (!email.equals(savedEmail)) {
            Toast.makeText(this, "User does not exist", Toast.LENGTH_LONG).show();
            return;
        }

        if (!password.equals(savedPassword)) {
            Toast.makeText(this, "Invalid password", Toast.LENGTH_LONG).show();
            return;
        }

        // 🔥 Mark user logged in
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("is_logged_in", true);
        editor.apply();

        boolean onboardingComplete = prefs.getBoolean("onboarding_complete", false);

        Intent intent;

        if (onboardingComplete) {
            intent = new Intent(LoginActivity.this, DashboardActivity.class);
        } else {
            intent = new Intent(LoginActivity.this, BirthDateActivity.class);
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}