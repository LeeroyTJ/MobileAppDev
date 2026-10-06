package com.example.mobileappdev.data.remote;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface StudentApiService {

    @GET("api/v1/students")
    Call<Map<String, Object>> getRoster(
            @Query("programme") String programme,
            @Query("group") String group,
            @Query("search") String search
    );

    @GET("api/v1/students/me/profile")
    Call<Map<String, Object>> getMyProfile();

    @PUT("api/v1/students/{id}")
    Call<Map<String, Object>> updateStudent(@Path("id") long id, @Body Map<String, Object> body);

    @DELETE("api/v1/students/{id}")
    Call<Map<String, Object>> deleteStudent(@Path("id") long id);
}