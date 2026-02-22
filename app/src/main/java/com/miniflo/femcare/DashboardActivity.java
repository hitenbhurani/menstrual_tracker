package com.miniflo.femcare;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class DashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        Button tempSignOutButton = findViewById(R.id.tempSignOutButton);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        FloatingActionButton fabTrack = findViewById(R.id.fabTrack);

        // --- LOAD DEFAULT FRAGMENT (Today) ---
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new TodayFragment()).commit();
        }

        // --- SIGN OUT LOGIC ---
        tempSignOutButton.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("FemCarePrefs", MODE_PRIVATE);
            prefs.edit().putBoolean("is_logged_in", false).apply();
            Toast.makeText(DashboardActivity.this, "Signed out successfully", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // --- FAB CLICK LOGIC ---
        fabTrack.setOnClickListener(v -> Toast.makeText(DashboardActivity.this, "Track screen coming soon!", Toast.LENGTH_SHORT).show());

        // --- BOTTOM NAV CLICK LOGIC (Swaps all 4 fragments perfectly) ---
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Fragment selectedFragment = null;

            if (itemId == R.id.nav_today) {
                selectedFragment = new TodayFragment();
            } else if (itemId == R.id.nav_calendar) {
                selectedFragment = new CalendarFragment();
            } else if (itemId == R.id.nav_notifications) {
                selectedFragment = new NotificationsFragment();
            } else if (itemId == R.id.nav_settings) {
                selectedFragment = new SettingsFragment();
            }

            // Perform the actual swap
            if (selectedFragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
                return true;
            }
            return false;
        });
    }
}