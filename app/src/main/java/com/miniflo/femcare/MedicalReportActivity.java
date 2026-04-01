package com.miniflo.femcare;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MedicalReportActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "FemCareMedicalReports";
    private static final String KEY_REPORT_HISTORY = "report_history_json";

    private ImageView ivReportPreview;
    private TextView tvSelectedSummary;
    private TextView tvHistorySummary;
    private MaterialButton btnCaptureReport;
    private MaterialButton btnUploadReport;
    private MaterialButton btnSaveReport;
    private MaterialButton btnViewDetails;
    private MaterialButton btnViewHistory;

    private Uri selectedSourceUri;
    private Uri capturedImageUri;
    private String currentCapturePath;
    private String selectedFileName = "";
    private String selectedMimeType = "";
    private long selectedFileSize = 0L;
    private boolean selectedIsImage = false;
    private String selectedSource = "";

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && capturedImageUri != null) {
                    selectedSourceUri = capturedImageUri;
                    selectedSource = "captured";
                    selectedFileName = currentCapturePath != null ? new File(currentCapturePath).getName() : "captured_report.jpg";
                    selectedMimeType = "image/jpeg";
                    selectedFileSize = currentCapturePath != null ? new File(currentCapturePath).length() : 0L;
                    selectedIsImage = true;
                    bindSelectionToUi();
                    Toast.makeText(this, "Image captured successfully", Toast.LENGTH_SHORT).show();
                } else {
                    clearTempCaptureIfExists();
                }
            }
    );

    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null || result.getData().getData() == null) {
                    return;
                }

                Uri pickedUri = result.getData().getData();
                handlePickedUri(pickedUri);
            }
    );

    private final ActivityResultLauncher<String> cameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> {
                if (granted) {
                    openCamera();
                } else {
                    Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medical_report);

        ivReportPreview = findViewById(R.id.ivReportPreview);
        tvSelectedSummary = findViewById(R.id.tvSelectedSummary);
        tvHistorySummary = findViewById(R.id.tvHistorySummary);
        btnCaptureReport = findViewById(R.id.btnCaptureReport);
        btnUploadReport = findViewById(R.id.btnUploadReport);
        btnSaveReport = findViewById(R.id.btnSaveReport);
        btnViewDetails = findViewById(R.id.btnViewDetails);
        btnViewHistory = findViewById(R.id.btnViewHistory);

        btnCaptureReport.setOnClickListener(v -> ensureCameraPermissionAndCapture());
        btnUploadReport.setOnClickListener(v -> openFilePicker());
        btnSaveReport.setOnClickListener(v -> saveSelectedReport());
        btnViewDetails.setOnClickListener(v -> showDetailsDialog());
        btnViewHistory.setOnClickListener(v -> showHistoryDialog());

        loadSavedReportsHistory();
    }

    private void ensureCameraPermissionAndCapture() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera();
            return;
        }
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        File imageFile;
        try {
            imageFile = createImageFile();
        } catch (IOException e) {
            Toast.makeText(this, "Unable to prepare image file", Toast.LENGTH_SHORT).show();
            return;
        }

        capturedImageUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", imageFile);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, capturedImageUri);
        intent.setClipData(ClipData.newRawUri("medical_report", capturedImageUri));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        try {
            cameraLauncher.launch(intent);
        } catch (ActivityNotFoundException e) {
            trySecureCameraFallback();
        } catch (Exception e) {
            clearTempCaptureIfExists();
            Toast.makeText(this, "Unable to open camera", Toast.LENGTH_LONG).show();
        }
    }

    private void trySecureCameraFallback() {
        if (capturedImageUri == null) {
            clearTempCaptureIfExists();
            Toast.makeText(this, "No compatible camera app found", Toast.LENGTH_LONG).show();
            return;
        }

        Intent secureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE_SECURE);
        secureIntent.putExtra(MediaStore.EXTRA_OUTPUT, capturedImageUri);
        secureIntent.setClipData(ClipData.newRawUri("medical_report", capturedImageUri));
        secureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        try {
            cameraLauncher.launch(secureIntent);
        } catch (Exception ex) {
            clearTempCaptureIfExists();
            Toast.makeText(this, "No compatible camera app found", Toast.LENGTH_LONG).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File rootDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (rootDir == null) {
            throw new IOException("Storage unavailable");
        }

        File captureDir = new File(rootDir, "FemCareReportsTemp");
        if (!captureDir.exists() && !captureDir.mkdirs()) {
            throw new IOException("Unable to create capture directory");
        }

        File imageFile = File.createTempFile("REPORT_CAPTURE_" + timeStamp + "_", ".jpg", captureDir);
        currentCapturePath = imageFile.getAbsolutePath();
        return imageFile;
    }

    private void openFilePicker() {
        Intent pickerIntent = new Intent(Intent.ACTION_GET_CONTENT);
        pickerIntent.setType("*/*");
        pickerIntent.addCategory(Intent.CATEGORY_OPENABLE);
        pickerIntent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/jpeg", "image/png", "application/pdf"});
        pickerIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        filePickerLauncher.launch(Intent.createChooser(pickerIntent, "Select medical report"));
    }

    private void handlePickedUri(Uri uri) {
        String mimeType = resolveMimeType(uri);
        String fileName = resolveDisplayName(uri);
        long fileSize = resolveDisplaySize(uri);

        boolean isImage = mimeType.startsWith("image/");
        boolean isPdf = "application/pdf".equalsIgnoreCase(mimeType)
                || fileName.toLowerCase(Locale.getDefault()).endsWith(".pdf");

        if (!isImage && !isPdf) {
            Toast.makeText(this, "Please select a JPG, PNG, or PDF file", Toast.LENGTH_LONG).show();
            return;
        }

        selectedSourceUri = uri;
        selectedSource = "uploaded";
        selectedFileName = fileName;
        selectedMimeType = isPdf ? "application/pdf" : mimeType;
        selectedFileSize = fileSize;
        selectedIsImage = isImage;
        bindSelectionToUi();
    }

    private void bindSelectionToUi() {
        String selectedType = selectedIsImage ? "Image" : "PDF";
        tvSelectedSummary.setText("Selected: " + selectedFileName + " (" + selectedType + ")");

        if (selectedIsImage) {
            ivReportPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Glide.with(this)
                    .load(selectedSourceUri)
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_menu_report_image)
                    .error(android.R.drawable.ic_dialog_alert)
                    .into(ivReportPreview);
        } else {
            ivReportPreview.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            ivReportPreview.setImageResource(android.R.drawable.ic_menu_save);
        }

        btnSaveReport.setEnabled(selectedSourceUri != null);
        btnViewDetails.setEnabled(selectedSourceUri != null);
    }

    private void saveSelectedReport() {
        if (selectedSourceUri == null) {
            Toast.makeText(this, "Capture or upload a file first", Toast.LENGTH_SHORT).show();
            return;
        }

        setSavingState(true);

        new Thread(() -> {
            try {
                SavedReport savedReport = copySelectionToPrivateStorage();
                appendSavedMetadata(savedReport);

                runOnUiThread(() -> {
                    setSavingState(false);
                    loadSavedReportsHistory();
                    Toast.makeText(this, "Report saved successfully", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setSavingState(false);
                    Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private SavedReport copySelectionToPrivateStorage() throws Exception {
        File rootDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (rootDir == null) {
            throw new IOException("Storage unavailable");
        }

        File reportsDir = new File(rootDir, "FemCareReports");
        if (!reportsDir.exists() && !reportsDir.mkdirs()) {
            throw new IOException("Unable to create reports directory");
        }

        String extension = getExtensionForSave(selectedMimeType, selectedFileName);
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String baseName = selectedFileName == null || selectedFileName.trim().isEmpty() ? "medical_report" : selectedFileName;
        File targetFile = new File(reportsDir, "REPORT_" + timeStamp + "_" + sanitizeFileName(baseName, extension));

        try (InputStream in = getContentResolver().openInputStream(selectedSourceUri);
             FileOutputStream out = new FileOutputStream(targetFile)) {

            if (in == null) {
                throw new IOException("Unable to open selected file");
            }

            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
        }

        Uri savedUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", targetFile);

        if (currentCapturePath != null) {
            File tempCapture = new File(currentCapturePath);
            if (tempCapture.exists() && !tempCapture.getAbsolutePath().equals(targetFile.getAbsolutePath())) {
                tempCapture.delete();
            }
            currentCapturePath = null;
            capturedImageUri = null;
        }

        return new SavedReport(
                targetFile.getName(),
                selectedMimeType,
                targetFile.length(),
                targetFile.getAbsolutePath(),
                savedUri.toString(),
                selectedSource,
                System.currentTimeMillis()
        );
    }

    private void appendSavedMetadata(SavedReport report) throws Exception {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String raw = preferences.getString(KEY_REPORT_HISTORY, "[]");
        JSONArray array = new JSONArray(raw == null ? "[]" : raw);

        JSONObject item = new JSONObject();
        item.put("fileName", report.fileName);
        item.put("mimeType", report.mimeType);
        item.put("sizeBytes", report.sizeBytes);
        item.put("savedPath", report.savedPath);
        item.put("savedUri", report.savedUri);
        item.put("source", report.source);
        item.put("savedAt", report.savedAtMillis);
        array.put(item);

        preferences.edit().putString(KEY_REPORT_HISTORY, array.toString()).apply();
    }

    private void loadSavedReportsHistory() {
        try {
            JSONArray array = getSavedHistoryArray();
            if (array.length() == 0) {
                tvHistorySummary.setText("No saved reports yet");
                btnViewHistory.setEnabled(false);
                return;
            }

            SimpleDateFormat format = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());
            JSONObject latest = array.getJSONObject(array.length() - 1);
            String latestText = "Saved reports: " + array.length()
                    + " | Last saved: " + format.format(new Date(latest.optLong("savedAt")));
            tvHistorySummary.setText(latestText);
            btnViewHistory.setEnabled(true);
        } catch (Exception e) {
            tvHistorySummary.setText("Unable to read saved history");
            btnViewHistory.setEnabled(false);
        }
    }

    private JSONArray getSavedHistoryArray() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String raw = preferences.getString(KEY_REPORT_HISTORY, "[]");
        try {
            return new JSONArray(raw == null ? "[]" : raw);
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private void showDetailsDialog() {
        if (selectedSourceUri == null) {
            Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show();
            return;
        }

        String details = "File Name: " + selectedFileName
                + "\nType: " + selectedMimeType
                + "\nSize: " + formatBytes(selectedFileSize)
                + "\nSource: " + capitalizeSource(selectedSource);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Selected File Details")
                .setMessage(details)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showHistoryDialog() {
        JSONArray array;
        try {
            array = getSavedHistoryArray();
        } catch (Exception e) {
            Toast.makeText(this, "Unable to load history", Toast.LENGTH_SHORT).show();
            return;
        }

        if (array.length() == 0) {
            Toast.makeText(this, "No saved reports yet", Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat format = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
        StringBuilder historyText = new StringBuilder();

        int maxItems = Math.min(array.length(), 12);
        for (int i = array.length() - 1, count = 1; i >= 0 && count <= maxItems; i--, count++) {
            JSONObject item = array.optJSONObject(i);
            if (item == null) {
                continue;
            }

            historyText.append(count)
                    .append(") ")
                    .append(item.optString("fileName", "Unknown file"))
                    .append("\n")
                    .append("Saved: ")
                    .append(format.format(new Date(item.optLong("savedAt"))))
                    .append("\n")
                    .append("Type: ")
                    .append(item.optString("mimeType", "-"))
                    .append("\n")
                    .append("Source: ")
                    .append(capitalizeSource(item.optString("source", "uploaded")));

            if (count < maxItems) {
                historyText.append("\n\n");
            }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Saved Reports History")
                .setMessage(historyText.toString())
                .setPositiveButton("Close", null)
                .show();
    }

    private String capitalizeSource(String source) {
        if (source == null || source.trim().isEmpty()) {
            return "Uploaded";
        }

        String clean = source.trim().toLowerCase(Locale.getDefault());
        return clean.substring(0, 1).toUpperCase(Locale.getDefault()) + clean.substring(1);
    }

    private String resolveMimeType(Uri uri) {
        String mime = getContentResolver().getType(uri);
        if (mime != null && !mime.trim().isEmpty()) {
            return mime;
        }

        String name = resolveDisplayName(uri).toLowerCase(Locale.getDefault());
        if (name.endsWith(".pdf")) return "application/pdf";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".jpeg") || name.endsWith(".jpg")) return "image/jpeg";
        return "application/octet-stream";
    }

    private String resolveDisplayName(Uri uri) {
        String fallback = "medical_report";
        Cursor cursor = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        String name = cursor.getString(nameIndex);
                        if (name != null && !name.trim().isEmpty()) {
                            return name;
                        }
                    }
                }
            } finally {
                cursor.close();
            }
        }

        String pathSegment = uri.getLastPathSegment();
        return pathSegment != null && !pathSegment.trim().isEmpty() ? pathSegment : fallback;
    }

    private long resolveDisplaySize(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, new String[]{OpenableColumns.SIZE}, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (index >= 0) {
                        return cursor.getLong(index);
                    }
                }
            } finally {
                cursor.close();
            }
        }
        return 0L;
    }

    private String sanitizeFileName(String fileName, String extension) {
        String clean = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (clean.toLowerCase(Locale.getDefault()).endsWith(extension.toLowerCase(Locale.getDefault()))) {
            return clean;
        }
        return clean + extension;
    }

    private String getExtensionForSave(String mimeType, String fileName) {
        if ("application/pdf".equalsIgnoreCase(mimeType)) {
            return ".pdf";
        }
        if ("image/png".equalsIgnoreCase(mimeType)) {
            return ".png";
        }
        if ("image/jpeg".equalsIgnoreCase(mimeType)) {
            return ".jpg";
        }

        String lowerName = fileName == null ? "" : fileName.toLowerCase(Locale.getDefault());
        if (lowerName.endsWith(".pdf")) return ".pdf";
        if (lowerName.endsWith(".png")) return ".png";
        if (lowerName.endsWith(".jpeg") || lowerName.endsWith(".jpg")) return ".jpg";
        return ".bin";
    }

    private String formatBytes(long bytes) {
        if (bytes <= 0) {
            return "0 B";
        }
        if (bytes < 1024) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format(Locale.getDefault(), "%.1f KB", kb);
        }
        double mb = kb / 1024.0;
        return String.format(Locale.getDefault(), "%.2f MB", mb);
    }

    private void setSavingState(boolean saving) {
        btnSaveReport.setEnabled(!saving && selectedSourceUri != null);
        btnCaptureReport.setEnabled(!saving);
        btnUploadReport.setEnabled(!saving);
    }

    private void clearTempCaptureIfExists() {
        if (currentCapturePath == null) {
            return;
        }

        File temp = new File(currentCapturePath);
        if (temp.exists()) {
            temp.delete();
        }

        currentCapturePath = null;
        capturedImageUri = null;
    }

    private static class SavedReport {
        final String fileName;
        final String mimeType;
        final long sizeBytes;
        final String savedPath;
        final String savedUri;
        final String source;
        final long savedAtMillis;

        SavedReport(String fileName, String mimeType, long sizeBytes, String savedPath, String savedUri, String source, long savedAtMillis) {
            this.fileName = fileName;
            this.mimeType = mimeType;
            this.sizeBytes = sizeBytes;
            this.savedPath = savedPath;
            this.savedUri = savedUri;
            this.source = source;
            this.savedAtMillis = savedAtMillis;
        }
    }
}