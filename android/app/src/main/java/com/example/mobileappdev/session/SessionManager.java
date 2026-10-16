package com.example.mobileappdev.session;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

public class SessionManager {

    private static final String PREFS_NAME = "cohorthub_session";
    private static final String KEY_TOKEN = "access_token";
    private static final String KEY_ACCOUNT_ID = "account_id";
    private static final String KEY_ROLE = "role";
    private static final String KEY_LAST_SYNCED = "last_synced_timestamp";

    private static volatile SessionManager INSTANCE;

    private final SharedPreferences prefs;

    private SessionManager(Context context) {
        SharedPreferences tempPrefs = null;
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            tempPrefs = EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

        } catch (Exception e) {
            // Delete corrupted shared preferences file and retry creation
            try {
                context.deleteSharedPreferences(PREFS_NAME);
                MasterKey masterKey = new MasterKey.Builder(context)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build();

                tempPrefs = EncryptedSharedPreferences.create(
                        context,
                        PREFS_NAME,
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                );
            } catch (Exception ex) {
                // Fallback to standard SharedPreferences to guarantee app never crashes
                tempPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            }
        }
        prefs = tempPrefs;
    }

    public static SessionManager getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (SessionManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SessionManager(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    public void saveSession(String token, long accountId, String role) {
        prefs.edit()
                .putString(KEY_TOKEN, token)
                .putLong(KEY_ACCOUNT_ID, accountId)
                .putString(KEY_ROLE, role)
                .apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public long getAccountId() {
        return prefs.getLong(KEY_ACCOUNT_ID, -1);
    }

    public String getRole() {
        return prefs.getString(KEY_ROLE, null);
    }

    public boolean isLoggedIn() {
        return getToken() != null;
    }

    public boolean isLecturer() {
        return "LECTURER".equals(getRole());
    }

    public void saveLastSyncedTime(long timestamp) {
        prefs.edit().putLong(KEY_LAST_SYNCED, timestamp).apply();
    }

    public long getLastSyncedTime() {
        return prefs.getLong(KEY_LAST_SYNCED, 0L);
    }

    public void clearSession() {
        prefs.edit().clear().apply();
    }
}
