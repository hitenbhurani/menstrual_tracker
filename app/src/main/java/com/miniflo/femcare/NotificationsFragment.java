package com.miniflo.femcare;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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

public class NotificationsFragment extends Fragment {

    private RecyclerView recyclerView;
    private View emptyStateLayout;
    private TextView btnMarkAllRead;
    private NotifAdapter adapter;
    private List<NotificationModel> notifList = new ArrayList<>();

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        recyclerView = view.findViewById(R.id.notifRecyclerView);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        btnMarkAllRead = view.findViewById(R.id.btnMarkAllRead);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new NotifAdapter();
        recyclerView.setAdapter(adapter);

        setupSwipeGestures();

        if (user != null) {
            generateSmartAlerts();
            listenForNotifications();
        }

        btnMarkAllRead.setOnClickListener(v -> markAllAsRead());

        return view;
    }

    // --- SMART GENERATION ENGINE ---
    private void generateSmartAlerts() {
        String todayKey = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime());

        SharedPreferences prefs = requireActivity().getSharedPreferences("FemCarePrefs", Context.MODE_PRIVATE);
        boolean alertsGeneratedToday = prefs.getBoolean("alerts_generated_" + todayKey, false);

        // 1. Check if they missed tracking today (Dynamic check)
        db.collection("users").document(user.getEmail()).collection("daily_logs").document(todayKey).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        if (!prefs.getBoolean("missed_log_generated_" + todayKey, false)) {
                            pushNotificationToDb("missed_log_" + todayKey, "Missing Daily Log", "You haven't tracked your symptoms today. Tap here to keep your predictions accurate!", "reminder");
                            prefs.edit().putBoolean("missed_log_generated_" + todayKey, true).apply();
                        }
                    } else {
                        db.collection("users").document(user.getEmail()).collection("notifications").document("missed_log_" + todayKey).delete();
                    }
                });

        // 2. Generate Static Daily Alerts (BMI & Quote) ONLY ONCE per day
        if (!alertsGeneratedToday) {
            db.collection("users").document(user.getEmail()).get().addOnSuccessListener(doc -> {
                if (doc.contains("bmi")) {
                    double bmi = doc.getDouble("bmi");
                    if (bmi > 25.0 || bmi < 18.5) {
                        pushNotificationToDb("bmi_insight_" + todayKey, "Health Insight", "Based on your BMI parameters, staying hydrated and maintaining a balanced diet can help regulate your cycle significantly!", "insight");
                    }
                }
            });

            pushNotificationToDb("quote_" + todayKey, "Daily Motivation", "Listen to your body, it's smarter than you think. Have a wonderful day!", "quote");

            prefs.edit().putBoolean("alerts_generated_" + todayKey, true).apply();
        }
    }

    private void pushNotificationToDb(String id, String title, String msg, String type) {
        Map<String, Object> notif = new HashMap<>();
        notif.put("title", title);
        notif.put("message", msg);
        notif.put("type", type);
        notif.put("isRead", false);
        notif.put("timestamp", System.currentTimeMillis());
        db.collection("users").document(user.getEmail()).collection("notifications").document(id).set(notif, SetOptions.merge());
    }

    // --- REAL-TIME FIRESTORE LISTENER ---
    private void listenForNotifications() {
        db.collection("users").document(user.getEmail()).collection("notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    notifList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        NotificationModel n = new NotificationModel(
                                doc.getId(),
                                doc.getString("title"),
                                doc.getString("message"),
                                doc.getString("type"),
                                doc.getBoolean("isRead") != null ? doc.getBoolean("isRead") : false,
                                doc.getLong("timestamp") != null ? doc.getLong("timestamp") : System.currentTimeMillis()
                        );
                        notifList.add(n);
                    }

                    adapter.notifyDataSetChanged();

                    if (notifList.isEmpty()) {
                        emptyStateLayout.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        emptyStateLayout.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                });
    }

    // --- SWIPE GESTURES ---
    private void setupSwipeGestures() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return;

                NotificationModel notif = notifList.get(position);

                if (direction == ItemTouchHelper.LEFT) {
                    db.collection("users").document(user.getEmail()).collection("notifications").document(notif.id).delete();
                    Toast.makeText(getContext(), "Notification removed", Toast.LENGTH_SHORT).show();
                } else if (direction == ItemTouchHelper.RIGHT) {
                    boolean newReadStatus = !notif.isRead;
                    db.collection("users").document(user.getEmail()).collection("notifications").document(notif.id).update("isRead", newReadStatus);
                    adapter.notifyItemChanged(position); // Re-draws the item so it bounces back!
                }
            }
        };
        new ItemTouchHelper(simpleCallback).attachToRecyclerView(recyclerView);
    }

    private void markAllAsRead() {
        if (notifList.isEmpty()) return;

        for (NotificationModel n : notifList) {
            if (!n.isRead) {
                db.collection("users").document(user.getEmail()).collection("notifications").document(n.id).update("isRead", true);
            }
        }
        Toast.makeText(getContext(), "All caught up!", Toast.LENGTH_SHORT).show();
    }

    // --- ADAPTER & MODEL ---
    private class NotifAdapter extends RecyclerView.Adapter<NotifAdapter.NotifViewHolder> {
        @NonNull
        @Override
        public NotifViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
            return new NotifViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull NotifViewHolder holder, int position) {
            NotificationModel notif = notifList.get(position);
            holder.title.setText(notif.title);
            holder.message.setText(notif.message);

            long diff = System.currentTimeMillis() - notif.timestamp;
            if (diff < 60000) holder.time.setText("Just now");
            else if (diff < 3600000) holder.time.setText((diff / 60000) + "m ago");
            else if (diff < 86400000) holder.time.setText((diff / 3600000) + "h ago");
            else holder.time.setText((diff / 86400000) + "d ago");

            if (notif.isRead) {
                holder.unreadDot.setVisibility(View.GONE);
                holder.background.setBackgroundColor(Color.parseColor("#FFFFFF"));
                holder.title.setTextColor(Color.parseColor("#757575"));
            } else {
                holder.unreadDot.setVisibility(View.VISIBLE);
                holder.background.setBackgroundColor(Color.parseColor("#FFF5F8"));
                holder.title.setTextColor(Color.parseColor("#1A1A1A"));
            }

            holder.itemView.setOnClickListener(v -> {
                if (!notif.isRead) {
                    db.collection("users").document(user.getEmail()).collection("notifications").document(notif.id).update("isRead", true);
                }
                if (notif.type.equals("reminder")) {
                    requireActivity().getSupportFragmentManager().beginTransaction()
                            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                            .replace(R.id.fragment_container, new TrackFragment())
                            .addToBackStack(null)
                            .commit();
                }
            });
        }

        @Override
        public int getItemCount() { return notifList.size(); }

        class NotifViewHolder extends RecyclerView.ViewHolder {
            TextView title, message, time;
            View unreadDot, background;
            public NotifViewHolder(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.notifTitle);
                message = itemView.findViewById(R.id.notifMessage);
                time = itemView.findViewById(R.id.notifTime);
                unreadDot = itemView.findViewById(R.id.unreadDot);
                background = itemView.findViewById(R.id.notifBackground);
            }
        }
    }

    private class NotificationModel {
        String id, title, message, type;
        boolean isRead;
        long timestamp;
        public NotificationModel(String id, String title, String message, String type, boolean isRead, long timestamp) {
            this.id = id; this.title = title; this.message = message; this.type = type; this.isRead = isRead; this.timestamp = timestamp;
        }
    }
}