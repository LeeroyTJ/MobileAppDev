package com.example.mobileappdev.data.remote;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
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

    @GET("api/v1/students/{id}")
    Call<Map<String, Object>> getStudentById(@Path("id") long id);

    @POST("api/v1/students")
    Call<Map<String, Object>> createStudent(@Body Map<String, Object> body);

    @PUT("api/v1/students/{id}")
    Call<Map<String, Object>> updateStudent(@Path("id") long id, @Body Map<String, Object> body);

    @PATCH("api/v1/students/{id}/student-number")
    Call<Map<String, Object>> patchStudentNumber(@Path("id") long id, @Body Map<String, Object> body);

    @DELETE("api/v1/students/{id}")
    Call<Map<String, Object>> deleteStudent(@Path("id") long id);

    @POST("api/v1/students/{id}/group-requests")
    Call<Map<String, Object>> requestGroupChange(@Path("id") long id, @Body Map<String, Object> body);
}
