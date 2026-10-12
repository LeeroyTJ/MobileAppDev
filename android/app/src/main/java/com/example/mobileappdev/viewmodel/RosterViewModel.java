package com.example.mobileappdev.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.mobileappdev.data.local.entity.StudentEntity;

import java.util.List;

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

    private final MutableLiveData<List<StudentEntity>> students =
            new MutableLiveData<>();

    public void setSearchQuery(String value) {
        searchQuery.setValue(value != null ? value : "");
    }

    public void setProgrammeFilter(String value) {
        programmeFilter.setValue(value != null ? value : "");
    }

    public void setGroupFilter(String value) {
        groupFilter.setValue(value != null ? value : "");
    }

    public void setStatusFilter(String value) {
        statusFilter.setValue(value != null ? value : "");
    }

    public void clearAllFilters() {
        searchQuery.setValue("");
        programmeFilter.setValue("");
        groupFilter.setValue("");
        statusFilter.setValue("");
    }

    public void setStudentsFound(int value) {
        studentsFound.setValue(value);
    }

    public void setStudents(List<StudentEntity> value) {
        students.setValue(value);
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

    public LiveData<List<StudentEntity>> getStudents() {
        return students;
    }
}
