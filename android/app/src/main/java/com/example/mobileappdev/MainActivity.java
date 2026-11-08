package com.example.mobileappdev;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mobileappdev.session.SessionManager;
import com.example.mobileappdev.sync.SyncWorker;

/**
 * Invisible router Activity + Splash Screen.
 * Shows the animated logo, then routes to the correct screen based on the session.
 */
public class MainActivity extends AppCompatActivity {

    // Makes sure the network callback is only registered once per app process
    private static boolean networkCallbackRegistered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // We'll create this layout next

        // Schedule periodic background sync worker
        SyncWorker.schedulePeriodic(getApplicationContext());

        // Find the logo and animate it
        ImageView logo = findViewById(R.id.splashLogo);
        if (logo != null) {
            // You can create a custom XML animation for this, or just use the default fade_in
            logo.startAnimation(AnimationUtils.loadAnimation(this, R.anim.splash_anim));
        }

        // M9: trigger a sync whenever the network comes back
        registerAutoSync();

        // Delay the routing by 1.5 seconds (1500 ms) so the user sees the animation
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            SessionManager sessionManager = SessionManager.getInstance(this);
            Class<?> targetActivity = !sessionManager.isLoggedIn() ? SignInActivity.class
                    : sessionManager.isLecturer() ? RosterActivity.class
                      : ProfileActivity.class;

            Intent intent = new Intent(this, targetActivity);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

            // Fade out the splash screen smoothly
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            finish();
        }, 1500); // 1.5 seconds delay
    }

    private void registerAutoSync() {
        if (networkCallbackRegistered) return;

        final Context appContext = getApplicationContext();
        ConnectivityManager cm =
                (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;

        cm.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                if (SessionManager.getInstance(appContext).isLoggedIn()) {
                    SyncWorker.triggerManualSync(appContext);
                }
            }
        });
        networkCallbackRegistered = true;
    }
}
