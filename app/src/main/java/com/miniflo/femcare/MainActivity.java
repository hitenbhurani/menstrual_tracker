package com.miniflo.femcare;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // I changed this from 3000ms to 1500ms (1.5 seconds) - it gives a much snappier, premium feel!
        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            // --- 1. FIREBASE AUTH CHECK ---
            // Ask Firebase securely if a user is currently logged in
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

            // --- 2. ONBOARDING CHECK ---
            // Still use SharedPreferences to check if they finished the setup screens
            SharedPreferences prefs = getSharedPreferences("FemCarePrefs", MODE_PRIVATE);
            boolean onboardingComplete = prefs.getBoolean("onboarding_complete", false);

            // Handy debug toast so you know exactly what the engine is doing!
            Toast.makeText(this,
                    "Firebase Logged In: " + (currentUser != null) + " | Onboarding: " + onboardingComplete,
                    Toast.LENGTH_LONG).show();

            Intent intent;

            // --- 3. THE TRAFFIC COP ROUTING ---
            if (currentUser == null) {
                // User not logged into Firebase -> Send to Login
                intent = new Intent(MainActivity.this, LoginActivity.class);
            }
            else if (!onboardingComplete) {
                // User logged in but onboarding incomplete -> Send to BirthDateActivity
                intent = new Intent(MainActivity.this, BirthDateActivity.class);
            }
            else {
                // User fully logged in AND set up -> Send to Dashboard
                intent = new Intent(MainActivity.this, DashboardActivity.class);
            }

            startActivity(intent);
            finish();

        }, 1500);
    }
}