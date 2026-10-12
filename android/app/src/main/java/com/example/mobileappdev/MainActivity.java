package com.example.mobileappdev;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mobileappdev.session.SessionManager;

/**
 * Invisible router Activity.
 * Shows no UI. Reads the active session on onCreate and forwards immediately:
 *   - Not logged in            -> SignInActivity
 *   - Logged in as LECTURER    -> RosterActivity (Lecturer Home)
 *   - Logged in as STUDENT     -> ProfileActivity (Student Home)
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SessionManager sessionManager = SessionManager.getInstance(this);
        Class<?> targetActivity = !sessionManager.isLoggedIn() ? SignInActivity.class
                : sessionManager.isLecturer()  ? RosterActivity.class
                : ProfileActivity.class;

        Intent intent = new Intent(this, targetActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
