package com.example.mobileappdev;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class SyncActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private Button btnRetryAll;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync);

        bindViews();
        setupListeners();
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btnBack);
        btnRetryAll = findViewById(R.id.btnRetryAll);
        bottomNav = findViewById(R.id.bottomNav);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnRetryAll.setOnClickListener(v -> {
            Toast.makeText(this, "Retrying all pending sync operations...", Toast.LENGTH_SHORT).show();
        });

        bottomNav.setSelectedItemId(R.id.navSync);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navHome) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (id == R.id.navGroup) {
                // Group action
                return true;
            } else if (id == R.id.navSync) {
                return true;
            } else if (id == R.id.navProfile) {
                startActivity(new Intent(this, ProfileActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }
}
