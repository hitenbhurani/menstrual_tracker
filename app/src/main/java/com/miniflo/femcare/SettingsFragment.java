package com.miniflo.femcare;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SettingsFragment extends Fragment {

    private TextView tvSettingsName, tvSettingsEmail;
    private TextView tvDescPeriod, tvDescOvulation, tvDescLog;
    private ImageView btnEditProfile;
    private SwitchMaterial switchPeriodAlert, switchOvulationAlert, switchLogAlert, switchDarkMode;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SharedPreferences prefs;

    // Cached user data for editing
    private String currentName = "";
    private String currentHeight = "";
    private String currentWeight = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        prefs = requireActivity().getSharedPreferences("FemCarePrefs", Context.MODE_PRIVATE);

        tvSettingsName = view.findViewById(R.id.tvSettingsName);
        tvSettingsEmail = view.findViewById(R.id.tvSettingsEmail);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);

        switchPeriodAlert = view.findViewById(R.id.switchPeriodAlert);
        switchOvulationAlert = view.findViewById(R.id.switchOvulationAlert);
        switchLogAlert = view.findViewById(R.id.switchLogAlert);
        switchDarkMode = view.findViewById(R.id.switchDarkMode);

        tvDescPeriod = view.findViewById(R.id.tvDescPeriod);
        tvDescOvulation = view.findViewById(R.id.tvDescOvulation);
        tvDescLog = view.findViewById(R.id.tvDescLog);

        // Dark Mode Logic
        boolean isDarkMode = prefs.getBoolean("pref_dark_mode", false);
        switchDarkMode.setChecked(isDarkMode);
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("pref_dark_mode", isChecked).apply();
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        loadUserProfile();
        enforceSmartNotificationLogic();
        setupClickListeners(view);

        return view;
    }

    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            tvSettingsEmail.setText(user.getEmail());

            db.collection("users").document(user.getEmail()).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            if (doc.contains("name")) {
                                currentName = doc.getString("name");
                                if (currentName != null && !currentName.isEmpty()) {
                                    String formattedName = currentName.substring(0, 1).toUpperCase() + currentName.substring(1);
                                    tvSettingsName.setText(formattedName);
                                }
                            }
                            if (doc.contains("heightCm")) currentHeight = String.valueOf(doc.getLong("heightCm"));
                            if (doc.contains("weightKg")) currentWeight = String.valueOf(doc.get("weightKg"));
                        }
                    });
        }
    }

    private void enforceSmartNotificationLogic() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("users").document(user.getEmail()).get().addOnSuccessListener(doc -> {
            if (doc.contains("lastPeriodStartMillis") && doc.contains("averageCycleLength")) {
                long lastPeriodMillis = doc.getLong("lastPeriodStartMillis");
                int cycleLength = doc.getLong("averageCycleLength").intValue();

                Calendar now = Calendar.getInstance();
                long diffMillis = now.getTimeInMillis() - lastPeriodMillis;
                int daysDiff = (int) Math.floor(diffMillis / (1000.0 * 60 * 60 * 24));
                int cycleDay = daysDiff % cycleLength;

                if (cycleDay >= cycleLength - 5) {
                    switchPeriodAlert.setEnabled(true);
                    switchPeriodAlert.setChecked(prefs.getBoolean("pref_alert_period", true));
                    tvDescPeriod.setText(switchPeriodAlert.isChecked() ? "ON: We'll alert you 2 days before." : "OFF: No heads-up reminder.");
                } else {
                    switchPeriodAlert.setChecked(false);
                    switchPeriodAlert.setEnabled(false);
                    tvDescPeriod.setText("Locked: You are not near your period yet.");
                }

                if (cycleDay >= cycleLength - 18 && cycleDay <= cycleLength - 12) {
                    switchOvulationAlert.setEnabled(true);
                    switchOvulationAlert.setChecked(prefs.getBoolean("pref_alert_ovulation", false));
                    tvDescOvulation.setText(switchOvulationAlert.isChecked() ? "ON: We'll remind you on Ovulation day." : "OFF: No ovulation reminder.");
                } else {
                    switchOvulationAlert.setChecked(false);
                    switchOvulationAlert.setEnabled(false);
                    tvDescOvulation.setText("Locked: Available only during Fertile window.");
                }

                switchPeriodAlert.setOnCheckedChangeListener((btn, isChecked) -> {
                    prefs.edit().putBoolean("pref_alert_period", isChecked).apply();
                    tvDescPeriod.setText(isChecked ? "ON: We'll alert you 2 days before." : "OFF: No heads-up reminder.");
                });

                switchOvulationAlert.setOnCheckedChangeListener((btn, isChecked) -> {
                    prefs.edit().putBoolean("pref_alert_ovulation", isChecked).apply();
                    tvDescOvulation.setText(isChecked ? "ON: We'll remind you on Ovulation day." : "OFF: No ovulation reminder.");
                });

                switchLogAlert.setOnCheckedChangeListener((btn, isChecked) -> {
                    prefs.edit().putBoolean("pref_alert_log", isChecked).apply();
                    tvDescLog.setText(isChecked ? "ON: Reminding you daily at 8 PM." : "OFF: No daily logging reminders.");
                });
            }
        });
    }

    private void setupClickListeners(View view) {
        btnEditProfile.setOnClickListener(v -> openEditProfileSheet());
        view.findViewById(R.id.btnExportData).setOnClickListener(v -> generateAndShareDetailedReport());

        view.findViewById(R.id.btnPrivacy).setOnClickListener(v ->
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Privacy & Security")
                        .setMessage("Your health data is encrypted and securely stored. You have full control over your data.")
                        .setPositiveButton("Got it", null)
                        .show()
        );

        view.findViewById(R.id.btnSignOut).setOnClickListener(v -> performSignOut());
    }

    private void generateAndShareDetailedReport() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        Toast.makeText(getContext(), "Fetching logs and generating report...", Toast.LENGTH_SHORT).show();

        db.collection("users").document(user.getEmail()).get().addOnSuccessListener(userDoc -> {
            if (userDoc.exists()) {
                db.collection("users").document(user.getEmail()).collection("daily_logs")
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .limit(30) // Last 30 days of logs
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            createDetailedPdfReport(userDoc, queryDocumentSnapshots);
                        });
            }
        });
    }

    private void createDetailedPdfReport(DocumentSnapshot userDoc, com.google.firebase.firestore.QuerySnapshot logs) {
        PdfDocument pdfDocument = new PdfDocument();
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        // Constants for layout
        final int PAGE_WIDTH = 595;
        final int PAGE_HEIGHT = 842;
        final int MARGIN = 40;
        final int COLUMN_2_X = 300;
        final int TABLE_X_DATE = 40;
        final int TABLE_X_CYCLE_DAY = 140;
        final int TABLE_X_SYMPTOMS = 200;
        final int TABLE_X_MOOD = 380;
        final int TABLE_X_WATER = 510;

        // 1. Calculate Trends Summarizer
        Map<String, Integer> symptomCounts = new HashMap<>();
        int totalWater = 0;
        int logCount = 0;
        for (QueryDocumentSnapshot log : logs) {
            List<String> traits = (List<String>) log.get("loggedTraits");
            if (traits != null) {
                for (String trait : traits) {
                    symptomCounts.put(trait, symptomCounts.getOrDefault(trait, 0) + 1);
                }
            }
            Long water = log.getLong("waterGlasses");
            if (water != null) totalWater += water;
            logCount++;
        }
        double avgWater = logCount > 0 ? (double) totalWater / logCount : 0;
        List<Map.Entry<String, Integer>> sortedSymptoms = new ArrayList<>(symptomCounts.entrySet());
        sortedSymptoms.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
        StringBuilder topSymptomsStr = new StringBuilder();
        for (int i = 0; i < Math.min(3, sortedSymptoms.size()); i++) {
            if (i > 0) topSymptomsStr.append(", ");
            topSymptomsStr.append(sortedSymptoms.get(i).getKey()).append(" (").append(sortedSymptoms.get(i).getValue()).append(")");
        }

        // Setup Page 1
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        int currentY = 60;

        // Background Watermark Setup (Alpha: 60)
        drawWatermark(canvas, paint, PAGE_WIDTH, PAGE_HEIGHT, 60);

        // Part 1: Header
        paint.setColor(Color.parseColor("#C2185B"));
        paint.setTextSize(18f);
        paint.setFakeBoldText(true);
        canvas.drawText("FemCare Health & Cycle Report", MARGIN, currentY, paint);
        currentY += 10;
        paint.setStrokeWidth(2f);
        canvas.drawLine(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY, paint);
        currentY += 30;

        // Part 2: Personal Medical Profile
        paint.setColor(Color.BLACK);
        paint.setTextSize(12f);
        paint.setFakeBoldText(true);
        canvas.drawText("Personal Medical Profile", MARGIN, currentY, paint);
        canvas.drawText("Cycle Intelligence Statistics", COLUMN_2_X, currentY, paint);
        currentY += 20;

        paint.setFakeBoldText(false);
        String name = userDoc.getString("name");
        String email = userDoc.getId();
        Long height = userDoc.getLong("heightCm");
        Object weightObj = userDoc.get("weightKg");
        Double bmi = userDoc.getDouble("bmi");
        Boolean birthControl = userDoc.getBoolean("onBirthControl");
        Boolean isRegular = userDoc.getBoolean("isRegular");
        Long stressLevel = userDoc.getLong("stressLevel");

        canvas.drawText("Full Name: " + (name != null ? name : "N/A"), MARGIN, currentY, paint);
        canvas.drawText("Avg Cycle Length: " + userDoc.getLong("averageCycleLength") + " days", COLUMN_2_X, currentY, paint);
        currentY += 15;
        canvas.drawText("Account Email: " + email, MARGIN, currentY, paint);
        canvas.drawText("Avg Period Duration: " + userDoc.getLong("periodDuration") + " days", COLUMN_2_X, currentY, paint);
        currentY += 15;
        canvas.drawText("Height: " + (height != null ? height + " cm" : "N/A"), MARGIN, currentY, paint);
        canvas.drawText("Cycle Regularity: " + (isRegular != null ? (isRegular ? "Regular" : "Irregular") : "N/A"), COLUMN_2_X, currentY, paint);
        currentY += 15;
        canvas.drawText("Current Weight: " + (weightObj != null ? weightObj + " kg" : "N/A"), MARGIN, currentY, paint);
        canvas.drawText("On Birth Control: " + (birthControl != null ? (birthControl ? "Yes" : "No") : "N/A"), COLUMN_2_X, currentY, paint);
        currentY += 15;
        canvas.drawText("Calculated BMI: " + (bmi != null ? String.format(Locale.getDefault(), "%.2f", bmi) : "N/A"), MARGIN, currentY, paint);
        canvas.drawText("Reported Stress: " + (stressLevel != null ? stressLevel + "/5" : "N/A"), COLUMN_2_X, currentY, paint);
        currentY += 40;

        // Part 3: Clinical Observation Summary (Structured Format)
        paint.setFakeBoldText(true);
        paint.setTextSize(13f);
        paint.setColor(Color.parseColor("#C2185B"));
        canvas.drawText("CLINICAL OBSERVATION SUMMARY", MARGIN, currentY, paint);
        currentY += 20;

        paint.setTextSize(11f);
        paint.setColor(Color.BLACK);
        paint.setFakeBoldText(true);
        canvas.drawText("Symptom Prevalence:", MARGIN, currentY, paint);
        paint.setFakeBoldText(false);
        canvas.drawText(topSymptomsStr.length() > 0 ? topSymptomsStr.toString() : "None identified", MARGIN + 130, currentY, paint);
        currentY += 18;

        paint.setFakeBoldText(true);
        canvas.drawText("Hydration Index:", MARGIN, currentY, paint);
        paint.setFakeBoldText(false);
        canvas.drawText(String.format(Locale.getDefault(), "%.1f glasses / day (Average)", avgWater), MARGIN + 130, currentY, paint);
        currentY += 18;

        paint.setFakeBoldText(true);
        canvas.drawText("Data Consistency:", MARGIN, currentY, paint);
        paint.setFakeBoldText(false);
        canvas.drawText(logCount + " entries recorded in last 30-day window", MARGIN + 130, currentY, paint);
        currentY += 18;

        paint.setFakeBoldText(true);
        canvas.drawText("Overall Assessment:", MARGIN, currentY, paint);
        paint.setFakeBoldText(false);
        String assessment = (logCount > 15) ? "High consistency - reliable cycle data" : "Moderate consistency - monitoring recommended";
        canvas.drawText(assessment, MARGIN + 130, currentY, paint);
        currentY += 45;

        // Part 4: Daily Tracking Log Table
        paint.setFakeBoldText(true);
        paint.setTextSize(12f);
        paint.setColor(Color.BLACK);
        canvas.drawText("DAILY TRACKING LOG", MARGIN, currentY, paint);
        currentY += 15;

        drawTableHeader(canvas, paint, currentY, PAGE_WIDTH, MARGIN, TABLE_X_DATE, TABLE_X_CYCLE_DAY, TABLE_X_SYMPTOMS, TABLE_X_MOOD, TABLE_X_WATER);
        currentY += 25;

        long lastPeriodMillis = userDoc.contains("lastPeriodStartMillis") ? userDoc.getLong("lastPeriodStartMillis") : 0;
        int cycleLengthVal = userDoc.contains("averageCycleLength") ? userDoc.getLong("averageCycleLength").intValue() : 28;

        paint.setTextSize(10f);
        for (QueryDocumentSnapshot log : logs) {
            if (currentY > 780) {
                pdfDocument.finishPage(page);
                pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pdfDocument.getPages().size() + 1).create();
                page = pdfDocument.startPage(pageInfo);
                canvas = page.getCanvas();
                drawWatermark(canvas, paint, PAGE_WIDTH, PAGE_HEIGHT, 60);
                currentY = 60;
                drawTableHeader(canvas, paint, currentY, PAGE_WIDTH, MARGIN, TABLE_X_DATE, TABLE_X_CYCLE_DAY, TABLE_X_SYMPTOMS, TABLE_X_MOOD, TABLE_X_WATER);
                currentY += 25;
                paint.setTextSize(10f);
            }

            String date = log.getId();
            String cycleDayStr = "N/A";
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Calendar logCal = Calendar.getInstance();
                logCal.setTime(sdf.parse(date));
                if (lastPeriodMillis > 0) {
                    long diff = logCal.getTimeInMillis() - lastPeriodMillis;
                    int daysDiff = (int) (diff / (1000 * 60 * 60 * 24));
                    int cd = (daysDiff % cycleLengthVal);
                    if (cd < 0) cd += cycleLengthVal;
                    cycleDayStr = String.valueOf(cd + 1);
                }
            } catch (Exception e) {}

            List<String> traits = (List<String>) log.get("loggedTraits");
            String symptoms = traits != null ? String.join(", ", traits) : "";
            if (symptoms.length() > 30) symptoms = symptoms.substring(0, 27) + "...";

            String mood = log.getString("mood");
            String discharge = log.getString("discharge");
            String moodDischarge = (mood != null ? mood : "") + (mood != null && discharge != null ? "/" : "") + (discharge != null ? discharge : "");
            if (moodDischarge.length() > 20) moodDischarge = moodDischarge.substring(0, 17) + "...";

            Long water = log.getLong("waterGlasses");

            canvas.drawText(date, TABLE_X_DATE, currentY, paint);
            canvas.drawText(cycleDayStr, TABLE_X_CYCLE_DAY, currentY, paint);
            canvas.drawText(symptoms, TABLE_X_SYMPTOMS, currentY, paint);
            canvas.drawText(moodDischarge, TABLE_X_MOOD, currentY, paint);
            canvas.drawText(String.valueOf(water != null ? water : 0), TABLE_X_WATER, currentY, paint);
            
            currentY += 20;
        }

        // Part 5: Medical Footer
        paint.setTextSize(8f);
        paint.setColor(Color.GRAY);
        paint.setFakeBoldText(false);
        paint.setTextSkewX(-0.25f);
        canvas.drawText("Confidential Medical Data. Automatically generated via FemCare. Not a clinical diagnosis.", MARGIN, 820, paint);
        paint.setTextSkewX(0);

        pdfDocument.finishPage(page);

        File file = new File(requireContext().getExternalFilesDir(null), "FemCare_Medical_Report.pdf");
        try {
            pdfDocument.writeTo(new FileOutputStream(file));
            pdfDocument.close();
            shareFile(file);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error generating medical PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private void drawWatermark(Canvas canvas, Paint paint, int width, int height, int alpha) {
        Bitmap logo = BitmapFactory.decodeResource(getResources(), R.drawable.logo
        );
        if (logo != null) {
            Bitmap scaledLogo = Bitmap.createScaledBitmap(logo, 300, 300, true);
            paint.setAlpha(alpha);
            canvas.drawBitmap(scaledLogo, (width - 300) / 2f, (height - 300) / 2f, paint);
            paint.setAlpha(255);
        }
    }

    private void drawTableHeader(Canvas canvas, Paint paint, int y, int width, int margin, int x1, int x2, int x3, int x4, int x5) {
        paint.setColor(Color.LTGRAY);
        canvas.drawRect(margin, y - 15, width - margin, y + 10, paint);
        paint.setColor(Color.BLACK);
        paint.setFakeBoldText(true);
        paint.setTextSize(10f);
        canvas.drawText("Date", x1, y, paint);
        canvas.drawText("Cycle Day", x2, y, paint);
        canvas.drawText("Symptoms", x3, y, paint);
        canvas.drawText("Mood/Discharge", x4, y, paint);
        canvas.drawText("Water", x5, y, paint);
        paint.setFakeBoldText(false);
    }

    private void shareFile(File file) {
        Uri uri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Share Detailed FemCare Report"));
    }

    private void openEditProfileSheet() {
        BottomSheetDialog editSheet = new BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_edit_profile, null);
        editSheet.setContentView(sheetView);

        TextInputEditText etEditName = sheetView.findViewById(R.id.etEditName);
        TextInputEditText etEditHeight = sheetView.findViewById(R.id.etEditHeight);
        TextInputEditText etEditWeight = sheetView.findViewById(R.id.etEditWeight);
        MaterialButton btnSaveProfile = sheetView.findViewById(R.id.btnSaveProfile);

        etEditName.setText(currentName);
        etEditHeight.setText(currentHeight);
        etEditWeight.setText(currentWeight);

        btnSaveProfile.setOnClickListener(v -> {
            String newName = etEditName.getText().toString().trim();
            String newHeight = etEditHeight.getText().toString().trim();
            String newWeight = etEditWeight.getText().toString().trim();

            if (newName.isEmpty()) {
                etEditName.setError("Name cannot be empty");
                return;
            }

            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("name", newName);
                if (!newHeight.isEmpty()) updates.put("heightCm", Long.parseLong(newHeight));
                if (!newWeight.isEmpty()) updates.put("weightKg", Double.parseDouble(newWeight));

                if (!newHeight.isEmpty() && !newWeight.isEmpty()) {
                    double heightMeters = Double.parseDouble(newHeight) / 100.0;
                    double weightVal = Double.parseDouble(newWeight);
                    double bmi = weightVal / (heightMeters * heightMeters);
                    updates.put("bmi", bmi);
                }

                db.collection("users").document(user.getEmail())
                        .set(updates, SetOptions.merge())
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "Profile Updated!", Toast.LENGTH_SHORT).show();
                            loadUserProfile();
                            editSheet.dismiss();
                        });
            }
        });

        editSheet.show();
    }

    private void performSignOut() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out of FemCare?")
                .setPositiveButton("Sign Out", (dialog, which) -> {
                    prefs.edit().clear().apply();
                    mAuth.signOut();
                    Toast.makeText(getContext(), "Signed out successfully", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
