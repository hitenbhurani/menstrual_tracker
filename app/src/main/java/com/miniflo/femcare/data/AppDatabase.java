package com.miniflo.femcare.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

// Tell Room exactly which tables to build
@Database(entities = {UserEntity.class, CycleEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    // Link the DAOs
    public abstract UserDao userDao();
    public abstract CycleDao cycleDao();

    // Singleton instance to prevent memory leaks
    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "femcare_database")
                            .fallbackToDestructiveMigration() // Wipes database if we change schema later
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}