package com.miniflo.femcare.repository;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.miniflo.femcare.data.AppDatabase;
import com.miniflo.femcare.data.CycleDao;
import com.miniflo.femcare.data.CycleEntity;
import com.miniflo.femcare.data.UserDao;
import com.miniflo.femcare.data.UserEntity;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuthRepository {
    private static final String TAG = "FIRESTORE_DEBUG";
    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;
    private final UserDao userDao;
    private final CycleDao cycleDao;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    public interface OnDataSavedListener {
        void onSaved(boolean success);
    }

    public AuthRepository(Application application) {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        AppDatabase db = AppDatabase.getInstance(application);
        userDao = db.userDao();
        cycleDao = db.cycleDao();

        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public LiveData<Boolean> registerUser(String email, String password, String name) {
        MutableLiveData<Boolean> registrationStatus = new MutableLiveData<>();

        String safeEmail = email == null ? "" : email.trim();
        String safePassword = password == null ? "" : password.trim();
        String safeName = name == null ? "" : name.trim();

        if (safeEmail.isEmpty() || safePassword.isEmpty()) {
            registrationStatus.setValue(false);
            return registrationStatus;
        }

        firebaseAuth.createUserWithEmailAndPassword(safeEmail, safePassword)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            UserEntity newUser = new UserEntity(safeEmail);
                            newUser.name = safeName;
                            newUser.age = 0;

                            firestore.collection("users").document(safeEmail)
                                    .get()
                                    .addOnSuccessListener(snapshot -> {
                                        Map<String, Object> userData = new HashMap<>();
                                        userData.put("email", safeEmail);
                                        userData.put("name", safeName);
                                        userData.put("age", 0);
                                        userData.put("onboardingComplete", false);

                                        if (!snapshot.contains("loginType")) {
                                            userData.put("loginType", "password");
                                        }
                                        if (!snapshot.contains("authProvider")) {
                                            userData.put("authProvider", "password");
                                        }

                                        firestore.collection("users").document(safeEmail)
                                                .set(userData, SetOptions.merge())
                                                .addOnSuccessListener(aVoid -> {
                                                    executorService.execute(() -> {
                                                        userDao.insertUser(newUser);
                                                        registrationStatus.postValue(true);
                                                    });
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.e(TAG, "Firestore Registration Error: ", e);
                                                    registrationStatus.postValue(false);
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Firestore Snapshot Error: ", e);
                                        registrationStatus.postValue(false);
                                    });
                        } else {
                            registrationStatus.postValue(false);
                        }
                    } else {
                        Log.e(TAG, "Auth Error: " + (task.getException() != null ? task.getException().getMessage() : "Unknown"));
                        registrationStatus.setValue(false);
                    }
                });

        return registrationStatus;
    }

    public void updateUserAge(String email, int calculatedAge, OnDataSavedListener listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("age", calculatedAge);

        firestore.collection("users").document(email)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    executorService.execute(() -> {
                        userDao.updateUserAge(email, calculatedAge);
                        if (listener != null) mainHandler.post(() -> listener.onSaved(true));
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating user age: ", e);
                    if (listener != null) mainHandler.post(() -> listener.onSaved(false));
                });
    }

    public void updateUserInfo(String email, boolean isRegular, boolean onBirthControl, int stressLevel, int height, int weight, double bmi, OnDataSavedListener listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isRegular", isRegular);
        updates.put("onBirthControl", onBirthControl);
        updates.put("stressLevel", stressLevel);
        updates.put("heightCm", height);
        updates.put("weightKg", weight);
        updates.put("bmi", bmi);

        firestore.collection("users").document(email)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    executorService.execute(() -> {
                        userDao.updateUserInfo(email, isRegular, onBirthControl, stressLevel, height, weight, bmi);
                        if (listener != null) mainHandler.post(() -> listener.onSaved(true));
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating user info: ", e);
                    if (listener != null) mainHandler.post(() -> listener.onSaved(false));
                });
    }

    public void updateCycleLength(String email, int cycleLength, OnDataSavedListener listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("averageCycleLength", cycleLength);

        firestore.collection("users").document(email)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    executorService.execute(() -> {
                        userDao.updateCycleLength(email, cycleLength);
                        if (listener != null) mainHandler.post(() -> listener.onSaved(true));
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating cycle length: ", e);
                    if (listener != null) mainHandler.post(() -> listener.onSaved(false));
                });
    }

    public void updatePeriodData(String email, int duration, long startMillis, OnDataSavedListener listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("periodDuration", duration);
        updates.put("lastPeriodStartMillis", startMillis);

        firestore.collection("users").document(email)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    executorService.execute(() -> {
                        userDao.updatePeriodDuration(email, duration);
                        
                        // NEW: Also log the first cycle entry into cycle_table for Experiment 10 Demo
                        long endMillis = startMillis + (duration * 86400000L); // duration in days to millis
                        CycleEntity firstCycle = new CycleEntity(startMillis, endMillis, 28, duration);
                        cycleDao.insertCycle(firstCycle);

                        if (listener != null) mainHandler.post(() -> listener.onSaved(true));
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating period data: ", e);
                    if (listener != null) mainHandler.post(() -> listener.onSaved(false));
                });
    }

    public void updateReproductiveHealth(String email, boolean hasProblem, OnDataSavedListener listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("hasPCOS", hasProblem);
        updates.put("hasThyroid", hasProblem);

        firestore.collection("users").document(email)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    executorService.execute(() -> {
                        userDao.updateReproductiveHealth(email, hasProblem);
                        if (listener != null) mainHandler.post(() -> listener.onSaved(true));
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating reproductive health: ", e);
                    if (listener != null) mainHandler.post(() -> listener.onSaved(false));
                });
    }

    public LiveData<Boolean> finalizeOnboarding(String email, String symptomsList) {
        MutableLiveData<Boolean> status = new MutableLiveData<>();
        Map<String, Object> updates = new HashMap<>();
        updates.put("symptoms", symptomsList);
        updates.put("onboardingComplete", true);

        firestore.collection("users").document(email)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    executorService.execute(() -> {
                        userDao.finalizeOnboarding(email, symptomsList);
                        status.postValue(true);
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Finalize Onboarding Error: ", e);
                    status.postValue(false);
                });
        return status;
    }

    public void updateLifestyleData(String email, boolean isPregnant, boolean tryingToConceive, int sleepHours, int exerciseFrequency, OnDataSavedListener listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isPregnant", isPregnant);
        updates.put("tryingToConceive", tryingToConceive);
        updates.put("sleepHours", sleepHours);
        updates.put("exerciseFrequency", exerciseFrequency);

        firestore.collection("users").document(email)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    executorService.execute(() -> {
                        userDao.updateLifestyleData(email, isPregnant, tryingToConceive, sleepHours, exerciseFrequency);
                        if (listener != null) mainHandler.post(() -> listener.onSaved(true));
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating lifestyle data: ", e);
                    if (listener != null) mainHandler.post(() -> listener.onSaved(false));
                });
    }

    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    public void signOut() {
        firebaseAuth.signOut();
    }
}