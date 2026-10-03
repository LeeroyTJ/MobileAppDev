package com.example.mobileappdev.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SyncViewModel extends ViewModel {

    private final MutableLiveData<Boolean> isSyncing =
            new MutableLiveData<>(false);

    private final MutableLiveData<Boolean> syncSuccessful =
            new MutableLiveData<>(false);

    private final MutableLiveData<String> syncMessage =
            new MutableLiveData<>("");

    public void setSyncing(boolean syncing) {
        isSyncing.setValue(syncing);
    }

    public void setSyncSuccessful(boolean successful) {
        syncSuccessful.setValue(successful);
    }

    public void setSyncMessage(String message) {
        syncMessage.setValue(message);
    }

    public LiveData<Boolean> getIsSyncing() {
        return isSyncing;
    }

    public LiveData<Boolean> getSyncSuccessful() {
        return syncSuccessful;
    }

    public LiveData<String> getSyncMessage() {
        return syncMessage;
    }
}