package com.miniflo.femcare.repository;

import android.app.Application;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.miniflo.femcare.data.AppDatabase;
import com.miniflo.femcare.data.UserDao;
import com.miniflo.femcare.data.UserEntity;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuthRepository {
    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;
    private final UserDao userDao;
    private final ExecutorService executorService;

    public AuthRepository(Application application) {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        AppDatabase db = AppDatabase.getInstance(application);
        userDao = db.userDao();

        executorService = Executors.newSingleThreadExecutor();
    }

    // Removed the 'age' parameter
    public LiveData<Boolean> registerUser(String email, String password, String name) {
        MutableLiveData<Boolean> registrationStatus = new MutableLiveData<>();

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                        if (firebaseUser != null) {

                            UserEntity newUser = new UserEntity(email);
                            newUser.name = name;
                            newUser.age = 0; // Temporarily set to 0. We will update this on the BirthDateActivity!

                            firestore.collection("users").document(email)
                                    .set(newUser)
                                    .addOnSuccessListener(aVoid -> {
                                        executorService.execute(() -> {
                                            userDao.insertUser(newUser);
                                            registrationStatus.postValue(true);
                                        });
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("AuthRepo", "Firestore Error: " + e.getMessage());
                                        registrationStatus.postValue(false);
                                    });
                        }
                    } else {
                        Log.e("AuthRepo", "Auth Error: " + task.getException().getMessage());
                        registrationStatus.setValue(false);
                    }
                });

        return registrationStatus;
    }

    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    public void signOut() {
        firebaseAuth.signOut();
    }

    public void updateUserAge(String email, int calculatedAge) {
        // 1. Update Cloud
        firestore.collection("users").document(email)
                .update("age", calculatedAge)
                .addOnSuccessListener(aVoid -> {
                    // 2. Update Local Room DB
                    executorService.execute(() -> userDao.updateUserAge(email, calculatedAge));
                });
    }

    public void updateUserInfo(String email, boolean isRegular, boolean onBirthControl, int stressLevel, int height, int weight, double bmi) {
        // 1. Prepare data map for Cloud
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("isRegular", isRegular);
        updates.put("onBirthControl", onBirthControl);
        updates.put("stressLevel", stressLevel);
        updates.put("heightCm", height);
        updates.put("weightKg", weight);
        updates.put("bmi", bmi);

        // 2. Update Cloud
        firestore.collection("users").document(email)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // 3. Update Local Room DB
                    executorService.execute(() -> userDao.updateUserInfo(email, isRegular, onBirthControl, stressLevel, height, weight, bmi));
                });
    }


    public void updateCycleLength(String email, int cycleLength) {
        firestore.collection("users").document(email)
                .update("averageCycleLength", cycleLength)
                .addOnSuccessListener(aVoid -> {
                    executorService.execute(() -> userDao.updateCycleLength(email, cycleLength));
                });
    }

    public void updatePeriodData(String email, int duration, long startMillis) {
        // 1. Prepare data for the Cloud
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("periodDuration", duration);
        updates.put("lastPeriodStartMillis", startMillis);

        // 2. Push to Firestore
        firestore.collection("users").document(email)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // 3. Update Local Room DB
                    executorService.execute(() -> userDao.updatePeriodDuration(email, duration));
                });
    }

    public void updateReproductiveHealth(String email, boolean hasProblem) {
        // We will update both hasPCOS and hasThyroid to the same boolean for now
        // based on their general answer to this question.
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("hasPCOS", hasProblem);
        updates.put("hasThyroid", hasProblem);

        firestore.collection("users").document(email)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    executorService.execute(() -> userDao.updateReproductiveHealth(email, hasProblem));
                });
    }

    public LiveData<Boolean> finalizeOnboarding(String email, String symptomsList) {
        MutableLiveData<Boolean> status = new MutableLiveData<>();
        // 1. Prepare data for the Cloud
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("symptoms", symptomsList);
        updates.put("onboardingComplete", true); // 🔥 THIS IS THE MAGIC FLAG!

        // 2. Push to Firestore
        firestore.collection("users").document(email)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // 3. Update Local Room DB
                    executorService.execute(() -> {
                        userDao.finalizeOnboarding(email, symptomsList);
                        status.postValue(true);
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("AuthRepo", "Finalize Onboarding Error: " + e.getMessage());
                    status.postValue(false);
                });
        return status;
    }

    public void updateLifestyleData(String email, boolean isPregnant, boolean tryingToConceive, int sleepHours, int exerciseFrequency) {
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("isPregnant", isPregnant);
        updates.put("tryingToConceive", tryingToConceive);
        updates.put("sleepHours", sleepHours);
        updates.put("exerciseFrequency", exerciseFrequency);

        firestore.collection("users").document(email)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    executorService.execute(() -> userDao.updateLifestyleData(email, isPregnant, tryingToConceive, sleepHours, exerciseFrequency));
                });
    }
}