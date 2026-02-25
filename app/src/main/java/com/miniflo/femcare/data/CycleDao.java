package com.miniflo.femcare.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface CycleDao {

    // Saves a new period log to the database
    @Insert
    void insertCycle(CycleEntity cycle);

    // Pulls every single cycle logged, ordered by the most recent first
    @Query("SELECT * FROM cycle_table ORDER BY startDateMillis DESC")
    LiveData<List<CycleEntity>> getAllCycles();

    // Pulls just the last 6 cycles so we can calculate the Weighted Moving Average!
    @Query("SELECT * FROM cycle_table ORDER BY startDateMillis DESC LIMIT 6")
    List<CycleEntity> getRecentCyclesForMath();

    // Deletes a specific cycle (for your "Swipe to Delete" gesture requirement)
    @Query("DELETE FROM cycle_table WHERE cycleId = :cycleId")
    void deleteCycle(int cycleId);
}