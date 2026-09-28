package com.example.mobileappdev.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class CapacityViewModel extends ViewModel {

    private final MutableLiveData<Integer> totalStudents =
            new MutableLiveData<>(0);

    private final MutableLiveData<Integer> remainingSpaces =
            new MutableLiveData<>(0);

    private final MutableLiveData<String> capacityStatus =
            new MutableLiveData<>("");

    public void setTotalStudents(int value) {
        totalStudents.setValue(value);
    }

    public void setRemainingSpaces(int value) {
        remainingSpaces.setValue(value);
    }

    public void setCapacityStatus(String value) {
        capacityStatus.setValue(value);
    }

    public LiveData<Integer> getTotalStudents() {
        return totalStudents;
    }

    public LiveData<Integer> getRemainingSpaces() {
        return remainingSpaces;
    }

    public LiveData<String> getCapacityStatus() {
        return capacityStatus;
    }
}