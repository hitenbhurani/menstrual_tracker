package com.miniflo.femcare;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // THIS DRAWS YOUR CUSTOM TEXT SCREEN
        setContentView(R.layout.activity_main);

        // Hide the top action bar so it looks clean
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Delay for 1.5 seconds (1500ms) to let the user read the screen
        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            SharedPreferences prefs = getSharedPreferences("FemCarePrefs", MODE_PRIVATE);
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

        }, 1500);
    }
}