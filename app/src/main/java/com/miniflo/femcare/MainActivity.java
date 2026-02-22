package com.miniflo.femcare;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            SharedPreferences prefs = getSharedPreferences("FemCarePrefs", MODE_PRIVATE);

            boolean isLoggedIn = prefs.getBoolean("is_logged_in", false);
            boolean onboardingComplete = prefs.getBoolean("onboarding_complete", false);
            Toast.makeText(this,
                    "LoggedIn: " + isLoggedIn + " | Onboarding: " + onboardingComplete,
                    Toast.LENGTH_LONG).show();

            Intent intent;

            if (!isLoggedIn) {
                // User not logged in
                intent = new Intent(MainActivity.this, LoginActivity.class);
            }
            else if (!onboardingComplete) {
                // User logged in but onboarding incomplete
                intent = new Intent(MainActivity.this, BirthDateActivity.class);
            }
            else {
                // User fully set up
                intent = new Intent(MainActivity.this, DashboardActivity.class);
            }

            startActivity(intent);
            finish();

        }, 3000);
    }
}