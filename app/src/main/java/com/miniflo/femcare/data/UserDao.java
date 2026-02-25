package com.miniflo.femcare.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface UserDao {

    // Saves the user profile. If it already exists, it overwrites it with the new updated data!
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUser(UserEntity user);

    // Grabs the specific user's data using their email
    @Query("SELECT * FROM user_table WHERE email = :email LIMIT 1")
    LiveData<UserEntity> getUserByEmail(String email);

    // Deletes the user profile (used when they log out or delete account)
    @Query("DELETE FROM user_table WHERE email = :email")
    void deleteUser(String email);

//    this one line of code to the interface so Room knows how to update the age:
    @Query("UPDATE user_table SET age = :newAge WHERE email = :userEmail")
    void updateUserAge(String userEmail, int newAge);

//    Update Database Tools for User Info
//    We need to teach the DAO and Repository how to save these specific fields.
    @Query("UPDATE user_table SET isRegular = :isRegular, onBirthControl = :onBirthControl, stressLevel = :stressLevel, heightCm = :height, weightKg = :weight, bmi = :bmi WHERE email = :userEmail")
    void updateUserInfo(String userEmail, boolean isRegular, boolean onBirthControl, int stressLevel, int height, int weight, double bmi);

//    let Room know how to save the cycle length:
    @Query("UPDATE user_table SET averageCycleLength = :cycleLength WHERE email = :userEmail")
    void updateCycleLength(String userEmail, int cycleLength);

//this query so Room knows how to update the duration:
    @Query("UPDATE user_table SET periodDuration = :duration WHERE email = :userEmail")
    void updatePeriodDuration(String userEmail, int duration);

    @Query("UPDATE user_table SET hasPCOS = :hasProblem WHERE email = :userEmail")
    void updateReproductiveHealth(String userEmail, boolean hasProblem);

    @Query("UPDATE user_table SET symptoms = :symptomsList WHERE email = :userEmail")
    void finalizeOnboarding(String userEmail, String symptomsList);

    @Query("UPDATE user_table SET isPregnant = :isPregnant, tryingToConceive = :tryingToConceive, sleepHours = :sleepHours, exerciseFrequency = :exerciseFrequency WHERE email = :userEmail")
    void updateLifestyleData(String userEmail, boolean isPregnant, boolean tryingToConceive, int sleepHours, int exerciseFrequency);
}
