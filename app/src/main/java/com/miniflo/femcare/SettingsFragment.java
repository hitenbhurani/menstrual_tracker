package com.miniflo.femcare;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
    private SwitchMaterial switchPeriodAlert, switchOvulationAlert, switchLogAlert;

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

        tvDescPeriod = view.findViewById(R.id.tvDescPeriod);
        tvDescOvulation = view.findViewById(R.id.tvDescOvulation);
        tvDescLog = view.findViewById(R.id.tvDescLog);

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
        // A4 Size is 595 x 842
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        int y = 40;
        int x = 40;

        // Title
        paint.setTextSize(18f);
        paint.setFakeBoldText(true);
        canvas.drawText("FemCare: Detailed Health & Cycle Report", x, y, paint);
        y += 40;

        // User Info Section
        paint.setTextSize(12f);
        paint.setFakeBoldText(true);
        canvas.drawText("User Information", x, y, paint);
        y += 20;
        paint.setFakeBoldText(false);
        canvas.drawText("Name: " + userDoc.getString("name"), x, y, paint);
        y += 15;
        canvas.drawText("Email: " + userDoc.getId(), x, y, paint);
        y += 15;
        canvas.drawText("Average Cycle Length: " + userDoc.getLong("averageCycleLength") + " days", x, y, paint);
        y += 15;
        canvas.drawText("Typical Period Duration: " + userDoc.getLong("periodDuration") + " days", x, y, paint);
        y += 40;

        // Logs Section
        paint.setFakeBoldText(true);
        canvas.drawText("Daily Tracking History (Last 30 Days)", x, y, paint);
        y += 20;
        paint.setFakeBoldText(false);
        paint.setTextSize(10f);

        if (logs.isEmpty()) {
            canvas.drawText("No daily logs found for the past 30 days.", x, y, paint);
        } else {
            for (QueryDocumentSnapshot logDoc : logs) {
                if (y > 780) { // Check for page end
                    pdfDocument.finishPage(page);
                    pageInfo = new PdfDocument.PageInfo.Builder(595, 842, pdfDocument.getPages().size() + 1).create();
                    page = pdfDocument.startPage(pageInfo);
                    canvas = page.getCanvas();
                    y = 40;
                }

                String date = logDoc.getId(); // Format: yyyy-MM-dd
                List<String> traits = (List<String>) logDoc.get("loggedTraits");
                Long water = logDoc.getLong("waterGlasses");

                String logEntry = "• " + date + ": " + (traits != null ? String.join(", ", traits) : "No traits") + 
                                  " | Water: " + (water != null ? water : 0) + " glasses";
                
                canvas.drawText(logEntry, x, y, paint);
                y += 15;
            }
        }

        y += 40;
        paint.setTextSize(8f);
        paint.setColor(Color.GRAY);
        canvas.drawText("Confidential: This report contains sensitive health data for medical analysis only.", x, y, paint);

        pdfDocument.finishPage(page);

        File file = new File(requireContext().getExternalFilesDir(null), "FemCare_Detailed_Report.pdf");
        try {
            pdfDocument.writeTo(new FileOutputStream(file));
            pdfDocument.close();
            shareFile(file);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error generating Detailed PDF", Toast.LENGTH_SHORT).show();
        }
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
