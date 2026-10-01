package com.example.mobileappdev.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class UnassignedStudentsViewModel extends ViewModel {

    private final MutableLiveData<Integer> totalUnassigned =
            new MutableLiveData<>(0);

    public void setTotalUnassigned(int value) {
        totalUnassigned.setValue(value);
    }

    public LiveData<Integer> getTotalUnassigned() {
        return totalUnassigned;
    }
}