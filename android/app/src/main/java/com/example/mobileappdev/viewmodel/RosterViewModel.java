package com.example.mobileappdev.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class RosterViewModel extends ViewModel {

    private final MutableLiveData<String> searchQuery =
            new MutableLiveData<>("");

    private final MutableLiveData<String> programmeFilter =
            new MutableLiveData<>("");

    private final MutableLiveData<String> groupFilter =
            new MutableLiveData<>("");

    private final MutableLiveData<String> statusFilter =
            new MutableLiveData<>("");

    private final MutableLiveData<Integer> studentsFound =
            new MutableLiveData<>(0);

    public void setSearchQuery(String value) {
        searchQuery.setValue(value);
    }

    public void setProgrammeFilter(String value) {
        programmeFilter.setValue(value);
    }

    public void setGroupFilter(String value) {
        groupFilter.setValue(value);
    }

    public void setStatusFilter(String value) {
        statusFilter.setValue(value);
    }

    public void setStudentsFound(int value) {
        studentsFound.setValue(value);
    }

    public LiveData<String> getSearchQuery() {
        return searchQuery;
    }

    public LiveData<String> getProgrammeFilter() {
        return programmeFilter;
    }

    public LiveData<String> getGroupFilter() {
        return groupFilter;
    }

    public LiveData<String> getStatusFilter() {
        return statusFilter;
    }

    public LiveData<Integer> getStudentsFound() {
        return studentsFound;
    }
}