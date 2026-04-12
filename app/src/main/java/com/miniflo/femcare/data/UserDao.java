package com.miniflo.femcare.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUser(UserEntity user);

    @Query("SELECT * FROM user_table WHERE email = :email LIMIT 1")
    LiveData<UserEntity> getUserByEmail(String email);

    @Query("SELECT COUNT(*) FROM user_table")
    int getUserCountForWarmup();

    @Query("DELETE FROM user_table WHERE email = :email")
    void deleteUser(String email);

    @Query("UPDATE user_table SET age = :newAge WHERE email = :userEmail")
    void updateUserAge(String userEmail, int newAge);

    @Query("UPDATE user_table SET isRegular = :isRegular, onBirthControl = :onBirthControl, stressLevel = :stressLevel, heightCm = :height, weightKg = :weight, bmi = :bmi WHERE email = :userEmail")
    void updateUserInfo(String userEmail, boolean isRegular, boolean onBirthControl, int stressLevel, int height, int weight, double bmi);

    @Query("UPDATE user_table SET averageCycleLength = :cycleLength WHERE email = :userEmail")
    void updateCycleLength(String userEmail, int cycleLength);

    @Query("UPDATE user_table SET periodDuration = :duration WHERE email = :userEmail")
    void updatePeriodDuration(String userEmail, int duration);

    @Query("UPDATE user_table SET hasPCOS = :hasProblem WHERE email = :userEmail")
    void updateReproductiveHealth(String userEmail, boolean hasProblem);

    @Query("UPDATE user_table SET symptoms = :symptomsList WHERE email = :userEmail")
    void finalizeOnboarding(String userEmail, String symptomsList);

    @Query("UPDATE user_table SET isPregnant = :isPregnant, tryingToConceive = :tryingToConceive, sleepHours = :sleepHours, exerciseFrequency = :exerciseFrequency WHERE email = :userEmail")
    void updateLifestyleData(String userEmail, boolean isPregnant, boolean tryingToConceive, int sleepHours, int exerciseFrequency);
}