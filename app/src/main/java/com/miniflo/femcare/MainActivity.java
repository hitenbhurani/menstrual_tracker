package com.miniflo.femcare;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Theme logic must come before super.onCreate
        SharedPreferences prefs = getSharedPreferences("FemCarePrefs", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("pref_dark_mode", false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);

        // THIS DRAWS YOUR CUSTOM TEXT SCREEN
        setContentView(R.layout.activity_main);

        // Hide the top action bar so it looks clean
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Delay for 1.75 seconds (1750ms) to let the user read the screen
        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            boolean onboardingComplete = prefs.getBoolean("onboarding_complete", false);

            Intent intent;

            if (currentUser == null) {
                intent = new Intent(MainActivity.this, LoginActivity.class);
            }
            else if (!onboardingComplete) {
                intent = new Intent(MainActivity.this, BirthDateActivity.class);
            }
            else {
                intent = new Intent(MainActivity.this, DashboardActivity.class);
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

            finish();

        }, 1750);
    }
}
