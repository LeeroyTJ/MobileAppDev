package com.example.mobileappdev;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    Button editStudentButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        editStudentButton = findViewById(R.id.editStudentButton);

        editStudentButton.setOnClickListener(v -> {

            Intent intent =
                    new Intent(MainActivity.this,
                            EditStudentActivity.class);

            startActivity(intent);
        });
    }
}