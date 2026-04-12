package com.miniflo.femcare;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class DashboardActivity extends AppCompatActivity {

    private static final int NOTIFICATION_PERMISSION_CODE = 102;

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        requestNotificationPermissionIfNeeded();

        BackgroundTaskScheduler.scheduleAll(this);
        BackgroundTaskScheduler.enqueueImmediateSync(this, "dashboard_open");

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        bottomNav = findViewById(R.id.bottomNav);
        FloatingActionButton fabTrack = findViewById(R.id.fabTrack);

        boolean openNotifications = getIntent() != null
                && getIntent().getBooleanExtra(NotificationHelper.EXTRA_OPEN_NOTIFICATIONS, false);

        // --- LOAD DEFAULT FRAGMENT (Today) ---
        if (savedInstanceState == null) {
            Fragment initialFragment = openNotifications ? new NotificationsFragment() : new TodayFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, initialFragment)
                    .commit();

            bottomNav.setSelectedItemId(openNotifications ? R.id.nav_notifications : R.id.nav_today);
        }

        // --- FAB CLICK LOGIC ---
        fabTrack.setOnClickListener(v -> {
            getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.fade_out)
                    .replace(R.id.fragment_container, new TrackFragment())
                    // Adding to backstack means if they press the phone's back button, it goes back to the dashboard!
                    .addToBackStack(null)
                    .commit();
        });

        // --- BOTTOM NAV CLICK LOGIC (Fragment Swapper) ---
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

            // Perform the visual swap
            if (selectedFragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out) // Smooth transition
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        if (intent != null && intent.getBooleanExtra(NotificationHelper.EXTRA_OPEN_NOTIFICATIONS, false)) {
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.fragment_container, new NotificationsFragment())
                    .commit();

            if (bottomNav != null) {
                bottomNav.setSelectedItemId(R.id.nav_notifications);
            }
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            return;
        }

        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                NOTIFICATION_PERMISSION_CODE
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (!granted) {
                Toast.makeText(this, "Notifications are disabled. Enable them to receive reminders.", Toast.LENGTH_LONG).show();
            }
        }
    }
}