package com.miniflo.femcare.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "cycle_table")
public class CycleEntity {

    @PrimaryKey(autoGenerate = true)
    public int cycleId; // Room will automatically number these 1, 2, 3...

    public long startDateMillis;
    public long endDateMillis;
    public int cycleLength; // How many days between this cycle and the next
    public int periodDuration; // How many days bleeding lasted

    // We can flag cycles that are extremely irregular (>45 days)
    public boolean isIrregular;

    public CycleEntity(long startDateMillis, long endDateMillis, int cycleLength, int periodDuration) {
        this.startDateMillis = startDateMillis;
        this.endDateMillis = endDateMillis;
        this.cycleLength = cycleLength;
        this.periodDuration = periodDuration;
        this.isIrregular = cycleLength > 45; // Automatic validation rule from your requirements!
    }
}