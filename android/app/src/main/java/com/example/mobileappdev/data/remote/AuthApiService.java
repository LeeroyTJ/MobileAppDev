package com.example.mobileappdev.data.remote;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthApiService {

    @POST("api/v1/auth/register")
    Call<Map<String, Object>> register(@Body Map<String, Object> body);

    @POST("api/v1/auth/login")
    Call<Map<String, Object>> login(@Body Map<String, Object> body);
}