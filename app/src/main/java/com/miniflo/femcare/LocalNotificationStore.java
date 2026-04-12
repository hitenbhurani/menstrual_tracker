package com.miniflo.femcare;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class LocalNotificationStore {

    private static final String PREFS_NAME = "FemCarePrefs";
    private static final String KEY_LOCAL_FEED_JSON = "local_notification_feed_json_v1";
    private static final int MAX_ITEMS = 200;

    private LocalNotificationStore() {
    }

    public static final class Item {
        public final String id;
        public final String title;
        public final String message;
        public final String type;
        public final boolean isRead;
        public final long timestamp;

        public Item(
                @NonNull String id,
                @NonNull String title,
                @NonNull String message,
                @NonNull String type,
                boolean isRead,
                long timestamp
        ) {
            this.id = id;
            this.title = title;
            this.message = message;
            this.type = type;
            this.isRead = isRead;
            this.timestamp = timestamp;
        }
    }

    public static synchronized void upsert(
            @NonNull Context context,
            @NonNull String id,
            @NonNull String title,
            @NonNull String message,
            @NonNull String type,
            boolean isRead,
            long timestamp
    ) {
        List<Item> items = readItems(context);
        boolean found = false;

        for (int i = 0; i < items.size(); i++) {
            Item existing = items.get(i);
            if (existing.id.equals(id)) {
                boolean mergedRead = existing.isRead || isRead;
                long mergedTimestamp = Math.max(existing.timestamp, timestamp);
                items.set(i, new Item(id, title, message, type, mergedRead, mergedTimestamp));
                found = true;
                break;
            }
        }

        if (!found) {
            items.add(new Item(id, title, message, type, isRead, timestamp));
        }

        sortNewestFirst(items);

        if (items.size() > MAX_ITEMS) {
            items = new ArrayList<>(items.subList(0, MAX_ITEMS));
        }

        writeItems(context, items);
    }

    @NonNull
    public static synchronized List<Item> getAll(@NonNull Context context) {
        List<Item> items = readItems(context);
        sortNewestFirst(items);
        return items;
    }

    public static synchronized void markRead(
            @NonNull Context context,
            @NonNull String id,
            boolean isRead
    ) {
        List<Item> items = readItems(context);
        boolean changed = false;

        for (int i = 0; i < items.size(); i++) {
            Item existing = items.get(i);
            if (existing.id.equals(id)) {
                items.set(i, new Item(existing.id, existing.title, existing.message, existing.type, isRead, existing.timestamp));
                changed = true;
                break;
            }
        }

        if (changed) {
            writeItems(context, items);
        }
    }

    public static synchronized void markAllRead(@NonNull Context context) {
        List<Item> items = readItems(context);
        boolean changed = false;

        for (int i = 0; i < items.size(); i++) {
            Item existing = items.get(i);
            if (!existing.isRead) {
                items.set(i, new Item(existing.id, existing.title, existing.message, existing.type, true, existing.timestamp));
                changed = true;
            }
        }

        if (changed) {
            writeItems(context, items);
        }
    }

    public static synchronized void delete(@NonNull Context context, @NonNull String id) {
        List<Item> items = readItems(context);
        boolean changed = false;

        for (int i = items.size() - 1; i >= 0; i--) {
            if (items.get(i).id.equals(id)) {
                items.remove(i);
                changed = true;
            }
        }

        if (changed) {
            writeItems(context, items);
        }
    }

    @NonNull
    private static List<Item> readItems(@NonNull Context context) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_LOCAL_FEED_JSON, "[]");

        List<Item> items = new ArrayList<>();

        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.optJSONObject(i);
                if (obj == null) {
                    continue;
                }

                String id = obj.optString("id", "");
                if (id.trim().isEmpty()) {
                    continue;
                }

                String title = obj.optString("title", "Notification");
                String message = obj.optString("message", "");
                String type = obj.optString("type", "general");
                boolean isRead = obj.optBoolean("isRead", false);
                long timestamp = obj.optLong("timestamp", System.currentTimeMillis());

                items.add(new Item(id, title, message, type, isRead, timestamp));
            }
        } catch (Exception ignored) {
            return new ArrayList<>();
        }

        return items;
    }

    private static void writeItems(@NonNull Context context, @NonNull List<Item> items) {
        JSONArray array = new JSONArray();

        for (Item item : items) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("id", item.id);
                obj.put("title", item.title);
                obj.put("message", item.message);
                obj.put("type", item.type);
                obj.put("isRead", item.isRead);
                obj.put("timestamp", item.timestamp);
                array.put(obj);
            } catch (Exception ignored) {
                // Ignore malformed item and continue writing remaining entries.
            }
        }

        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LOCAL_FEED_JSON, array.toString()).apply();
    }

    private static void sortNewestFirst(@NonNull List<Item> items) {
        Collections.sort(items, Comparator.comparingLong((Item item) -> item.timestamp).reversed());
    }
}
