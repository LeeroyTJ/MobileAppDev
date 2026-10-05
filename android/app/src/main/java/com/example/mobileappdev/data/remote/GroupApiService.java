package com.example.mobileappdev.data.remote;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface GroupApiService {

    @GET("api/v1/groups")
    Call<Map<String, Object>> listGroups();

    @POST("api/v1/groups/{groupId}/assign")
    Call<Map<String, Object>> assignStudent(@Path("groupId") long groupId, @Body Map<String, Object> body);

    @POST("api/v1/groups/{groupId}/transfer")
    Call<Map<String, Object>> transferStudent(@Path("groupId") long groupId, @Body Map<String, Object> body);
}