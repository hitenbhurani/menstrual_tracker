package com.miniflo.femcare;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText emailInput, passwordInput;
    private TextInputLayout emailLayout, passwordLayout;
    private MaterialButton loginButton;
    private ProgressBar progressBar;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        loginButton = findViewById(R.id.loginButton);
        progressBar = findViewById(R.id.loginProgressBar);

        loginButton.setOnClickListener(v -> loginUser());

        findViewById(R.id.registerText).setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class))
        );
    }

    private void loginUser() {
        String email = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
        String password = passwordInput.getText() != null ? passwordInput.getText().toString().trim() : "";

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

        // --- STRICT FIREBASE AUTHENTICATION ---
        progressBar.setVisibility(View.VISIBLE);
        loginButton.setEnabled(false);

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {

                        // User exists! Now pull their specific profile from the cloud.
                        firestore.collection("users").document(email).get()
                                .addOnCompleteListener(dbTask -> {
                                    progressBar.setVisibility(View.GONE);
                                    loginButton.setEnabled(true);

                                    if (dbTask.isSuccessful() && dbTask.getResult() != null) {
                                        DocumentSnapshot document = dbTask.getResult();

                                        // Check if the cloud says they finished onboarding
                                        boolean onboardingComplete = false;
                                        if (document.exists() && document.contains("onboardingComplete")) {
                                            onboardingComplete = Boolean.TRUE.equals(document.getBoolean("onboardingComplete"));
                                        }

                                        // CRITICAL FIX: Overwrite the phone's local memory with this specific user's data!
                                        SharedPreferences prefs = getSharedPreferences("FemCarePrefs", MODE_PRIVATE);
                                        prefs.edit()
                                                .putBoolean("is_logged_in", true)
                                                .putBoolean("onboarding_complete", onboardingComplete)
                                                .apply();

                                        // Route the user
                                        Intent intent;
                                        if (onboardingComplete) {
                                            intent = new Intent(LoginActivity.this, DashboardActivity.class);
                                        } else {
                                            intent = new Intent(LoginActivity.this, BirthDateActivity.class);
                                        }
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        finish();

                                    } else {
                                        Toast.makeText(this, "Failed to sync profile from cloud.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        progressBar.setVisibility(View.GONE);
                        loginButton.setEnabled(true);
                        Toast.makeText(this, "Login Failed. Check email and password.", Toast.LENGTH_LONG).show();
                    }
                });
    }
}