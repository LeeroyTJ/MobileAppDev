package com.example.mobileappdev.sync;

import androidx.annotation.Keep;

import com.example.mobileappdev.data.remote.SyncApiModels.SyncRequest;
import com.example.mobileappdev.data.remote.SyncApiModels.SyncResponseEnvelope;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

@Keep
public interface SyncApiService {

    @POST("/api/v1/sync")
    Call<SyncResponseEnvelope> pushOperations(@Body SyncRequest request);
}
