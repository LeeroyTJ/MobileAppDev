package com.example.mobileappdev.login.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class LoginViewModel extends ViewModel {

    private final MutableLiveData<String> studentNumber =
            new MutableLiveData<>("");

    private final MutableLiveData<String> password =
            new MutableLiveData<>("");

    private final MutableLiveData<Boolean> isLoggingIn =
            new MutableLiveData<>(false);

    private final MutableLiveData<Boolean> loginSuccessful =
            new MutableLiveData<>(false);

    public void setStudentNumber(String value) {
        studentNumber.setValue(value);
    }

    public void setPassword(String value) {
        password.setValue(value);
    }

    public void setLoggingIn(boolean loggingIn) {
        isLoggingIn.setValue(loggingIn);
    }

    public void setLoginSuccessful(boolean successful) {
        loginSuccessful.setValue(successful);
    }

    public LiveData<String> getStudentNumber() {
        return studentNumber;
    }

    public LiveData<String> getPassword() {
        return password;
    }

    public LiveData<Boolean> getIsLoggingIn() {
        return isLoggingIn;
    }

    public LiveData<Boolean> getLoginSuccessful() {
        return loginSuccessful;
    }
}