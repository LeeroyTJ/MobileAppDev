package com.example.mobileappdev.data.remote;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface GroupApiService {

    @POST("api/v1/groups/{id}/assign")
    Call<Map<String, Object>> assignStudent(@Path("id") long groupId, @Body Map<String, Object> body);

    @POST("api/v1/groups/{id}/transfer")
    Call<Map<String, Object>> transferStudent(@Path("id") long groupId, @Body Map<String, Object> body);
}
