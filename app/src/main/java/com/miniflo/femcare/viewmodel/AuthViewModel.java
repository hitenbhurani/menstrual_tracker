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

    public LiveData<Boolean> register(String email, String password, String name) {
        return repository.registerUser(email, password, name);
    }

    public void updateAge(String email, int calculatedAge, AuthRepository.OnDataSavedListener listener) {
        repository.updateUserAge(email, calculatedAge, listener);
    }

    public void updateUserInfo(String email, boolean isRegular, boolean onBirthControl, int stressLevel, int height, int weight, double bmi, AuthRepository.OnDataSavedListener listener) {
        repository.updateUserInfo(email, isRegular, onBirthControl, stressLevel, height, weight, bmi, listener);
    }

    public void updateCycleLength(String email, int cycleLength, AuthRepository.OnDataSavedListener listener) {
        repository.updateCycleLength(email, cycleLength, listener);
    }

    public void updatePeriodData(String email, int duration, long startMillis, AuthRepository.OnDataSavedListener listener) {
        repository.updatePeriodData(email, duration, startMillis, listener);
    }

    public void updateReproductiveHealth(String email, boolean hasProblem, AuthRepository.OnDataSavedListener listener) {
        repository.updateReproductiveHealth(email, hasProblem, listener);
    }

    public LiveData<Boolean> finalizeOnboarding(String email, String symptomsList) {
        return repository.finalizeOnboarding(email, symptomsList);
    }

    public void updateLifestyleData(String email, boolean isPregnant, boolean tryingToConceive, int sleepHours, int exerciseFrequency, AuthRepository.OnDataSavedListener listener) {
        repository.updateLifestyleData(email, isPregnant, tryingToConceive, sleepHours, exerciseFrequency, listener);
    }
}
