package com.miniflo.femcare;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TrackFragment extends Fragment {

    private static final String TAG = "TrackFragment";

    private TextView tvTrackDateHeader, tvWaterCount, tvHistMonth, tvDailyInsightTrack;
    private LinearLayout weekStripLayoutTrack, periodEndsContainer;
    private CheckBox cbPeriodEnds;
    private RecyclerView historyCalendarGrid;
    private LineChart symptomTrendChart;
    private View cardFindDoctor;

    private List<TextView> allChips = new ArrayList<>();
    private List<String> selectedLogs = new ArrayList<>();
    private Map<String, List<String>> loggedHistoryCache = new HashMap<>();

    private Calendar currentlySelectedDate;
    private Calendar currentHistoryMonth;
    private long userLastPeriodMillis = 0;
    private int userPeriodDuration = 5;
    private int userCycleLength = 28;
    private int waterGlasses = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_track, container, false);

        tvTrackDateHeader = view.findViewById(R.id.tvTrackDateHeader);
        tvDailyInsightTrack = view.findViewById(R.id.tvDailyInsightTrack);
        weekStripLayoutTrack = view.findViewById(R.id.weekStripLayoutTrack);
        periodEndsContainer = view.findViewById(R.id.periodEndsContainer);
        cbPeriodEnds = view.findViewById(R.id.cbPeriodEnds);
        historyCalendarGrid = view.findViewById(R.id.historyCalendarGrid);
        symptomTrendChart = view.findViewById(R.id.symptomTrendChart);
        tvWaterCount = view.findViewById(R.id.tvWaterCount);
        tvHistMonth = view.findViewById(R.id.tvHistMonth);
        cardFindDoctor = view.findViewById(R.id.cardFindDoctor);

        if (cardFindDoctor != null) {
            cardFindDoctor.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(requireContext(), FindDoctorActivity.class));
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Unable to open Nearby Help right now", Toast.LENGTH_LONG).show();
                }
            });
        }

        currentlySelectedDate = Calendar.getInstance();
        currentHistoryMonth = Calendar.getInstance();

        setupHeaderAndStrip();
        setupToggleChips(view);

        // Setup the new dynamic "Other" buttons
        setupCustomChip(view.findViewById(R.id.sympOther), "Symptom");
        setupCustomChip(view.findViewById(R.id.moodOther), "Mood");
        setupCustomChip(view.findViewById(R.id.disOther), "Discharge");
        setupCustomChip(view.findViewById(R.id.enOther), "Energy");

        setupWaterTracker(view);
        setupHistoryControls(view);

        fetchPeriodLogicAndHistory();
        fetchSymptomTrends();

        View saveButton = view.findViewById(R.id.btnSaveDailyLog);
        saveButton.setOnClickListener(v -> saveDailyLogToDatabase(v));

        setupSwipeGestures();
        return view;
    }

    // --- NEW LOGIC: Dynamic Custom Chips ---
    private void setupCustomChip(TextView customChip, String categoryPrefix) {
        if (customChip == null) return;

        customChip.setOnClickListener(v -> {
            // If it's already selected, unselect it and reset text
            if (customChip.getBackground().getConstantState().equals(getResources().getDrawable(R.drawable.bg_symptom_selected).getConstantState())) {
                customChip.setBackgroundResource(R.drawable.bg_symptom_unselected);
                customChip.setTextColor(Color.parseColor("#C2185B"));

                // Remove the old custom entry from logs
                selectedLogs.removeIf(log -> log.startsWith(categoryPrefix + ": "));
                customChip.setText("+ Other");
                return;
            }

            // Otherwise, open dialog to let them type their custom value
            EditText input = new EditText(getContext());
            input.setHint("Type your custom " + categoryPrefix.toLowerCase());

            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Add Custom " + categoryPrefix)
                    .setView(input)
                    .setPositiveButton("Add", (dialog, which) -> {
                        String customText = input.getText().toString().trim();
                        if (!customText.isEmpty()) {
                            // Save to backend list
                            String formattedEntry = categoryPrefix + ": " + customText;
                            selectedLogs.add(formattedEntry);

                            // Visually update the chip
                            customChip.setText(customText);
                            customChip.setBackgroundResource(R.drawable.bg_symptom_selected);
                            customChip.setTextColor(Color.BLACK);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void fetchSymptomTrends() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance().collection("users").document(user.getEmail())
                .collection("daily_logs").orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(7).get().addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Entry> entries = new ArrayList<>();
                    List<String> labels = new ArrayList<>();
                    int i = 0;

                    List<DocumentSnapshot> docs = queryDocumentSnapshots.getDocuments();
                    for (int j = docs.size() - 1; j >= 0; j--) {
                        DocumentSnapshot doc = docs.get(j);
                        List<String> traits = (List<String>) doc.get("loggedTraits");
                        int symptomCount = (traits != null) ? traits.size() : 0;

                        entries.add(new Entry(i, symptomCount));
                        labels.add(doc.getId().substring(5)); // Show MM-DD
                        i++;
                    }
                    setupAestheticGraph(entries, labels);
                });
    }

    // --- NEW LOGIC: Highly Aesthetic Gradient Line Chart ---
    private void setupAestheticGraph(List<Entry> entries, List<String> labels) {
        if (entries.isEmpty()) {
            symptomTrendChart.setNoDataText("Log symptoms daily to reveal your intensity trends.");
            return;
        }

        LineDataSet dataSet = new LineDataSet(entries, "Intensity");

        // Line styling
        dataSet.setColor(Color.parseColor("#E91E63"));
        dataSet.setLineWidth(4f);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // Smooth, swooping curve

        // Circle styling
        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(Color.parseColor("#C2185B"));
        dataSet.setCircleRadius(6f);
        dataSet.setDrawCircleHole(true);
        dataSet.setCircleHoleColor(Color.WHITE);
        dataSet.setDrawValues(false);

        // Beautiful Gradient Fill beneath the curve
        dataSet.setDrawFilled(true);
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.parseColor("#66F06292"), Color.parseColor("#00F06292")} // Soft pink fading to clear
        );
        dataSet.setFillDrawable(gradient);

        LineData lineData = new LineData(dataSet);
        symptomTrendChart.setData(lineData);

        symptomTrendChart.getDescription().setEnabled(false);
        symptomTrendChart.getLegend().setEnabled(false);
        symptomTrendChart.setTouchEnabled(true);

        // Clean X Axis
        XAxis xAxis = symptomTrendChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setAxisLineColor(Color.parseColor("#E0E0E0"));
        xAxis.setTextColor(Color.parseColor("#757575"));
        xAxis.setGranularity(1f);

        // Clean Y Axes (Remove visual clutter)
        symptomTrendChart.getAxisLeft().setDrawGridLines(true);
        symptomTrendChart.getAxisLeft().setGridColor(Color.parseColor("#F5F5F5"));
        symptomTrendChart.getAxisLeft().setAxisLineColor(Color.TRANSPARENT);
        symptomTrendChart.getAxisLeft().setTextColor(Color.parseColor("#9E9E9E"));
        symptomTrendChart.getAxisLeft().setAxisMinimum(0f);

        symptomTrendChart.getAxisRight().setEnabled(false);

        symptomTrendChart.animateX(800);
        symptomTrendChart.invalidate();
    }

    private void setupSwipeGestures() {
        GestureDetector gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > Math.abs(e2.getY() - e1.getY())) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) changeHistoryMonth(-1);
                        else changeHistoryMonth(1);
                        return true;
                    }
                }
                return false;
            }
        });

        historyCalendarGrid.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                gestureDetector.onTouchEvent(e);
                return false;
            }
        });
    }

    private void changeHistoryMonth(int amount) {
        currentHistoryMonth.add(Calendar.MONTH, amount);
        buildHistoryCalendar();
    }

    private void setupWaterTracker(View view) {
        view.findViewById(R.id.btnWaterMinus).setOnClickListener(v -> {
            if (waterGlasses > 0) {
                waterGlasses--;
                tvWaterCount.setText(String.valueOf(waterGlasses));
            }
        });
        view.findViewById(R.id.btnWaterPlus).setOnClickListener(v -> {
            if (waterGlasses < 20) {
                waterGlasses++;
                tvWaterCount.setText(String.valueOf(waterGlasses));
            }
        });
    }

    private void setupHistoryControls(View view) {
        view.findViewById(R.id.btnHistPrev).setOnClickListener(v -> changeHistoryMonth(-1));
        view.findViewById(R.id.btnHistNext).setOnClickListener(v -> changeHistoryMonth(1));
    }

    private void fetchPeriodLogicAndHistory() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(user.getEmail()).get().addOnSuccessListener(doc -> {
            Long lastPeriod = doc.getLong("lastPeriodStartMillis");
            Long periodDuration = doc.getLong("periodDuration");
            Long cycleLength = doc.getLong("averageCycleLength");

            if (lastPeriod != null) {
                userLastPeriodMillis = lastPeriod;
                userPeriodDuration = periodDuration != null ? periodDuration.intValue() : 5;
                userCycleLength = cycleLength != null ? cycleLength.intValue() : 28;
                enforcePeriodCheckboxLogic();
            }
        });

        db.collection("users").document(user.getEmail()).collection("daily_logs").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    loggedHistoryCache.clear();
                    for (DocumentSnapshot logDoc : queryDocumentSnapshots) {
                        List<String> traits = (List<String>) logDoc.get("loggedTraits");
                        Long water = logDoc.getLong("waterGlasses");
                        List<String> combinedTraits = new ArrayList<>();
                        if (traits != null) combinedTraits.addAll(traits);
                        if (water != null) combinedTraits.add("💧 Water: " + water + " glasses");

                        if (!combinedTraits.isEmpty()) {
                            loggedHistoryCache.put(logDoc.getId(), combinedTraits);
                        }
                    }
                    buildHistoryCalendar();
                });
    }

    private void enforcePeriodCheckboxLogic() {
        if (userLastPeriodMillis == 0) return;
        Calendar now = Calendar.getInstance();
        now.set(Calendar.HOUR_OF_DAY, 0);

        long cycleMillis = userCycleLength * 24L * 60L * 60L * 1000L;
        long activeStart = userLastPeriodMillis;
        long nextP = activeStart + cycleMillis;
        while (nextP < now.getTimeInMillis()) {
            activeStart += cycleMillis;
            nextP = activeStart + cycleMillis;
        }

        long diffMillis = now.getTimeInMillis() - activeStart;
        int daysDiff = (int) Math.floor(diffMillis / (1000.0 * 60 * 60 * 24));

        boolean isCurrentlyBleeding = (daysDiff >= 0 && daysDiff < userPeriodDuration);

        if (isCurrentlyBleeding) {
            tvDailyInsightTrack.setText("Menstrual Phase: Be gentle with yourself today. Hydration helps with cramps!");
        } else if (daysDiff >= userCycleLength - 19 && daysDiff <= userCycleLength - 13) {
            tvDailyInsightTrack.setText("Fertile Window: Energy is peaking! Great day for a workout or socializing.");
        } else {
            tvDailyInsightTrack.setText("Luteal Phase: You might feel a bit slower today. Rest up if you need to!");
        }

        if (!isCurrentlyBleeding) {
            cbPeriodEnds.setChecked(false);
            periodEndsContainer.setOnClickListener(v ->
                    Toast.makeText(getContext(), "You have to actually start a period before you can end it! XD", Toast.LENGTH_LONG).show()
            );
        } else {
            periodEndsContainer.setOnClickListener(v -> cbPeriodEnds.setChecked(!cbPeriodEnds.isChecked()));
        }
    }

    private void buildHistoryCalendar() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvHistMonth.setText(sdf.format(currentHistoryMonth.getTime()));

        historyCalendarGrid.setLayoutManager(new GridLayoutManager(getContext(), 7));
        LogAdapter adapter = new LogAdapter();

        List<Calendar> daysInMonth = new ArrayList<>();
        Calendar monthCal = (Calendar) currentHistoryMonth.clone();
        monthCal.set(Calendar.DAY_OF_MONTH, 1);

        int firstDayOfWeek = monthCal.get(Calendar.DAY_OF_WEEK) - 1;
        monthCal.add(Calendar.DAY_OF_MONTH, -firstDayOfWeek);

        for (int i = 0; i < 42; i++) {
            daysInMonth.add((Calendar) monthCal.clone());
            monthCal.add(Calendar.DAY_OF_MONTH, 1);
        }

        adapter.setDays(daysInMonth);
        historyCalendarGrid.setAdapter(adapter);
    }

    private class LogAdapter extends RecyclerView.Adapter<LogAdapter.LogViewHolder> {
        private List<Calendar> days = new ArrayList<>();
        SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat displayFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

        public void setDays(List<Calendar> days) { this.days = days; notifyDataSetChanged(); }

        @NonNull
        @Override
        public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
            return new LogViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
            Calendar cellDate = days.get(position);
            holder.cellDayText.setText(String.valueOf(cellDate.get(Calendar.DAY_OF_MONTH)));

            if (cellDate.get(Calendar.MONTH) != currentHistoryMonth.get(Calendar.MONTH)) {
                holder.cellDayText.setTextColor(Color.parseColor("#757575"));
                holder.cellDayText.setBackground(null);
                holder.itemView.setOnLongClickListener(null);
                return;
            }

            String dateKey = dbFormat.format(cellDate.getTime());

            if (loggedHistoryCache.containsKey(dateKey)) {
                GradientDrawable shape = new GradientDrawable();
                shape.setShape(GradientDrawable.OVAL);
                shape.setColor(Color.parseColor("#F8BBD0"));
                shape.setSize(40, 40);
                holder.cellDayText.setBackground(shape);
                holder.cellDayText.setTextColor(Color.parseColor("#C2185B"));

                holder.itemView.setOnLongClickListener(v -> {
                    List<String> traits = loggedHistoryCache.get(dateKey);
                    String msg = (traits != null) ? String.join("\n• ", traits) : "No details.";
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Log from " + displayFormat.format(cellDate.getTime()))
                            .setMessage("• " + msg)
                            .setPositiveButton("Close", null)
                            .show();
                    return true;
                });
            } else {
                holder.cellDayText.setBackground(null);
                holder.cellDayText.setTextColor(Color.BLACK);
                holder.itemView.setOnLongClickListener(null);
            }
        }
        @Override
        public int getItemCount() { return days.size(); }

        class LogViewHolder extends RecyclerView.ViewHolder {
            TextView cellDayText;
            public LogViewHolder(@NonNull View itemView) { super(itemView); cellDayText = itemView.findViewById(R.id.cellDayText); }
        }
    }

    private void setupHeaderAndStrip() {
        SimpleDateFormat headerFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
        tvTrackDateHeader.setText("Tracking for " + headerFormat.format(currentlySelectedDate.getTime()));

        Calendar calendar = (Calendar) currentlySelectedDate.clone();
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
        SimpleDateFormat dayLetterFormat = new SimpleDateFormat("E", Locale.getDefault());

        weekStripLayoutTrack.removeAllViews();
        for (int i = 0; i < 7; i++) {
            LinearLayout dayCol = new LinearLayout(getContext());
            dayCol.setOrientation(LinearLayout.VERTICAL);
            dayCol.setGravity(Gravity.CENTER);
            dayCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

            TextView letterText = new TextView(getContext());
            letterText.setText(dayLetterFormat.format(calendar.getTime()).substring(0, 1));
            letterText.setTextColor(Color.parseColor("#BDBDBD"));
            letterText.setGravity(Gravity.CENTER);

            TextView numberText = new TextView(getContext());
            numberText.setText(String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)));
            numberText.setTextSize(18f);
            numberText.setGravity(Gravity.CENTER);

            if (calendar.get(Calendar.DAY_OF_YEAR) == currentlySelectedDate.get(Calendar.DAY_OF_YEAR)) {
                numberText.setTextColor(Color.WHITE);
                numberText.setBackgroundResource(R.drawable.circle_background_pink);
            } else {
                numberText.setTextColor(Color.BLACK);
            }

            dayCol.addView(letterText);
            dayCol.addView(numberText);
            weekStripLayoutTrack.addView(dayCol);
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private void setupToggleChips(View view) {
        int[] chipIds = {
                R.id.sympCramps, R.id.sympBackache, R.id.sympBloating, R.id.sympFatigue, R.id.sympHeadache, R.id.sympTender, R.id.sympAcne, R.id.sympSpotting, R.id.sympNausea,
                R.id.moodCalm, R.id.moodHappy, R.id.moodSad, R.id.moodAnxious, R.id.moodSwings, R.id.moodIrritable, R.id.moodApathetic, R.id.moodSensitive,
                R.id.disDry, R.id.disSticky, R.id.disCreamy, R.id.disEggwhite, R.id.disWatery,
                R.id.enHigh, R.id.enNormal, R.id.enExhausted
        };

        for (int id : chipIds) {
            TextView chip = view.findViewById(id);
            if (chip == null) continue;
            allChips.add(chip);
            chip.setOnClickListener(v -> {
                String trait = chip.getText().toString();
                if (selectedLogs.contains(trait)) {
                    selectedLogs.remove(trait);
                    chip.setBackgroundResource(R.drawable.bg_symptom_unselected);
                } else {
                    selectedLogs.add(trait);
                    chip.setBackgroundResource(R.drawable.bg_symptom_selected);
                }
            });
        }
    }

    private void saveDailyLogToDatabase(@NonNull View triggerView) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            if (isAdded() && getContext() != null) {
                Toast.makeText(getContext(), "Please sign in again to save logs.", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        String email = user.getEmail().trim();

        String dateKey = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentlySelectedDate.getTime());
        String todayKey = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime());

        Map<String, Object> logData = new HashMap<>();
        logData.put("loggedTraits", new ArrayList<>(selectedLogs));
        logData.put("waterGlasses", waterGlasses);
        logData.put("periodEndsToday", cbPeriodEnds.isChecked());
        logData.put("timestamp", System.currentTimeMillis());

        triggerView.setEnabled(false);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        com.google.firebase.firestore.DocumentReference logDocRef = db.collection("users")
                .document(email)
                .collection("daily_logs")
                .document(dateKey);

        boolean wasExisting = loggedHistoryCache.containsKey(dateKey);
        persistDailyLog(logDocRef, logData, dateKey, todayKey, wasExisting, triggerView);
    }

    private void persistDailyLog(
            @NonNull com.google.firebase.firestore.DocumentReference logDocRef,
            @NonNull Map<String, Object> logData,
            @NonNull String dateKey,
            @NonNull String todayKey,
            boolean wasExisting,
            @NonNull View triggerView
    ) {
        logDocRef
                .set(logData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    logDocRef.getFirestore()
                            .waitForPendingWrites()
                            .addOnSuccessListener(unused -> {
                                triggerView.setEnabled(true);

                                if (!isAdded() || getContext() == null) {
                                    return;
                                }

                                FirebaseAuthState.clearAuthError(requireContext());

                                boolean isUpdate = wasExisting;
                                String toastMessage = isUpdate ? "Daily Log Updated!" : "Daily Log Saved successfully!";
                                Toast.makeText(getContext(), toastMessage, Toast.LENGTH_SHORT).show();

                                String notifIdPrefix = isUpdate ? "log_updated_" : "log_saved_";
                                String notifTitle = isUpdate ? "Daily Log Updated" : "Daily Log Saved";
                                String notifBody;
                                if (isUpdate) {
                                    notifBody = "Your tracked log was updated successfully.";
                                } else {
                                    notifBody = dateKey.equals(todayKey)
                                            ? "Your daily symptoms were saved and synced."
                                            : "A historical daily log was saved successfully.";
                                }

                                try {
                                    NotificationPublisher.publishForCurrentUser(
                                            getContext(),
                                            notifIdPrefix + dateKey + "_" + System.currentTimeMillis(),
                                            notifTitle,
                                            notifBody,
                                            "log",
                                            true
                                    );

                                    BackgroundTaskScheduler.enqueueImmediateSync(getContext(), isUpdate ? "daily_log_updated" : "daily_log_saved");
                                } catch (Exception e) {
                                    Log.e(TAG, "Post-save notification sync failed", e);
                                }

                                fetchPeriodLogicAndHistory();
                                fetchSymptomTrends();
                            })
                            .addOnFailureListener(e -> {
                                triggerView.setEnabled(true);
                                Log.e(TAG, "Daily log write was not acknowledged by Firebase", e);
                                handleCloudSaveFailure(
                                        e,
                                        "Could not confirm Firebase save. Please check connection and retry."
                                );
                            });
                })
                .addOnFailureListener(e -> {
                    triggerView.setEnabled(true);
                    Log.e(TAG, "Failed to save daily log", e);
                    handleCloudSaveFailure(
                            e,
                            "Could not save daily log. Please check internet and try again."
                    );
                });
    }

    private void handleCloudSaveFailure(@NonNull Exception error, @NonNull String genericMessage) {
        if (!isAdded() || getContext() == null) {
            return;
        }

        if (FirebaseAuthState.isAuthTokenError(error)) {
            FirebaseAuthState.markAuthError(requireContext());
            Toast.makeText(
                    getContext(),
                    "Firebase auth session failed. Please sign out, sign in again, and retry.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        Toast.makeText(getContext(), genericMessage, Toast.LENGTH_SHORT).show();
    }
}