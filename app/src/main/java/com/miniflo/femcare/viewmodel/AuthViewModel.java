package com.miniflo.femcare.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.miniflo.femcare.repository.AuthRepository;

public class AuthViewModel extends AndroidViewModel {

    private final AuthRepository repository;

    public AuthViewModel(@NonNull Application application) {
        super(application);
        repository = new AuthRepository(application);
    }

    // Removed the 'age' parameter here too
    public LiveData<Boolean> register(String email, String password, String name) {
        return repository.registerUser(email, password, name);
    }

    // this method so the UI can safely talk to the Repository:
    public void updateAge(String email, int calculatedAge) {
        repository.updateUserAge(email, calculatedAge);
    }

    public void updateUserInfo(String email, boolean isRegular, boolean onBirthControl, int stressLevel, int height, int weight, double bmi) {
        repository.updateUserInfo(email, isRegular, onBirthControl, stressLevel, height, weight, bmi);
    }

    public void updateCycleLength(String email, int cycleLength) {
        repository.updateCycleLength(email, cycleLength);
    }

//    Add the bridge method:
    public void updatePeriodData(String email, int duration, long startMillis) {
        repository.updatePeriodData(email, duration, startMillis);
    }

    public void updateReproductiveHealth(String email, boolean hasProblem) {
        repository.updateReproductiveHealth(email, hasProblem);
    }

    public LiveData<Boolean> finalizeOnboarding(String email, String symptomsList) {
        return repository.finalizeOnboarding(email, symptomsList);
    }

    public void updateLifestyleData(String email, boolean isPregnant, boolean tryingToConceive, int sleepHours, int exerciseFrequency) {
        repository.updateLifestyleData(email, isPregnant, tryingToConceive, sleepHours, exerciseFrequency);
    }
}
