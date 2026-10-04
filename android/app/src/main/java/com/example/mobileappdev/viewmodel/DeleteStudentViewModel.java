package com.example.mobileappdev.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class DeleteStudentViewModel extends ViewModel {

    private final MutableLiveData<String> studentName =
            new MutableLiveData<>("");

    private final MutableLiveData<String> studentNumber =
            new MutableLiveData<>("");

    private final MutableLiveData<Boolean> isDeleting =
            new MutableLiveData<>(false);

    private final MutableLiveData<Boolean> deleteConfirmed =
            new MutableLiveData<>(false);

    public void setStudentName(String value) {
        studentName.setValue(value);
    }

    public void setStudentNumber(String value) {
        studentNumber.setValue(value);
    }

    public void setDeleting(boolean deleting) {
        isDeleting.setValue(deleting);
    }

    public void setDeleteConfirmed(boolean confirmed) {
        deleteConfirmed.setValue(confirmed);
    }

    public LiveData<String> getStudentName() {
        return studentName;
    }

    public LiveData<String> getStudentNumber() {
        return studentNumber;
    }

    public LiveData<Boolean> getIsDeleting() {
        return isDeleting;
    }

    public LiveData<Boolean> getDeleteConfirmed() {
        return deleteConfirmed;
    }
}