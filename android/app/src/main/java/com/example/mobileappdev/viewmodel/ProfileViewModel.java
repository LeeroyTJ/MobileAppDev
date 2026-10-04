package com.example.mobileappdev.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ProfileViewModel extends ViewModel {

    private final MutableLiveData<String> studentName =
            new MutableLiveData<>("");

    private final MutableLiveData<String> studentNumber =
            new MutableLiveData<>("");

    private final MutableLiveData<String> programmeCode =
            new MutableLiveData<>("");

    private final MutableLiveData<String> labGroupCode =
            new MutableLiveData<>("");

    public void setStudentName(String value) {
        studentName.setValue(value);
    }

    public void setStudentNumber(String value) {
        studentNumber.setValue(value);
    }

    public void setProgrammeCode(String value) {
        programmeCode.setValue(value);
    }

    public void setLabGroupCode(String value) {
        labGroupCode.setValue(value);
    }

    public LiveData<String> getStudentName() {
        return studentName;
    }

    public LiveData<String> getStudentNumber() {
        return studentNumber;
    }

    public LiveData<String> getProgrammeCode() {
        return programmeCode;
    }

    public LiveData<String> getLabGroupCode() {
        return labGroupCode;
    }
}
