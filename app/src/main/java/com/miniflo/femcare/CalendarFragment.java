package com.miniflo.femcare;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
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

public class CalendarFragment extends Fragment {

    private TextView tvMonthYear;
    private RecyclerView calendarRecyclerView, rvRecentNotes;
    private CalendarAdapter adapter;
    private NotesAdapter notesAdapter;
    private Calendar currentCalendarDisplay;

    // Baseline Data
    private long lastPeriodMillis = 0;
    private int cycleLength = 28;
    private int periodDuration = 5;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        tvMonthYear = view.findViewById(R.id.tvMonthYear);
        calendarRecyclerView = view.findViewById(R.id.calendarRecyclerView);
        rvRecentNotes = view.findViewById(R.id.rvRecentNotes);
        ImageView btnPrev = view.findViewById(R.id.btnPrevMonth);
        ImageView btnNext = view.findViewById(R.id.btnNextMonth);

        setupLegendColors(view);

        currentCalendarDisplay = Calendar.getInstance();

        calendarRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 7));
        adapter = new CalendarAdapter();
        calendarRecyclerView.setAdapter(adapter);

        rvRecentNotes.setLayoutManager(new LinearLayoutManager(getContext()));
        notesAdapter = new NotesAdapter();
        rvRecentNotes.setAdapter(notesAdapter);

        fetchUserData();
        fetchRecentNotes();

        // Button Clicks
        btnPrev.setOnClickListener(v -> changeMonth(-1));
        btnNext.setOnClickListener(v -> changeMonth(1));

        // Connect the "Edit Period Dates" button to our new Bottom Sheet
        view.findViewById(R.id.btnEditDates).setOnClickListener(v -> openEditPeriodBottomSheet());

        // --- THE SWIPE GESTURES ---
        setupSwipeGestures();

        return view;
    }

    private void changeMonth(int amount) {
        currentCalendarDisplay.add(Calendar.MONTH, amount);
        buildCalendar();
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
                        if (diffX > 0) {
                            changeMonth(-1); // Swipe Right -> Previous Month
                        } else {
                            changeMonth(1);  // Swipe Left -> Next Month
                        }
                        return true;
                    }
                }
                return false;
            }
        });

        // Set the touch listener on the root view or a container to catch swipes
        View rootView = calendarRecyclerView.getRootView();
        rootView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true;
        });
        
        // Also ensure the RecyclerView doesn't consume all touch events if you want to swipe on it
        calendarRecyclerView.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                gestureDetector.onTouchEvent(e);
                return false;
            }
        });
    }

    // --- MATH FIX: NORMALIZE TIMESTAMPS TO MIDNIGHT ---
    private long normalizeToMidnight(long timeInMillis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timeInMillis);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private void fetchUserData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance().collection("users").document(user.getEmail()).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot doc = task.getResult();
                        if (doc.contains("lastPeriodStartMillis")) {
                            lastPeriodMillis = normalizeToMidnight(doc.getLong("lastPeriodStartMillis"));
                            cycleLength = doc.getLong("averageCycleLength").intValue();
                            periodDuration = doc.getLong("periodDuration").intValue();
                        }
                        buildCalendar();
                    }
                });
    }

    private void fetchRecentNotes() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance().collection("users").document(user.getEmail())
                .collection("notes")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<NoteItem> notes = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String date = doc.getId();
                        String text = doc.getString("noteText");
                        if (text != null && !text.isEmpty()) {
                            notes.add(new NoteItem(date, text));
                        }
                    }
                    notesAdapter.setNotes(notes);
                });
    }

    private void buildCalendar() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvMonthYear.setText(sdf.format(currentCalendarDisplay.getTime()));

        List<Calendar> daysInMonth = new ArrayList<>();
        Calendar monthCal = (Calendar) currentCalendarDisplay.clone();
        monthCal.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = monthCal.get(Calendar.DAY_OF_WEEK) - 1;
        monthCal.add(Calendar.DAY_OF_MONTH, -firstDayOfWeek);

        for (int i = 0; i < 42; i++) {
            daysInMonth.add((Calendar) monthCal.clone());
            monthCal.add(Calendar.DAY_OF_MONTH, 1);
        }

        adapter.setDays(daysInMonth);
    }

    // --- FEATURE 1: EDIT PERIOD DATES LOGIC ---
    private void openEditPeriodBottomSheet() {
        BottomSheetDialog editSheet = new BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_edit_period, null);
        editSheet.setContentView(sheetView);

        MaterialButton btnSelectNewDate = sheetView.findViewById(R.id.btnSelectNewDate);
        TextInputEditText etEditCycleLength = sheetView.findViewById(R.id.etEditCycleLength);
        MaterialButton btnSaveEdits = sheetView.findViewById(R.id.btnSaveEdits);

        etEditCycleLength.setText(String.valueOf(cycleLength));
        final long[] newlySelectedMillis = {lastPeriodMillis};

        btnSelectNewDate.setOnClickListener(v -> {
            Calendar currentCal = Calendar.getInstance();
            currentCal.setTimeInMillis(lastPeriodMillis);

            android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        Calendar selectedCal = Calendar.getInstance();
                        selectedCal.set(year, month, dayOfMonth);
                        newlySelectedMillis[0] = normalizeToMidnight(selectedCal.getTimeInMillis());

                        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
                        btnSelectNewDate.setText("Start Date: " + sdf.format(selectedCal.getTime()));
                    },
                    currentCal.get(Calendar.YEAR),
                    currentCal.get(Calendar.MONTH),
                    currentCal.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        btnSaveEdits.setOnClickListener(v -> {
            String newCycleStr = etEditCycleLength.getText().toString().trim();
            if (newCycleStr.isEmpty()) {
                etEditCycleLength.setError("Please enter a cycle length");
                return;
            }

            int newCycle = Integer.parseInt(newCycleStr);
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

            if (user != null && user.getEmail() != null) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("lastPeriodStartMillis", newlySelectedMillis[0]);
                updates.put("averageCycleLength", newCycle);

                FirebaseFirestore.getInstance().collection("users").document(user.getEmail())
                        .update(updates)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "Cycle updated!", Toast.LENGTH_SHORT).show();
                            lastPeriodMillis = newlySelectedMillis[0];
                            cycleLength = newCycle;
                            adapter.notifyDataSetChanged(); // Redraw calendar immediately!
                            editSheet.dismiss();
                        });
            }
        });

        editSheet.show();
    }

    // --- FEATURE 2: SAVE NOTES LOGIC ---
    private void openNoteBottomSheet(Calendar selectedDate, String status) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_note, null);
        bottomSheetDialog.setContentView(sheetView);

        TextView tvTitle = sheetView.findViewById(R.id.tvSheetDateTitle);
        TextView tvStatus = sheetView.findViewById(R.id.tvSheetStatus);
        TextInputEditText etNote = sheetView.findViewById(R.id.etNoteInput);
        MaterialButton btnSave = sheetView.findViewById(R.id.btnSaveNote);

        SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
        String dbDateKey = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.getTime());

        tvTitle.setText(sdf.format(selectedDate.getTime()));
        tvStatus.setText("Status: " + status);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseFirestore.getInstance().collection("users").document(user.getEmail())
                    .collection("notes").document(dbDateKey).get().addOnSuccessListener(doc -> {
                        if (doc.exists() && doc.contains("noteText")) {
                            etNote.setText(doc.getString("noteText"));
                        }
                    });
        }

        btnSave.setOnClickListener(v -> {
            String note = etNote.getText().toString().trim();
            if (user != null) {
                Map<String, Object> noteData = new HashMap<>();
                noteData.put("noteText", note);
                noteData.put("timestamp", System.currentTimeMillis());

                FirebaseFirestore.getInstance().collection("users").document(user.getEmail())
                        .collection("notes").document(dbDateKey)
                        .set(noteData, SetOptions.merge())
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "Note saved securely!", Toast.LENGTH_SHORT).show();
                            fetchRecentNotes(); // Refresh list
                            bottomSheetDialog.dismiss();
                        });
            }
        });

        bottomSheetDialog.show();
    }

    private class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder> {
        private List<Calendar> days = new ArrayList<>();

        public void setDays(List<Calendar> days) {
            this.days = days;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
            return new CalendarViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
            Calendar cellDate = days.get(position);
            holder.cellDayText.setText(String.valueOf(cellDate.get(Calendar.DAY_OF_MONTH)));

            if (cellDate.get(Calendar.MONTH) == currentCalendarDisplay.get(Calendar.MONTH)) {
                holder.cellDayText.setTextColor(Color.BLACK);
            } else {
                holder.cellDayText.setTextColor(Color.parseColor("#757575")); // Darker gray for other months
            }

            String dayStatus = "No active phase";
            holder.cellDayText.setBackground(null);

            if (lastPeriodMillis > 0) {
                long cellMillis = normalizeToMidnight(cellDate.getTimeInMillis());
                long diffMillis = cellMillis - lastPeriodMillis;
                int daysDiff = (int) Math.floor(diffMillis / (1000.0 * 60 * 60 * 24));

                int cycleDay = daysDiff % cycleLength;
                if (cycleDay < 0) {
                    cycleDay = (cycleLength + cycleDay) % cycleLength;
                }

                if (cycleDay >= 0 && cycleDay < periodDuration) {
                    if (daysDiff < cycleLength) {
                        holder.cellDayText.setBackground(createCircleBg("#C2185B", true));
                        holder.cellDayText.setTextColor(Color.WHITE);
                        dayStatus = "Period Day " + (cycleDay + 1);
                    } else {
                        holder.cellDayText.setBackground(createCircleBg("#C2185B", false));
                        dayStatus = "Expected Period";
                    }
                } else if (cycleDay == cycleLength - 14) {
                    holder.cellDayText.setBackground(createCircleBg("#F44336", true));
                    holder.cellDayText.setTextColor(Color.WHITE);
                    dayStatus = "Ovulation Day";
                } else if (cycleDay >= cycleLength - 19 && cycleDay <= cycleLength - 13) {
                    holder.cellDayText.setBackground(createCircleBg("#F8BBD0", true));
                    dayStatus = "Fertile Window";
                }
            }

            String finalStatus = dayStatus;
            holder.itemView.setOnClickListener(v -> openNoteBottomSheet(cellDate, finalStatus));
        }

        @Override
        public int getItemCount() { return days.size(); }

        class CalendarViewHolder extends RecyclerView.ViewHolder {
            TextView cellDayText;
            public CalendarViewHolder(@NonNull View itemView) {
                super(itemView);
                cellDayText = itemView.findViewById(R.id.cellDayText);
            }
        }
    }

    // --- RECENT NOTES ADAPTER ---
    private class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {
        private List<NoteItem> notes = new ArrayList<>();

        public void setNotes(List<NoteItem> notes) { this.notes = notes; notifyDataSetChanged(); }

        @NonNull
        @Override
        public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent_note, parent, false);
            return new NoteViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
            NoteItem item = notes.get(position);
            holder.tvNoteDate.setText(item.date);
            holder.tvNoteText.setText(item.text);
        }

        @Override
        public int getItemCount() { return notes.size(); }

        class NoteViewHolder extends RecyclerView.ViewHolder {
            TextView tvNoteDate, tvNoteText;
            public NoteViewHolder(@NonNull View v) {
                super(v);
                tvNoteDate = v.findViewById(R.id.tvNoteDate);
                tvNoteText = v.findViewById(R.id.tvNoteText);
            }
        }
    }

    private static class NoteItem {
        String date, text;
        NoteItem(String date, String text) { this.date = date; this.text = text; }
    }

    private GradientDrawable createCircleBg(String hexColor, boolean isSolid) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setSize(40, 40);
        if (isSolid) {
            shape.setColor(Color.parseColor(hexColor));
        } else {
            shape.setColor(Color.TRANSPARENT);
            shape.setStroke(4, Color.parseColor(hexColor));
        }
        return shape;
    }

    private void setupLegendColors(View v) {
        TextView p = v.findViewById(R.id.legPeriod);
        TextView e = v.findViewById(R.id.legExpected);
        TextView o = v.findViewById(R.id.legOvulation);
        TextView f = v.findViewById(R.id.legFertile);

        p.setCompoundDrawablesWithIntrinsicBounds(createCircleBg("#C2185B", true), null, null, null);
        e.setCompoundDrawablesWithIntrinsicBounds(createCircleBg("#C2185B", false), null, null, null);
        o.setCompoundDrawablesWithIntrinsicBounds(createCircleBg("#F44336", true), null, null, null);
        f.setCompoundDrawablesWithIntrinsicBounds(createCircleBg("#F8BBD0", true), null, null, null);
    }
}
