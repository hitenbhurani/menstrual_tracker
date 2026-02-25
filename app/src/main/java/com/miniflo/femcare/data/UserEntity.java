package com.miniflo.femcare.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

// This tells Room to create a SQLite table named "user_table"
@Entity(tableName = "user_table")
public class UserEntity {

    @PrimaryKey
    @NonNull
    public String email; // Using email as the primary key since it matches Firebase

    public String name;
    public int age;

    // Cycle Data
    public int averageCycleLength;
    public int periodDuration;
    public boolean isRegular;

    // Health Data
    public boolean hasPCOS;
    public boolean hasThyroid;
    public boolean onBirthControl;
    public boolean isPregnant;
    public boolean tryingToConceive;

    // Lifestyle Data
    public int stressLevel; // 1-5 scale
    public int sleepHours; // 0-12
    public int exerciseFrequency; // days per week
    public double weightKg;
    public double heightCm;
    public double bmi;
    
    public String symptoms;

    // Constructor required by Room
    public UserEntity(@NonNull String email) {
        this.email = email;
    }
}