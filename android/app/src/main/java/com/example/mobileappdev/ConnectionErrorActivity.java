package com.example.mobileappdev;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ConnectionErrorActivity extends AppCompatActivity {

    private Button btnRetry;
    private Button btnOfflineMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection_error);

        bindViews();
        setupListeners();
    }

    private void bindViews() {
        btnRetry = findViewById(R.id.btnRetry);
        btnOfflineMode = findViewById(R.id.btnOfflineMode);
    }

    private void setupListeners() {
        // Retry connection action
        btnRetry.setOnClickListener(v -> {
            Toast.makeText(this, "Retrying connection to server...", Toast.LENGTH_SHORT).show();
            // Perform connection check logic here
        });

        // Continue in offline mode action
        btnOfflineMode.setOnClickListener(v -> {
            Toast.makeText(this, "Switching to offline mode", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ConnectionErrorActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
