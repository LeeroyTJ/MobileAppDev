package com.example.mobileappdev.data.remote;

import android.content.Context;

import com.example.mobileappdev.session.SessionManager;
import com.example.mobileappdev.sync.SyncApiService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    // 10.0.2.2 is the Android emulator's alias for the host machine's
    // localhost. Change this if testing on a physical device or once
    // you have a real deployed server address.
    //
    // Backend dev machine on the LAN (physical-device testing):
    //   http://192.168.137.1:3000/
    private static final String BASE_URL = "http://10.0.2.2:3000/";

    private static volatile Retrofit retrofit;

    public static Retrofit getInstance(Context context) {
        if (retrofit == null) {
            synchronized (ApiClient.class) {
                if (retrofit == null) {
                    SessionManager sessionManager = SessionManager.getInstance(context);

                    OkHttpClient httpClient = new OkHttpClient.Builder()
                            .addInterceptor(new AuthInterceptor(sessionManager))
                            .build();

                    Gson gson = new GsonBuilder().create();

                    retrofit = new Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .client(httpClient)
                            .addConverterFactory(GsonConverterFactory.create(gson))
                            .build();
                }
            }
        }
        return retrofit;
    }

    public static AuthApiService getAuthService(Context context) {
        return getInstance(context).create(AuthApiService.class);
    }

    public static StudentApiService getStudentService(Context context) {
        return getInstance(context).create(StudentApiService.class);
    }

    public static GroupApiService getGroupService(Context context) {
        return getInstance(context).create(GroupApiService.class);
    }

    public static SyncApiService getSyncService(Context context) {
        return getInstance(context).create(SyncApiService.class);
    }
}