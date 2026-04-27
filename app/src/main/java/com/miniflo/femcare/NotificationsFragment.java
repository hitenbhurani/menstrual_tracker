package com.miniflo.femcare;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
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
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationsFragment extends Fragment {

    private static final String TAG = "NotificationsFragment";

    private RecyclerView recyclerView;
    private View emptyStateLayout;
    private TextView btnMarkAllRead;
    private NotifAdapter adapter;
    private final List<NotificationModel> notifList = new ArrayList<>();

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration notificationsListener;

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
        loadLocalNotifications();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (hasValidUser(currentUser)) {
            listenForNotifications(currentUser);
        }

        btnMarkAllRead.setOnClickListener(v -> markAllAsRead());

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (notificationsListener != null) {
            notificationsListener.remove();
            notificationsListener = null;
        }
    }

    private boolean hasValidUser(@Nullable FirebaseUser user) {
        return user != null
                && user.getEmail() != null
                && !user.getEmail().trim().isEmpty();
    }

    private void loadLocalNotifications() {
        if (!isAdded() || getContext() == null) {
            return;
        }

        notifList.clear();
        for (LocalNotificationStore.Item item : LocalNotificationStore.getAll(requireContext())) {
            notifList.add(new NotificationModel(item.id, item.title, item.message, item.type, item.isRead, item.timestamp));
        }

        sortNotifications();
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void listenForNotifications(@NonNull FirebaseUser user) {
        if (notificationsListener != null) {
            notificationsListener.remove();
            notificationsListener = null;
        }

        String email = user.getEmail().trim();
        notificationsListener = db.collection("users")
                .document(email)
                .collection("notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (!isAdded() || getContext() == null) {
                        return;
                    }

                    if (error != null) {
                        Log.w(TAG, "Notifications listener failed", error);
                        if (FirebaseAuthState.isAuthTokenError(error)) {
                            FirebaseAuthState.markAuthError(requireContext());
                            FirebaseAuthState.logAuthErrorDetail(requireContext(), error);
                            if (notificationsListener != null) {
                                notificationsListener.remove();
                                notificationsListener = null;
                            }
                            if (isAdded() && getContext() != null) {
                                Toast.makeText(
                                        getContext(),
                                        "Firebase session issue detected. Showing cached notifications.",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                        loadLocalNotifications();
                        return;
                    }

                    if (value == null) {
                        loadLocalNotifications();
                        return;
                    }

                    FirebaseAuthState.clearAuthError(requireContext());

                    Map<String, NotificationModel> merged = new HashMap<>();

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        String id = doc.getId();
                        String title = doc.getString("title");
                        String message = doc.getString("message");
                        String type = doc.getString("type");
                        boolean isRead = doc.getBoolean("isRead") != null && Boolean.TRUE.equals(doc.getBoolean("isRead"));
                        long timestamp = doc.getLong("timestamp") != null
                                ? doc.getLong("timestamp")
                                : System.currentTimeMillis();

                        merged.put(id, new NotificationModel(
                                id,
                                title != null ? title : "Notification",
                                message != null ? message : "",
                                type != null ? type : "general",
                                isRead,
                                timestamp
                        ));
                    }

                    for (LocalNotificationStore.Item local : LocalNotificationStore.getAll(requireContext())) {
                        if (!merged.containsKey(local.id)) {
                            merged.put(local.id, new NotificationModel(
                                    local.id,
                                    local.title,
                                    local.message,
                                    local.type,
                                    local.isRead,
                                    local.timestamp
                            ));
                        }
                    }

                    notifList.clear();
                    notifList.addAll(merged.values());
                    sortNotifications();
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                });
    }

    private void sortNotifications() {
        Collections.sort(notifList, Comparator.comparingLong((NotificationModel n) -> n.timestamp).reversed());
    }

    private void updateEmptyState() {
        if (notifList.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void setupSwipeGestures() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position == RecyclerView.NO_POSITION || position >= notifList.size()) {
                    return;
                }

                NotificationModel notif = notifList.get(position);
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

                if (direction == ItemTouchHelper.LEFT) {
                    LocalNotificationStore.delete(requireContext(), notif.id);
                    notifList.remove(position);
                    adapter.notifyItemRemoved(position);
                    updateEmptyState();

                    if (hasValidUser(currentUser)) {
                        db.collection("users")
                                .document(currentUser.getEmail().trim())
                                .collection("notifications")
                                .document(notif.id)
                                .delete();
                    }

                    Toast.makeText(getContext(), "Notification removed", Toast.LENGTH_SHORT).show();
                } else if (direction == ItemTouchHelper.RIGHT) {
                    boolean newReadStatus = !notif.isRead;
                    notif.isRead = newReadStatus;
                    LocalNotificationStore.markRead(requireContext(), notif.id, newReadStatus);
                    adapter.notifyItemChanged(position);

                    if (hasValidUser(currentUser)) {
                        db.collection("users")
                                .document(currentUser.getEmail().trim())
                                .collection("notifications")
                                .document(notif.id)
                                .update("isRead", newReadStatus);
                    }
                }
            }
        };

        new ItemTouchHelper(simpleCallback).attachToRecyclerView(recyclerView);
    }

    private void markAllAsRead() {
        if (notifList.isEmpty()) {
            return;
        }

        LocalNotificationStore.markAllRead(requireContext());

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        for (NotificationModel n : notifList) {
            if (!n.isRead) {
                n.isRead = true;
                if (hasValidUser(currentUser)) {
                    db.collection("users")
                            .document(currentUser.getEmail().trim())
                            .collection("notifications")
                            .document(n.id)
                            .update("isRead", true);
                }
            }
        }

        adapter.notifyDataSetChanged();
        Toast.makeText(getContext(), "All caught up!", Toast.LENGTH_SHORT).show();
    }

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
            if (diff < 60_000L) {
                holder.time.setText("Just now");
            } else if (diff < 3_600_000L) {
                holder.time.setText((diff / 60_000L) + "m ago");
            } else if (diff < 86_400_000L) {
                holder.time.setText((diff / 3_600_000L) + "h ago");
            } else {
                holder.time.setText((diff / 86_400_000L) + "d ago");
            }

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
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition == RecyclerView.NO_POSITION || adapterPosition >= notifList.size()) {
                    return;
                }

                NotificationModel clicked = notifList.get(adapterPosition);
                if (!clicked.isRead) {
                    clicked.isRead = true;
                    LocalNotificationStore.markRead(requireContext(), clicked.id, true);
                    adapter.notifyItemChanged(adapterPosition);

                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (hasValidUser(currentUser)) {
                        db.collection("users")
                                .document(currentUser.getEmail().trim())
                                .collection("notifications")
                                .document(clicked.id)
                                .update("isRead", true);
                    }
                }

                if ("reminder".equals(clicked.type)) {
                    requireActivity().getSupportFragmentManager().beginTransaction()
                            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                            .replace(R.id.fragment_container, new TrackFragment())
                            .addToBackStack(null)
                            .commit();
                }
            });
        }

        @Override
        public int getItemCount() {
            return notifList.size();
        }

        class NotifViewHolder extends RecyclerView.ViewHolder {
            TextView title;
            TextView message;
            TextView time;
            View unreadDot;
            View background;

            NotifViewHolder(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.notifTitle);
                message = itemView.findViewById(R.id.notifMessage);
                time = itemView.findViewById(R.id.notifTime);
                unreadDot = itemView.findViewById(R.id.unreadDot);
                background = itemView.findViewById(R.id.notifBackground);
            }
        }
    }

    private static class NotificationModel {
        String id;
        String title;
        String message;
        String type;
        boolean isRead;
        long timestamp;

        NotificationModel(String id, String title, String message, String type, boolean isRead, long timestamp) {
            this.id = id;
            this.title = title;
            this.message = message;
            this.type = type;
            this.isRead = isRead;
            this.timestamp = timestamp;
        }
    }
}
