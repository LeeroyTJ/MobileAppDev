package com.example.mobileappdev.data.remote;

import android.content.Context;

import com.example.mobileappdev.session.SessionManager;
import com.example.mobileappdev.sync.SyncApiService;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static final String BASE_URL = "http://10.0.2.2:3000/";

    private static Retrofit retrofit = null;

    public static Retrofit getClient(Context context) {
        if (retrofit == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new AuthInterceptor(SessionManager.getInstance(context)))
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static AuthApiService getAuthService(Context context) {
        return getClient(context).create(AuthApiService.class);
    }

    public static StudentApiService getStudentService(Context context) {
        return getClient(context).create(StudentApiService.class);
    }

    public static GroupApiService getGroupService(Context context) {
        return getClient(context).create(GroupApiService.class);
    }

    public static SyncApiService getSyncService(Context context) {
        return getClient(context).create(SyncApiService.class);
    }
}
