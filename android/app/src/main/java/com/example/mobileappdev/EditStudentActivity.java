package com.example.mobileappdev;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class EditStudentActivity extends AppCompatActivity {

    Spinner spinnerLabGroup;
    Spinner spinnerProgramme;

    LinearLayout changeGroup;
    LinearLayout correctStudentNumber;
    LinearLayout editDetails;

    Button deleteStudent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_edit_student);

        // Find views
        spinnerLabGroup = findViewById(R.id.spinnerLabGroup);
        spinnerProgramme = findViewById(R.id.spinnerProgramme);

        changeGroup = findViewById(R.id.changeGroup);
        correctStudentNumber = findViewById(R.id.correctStudentNumber);
        editDetails = findViewById(R.id.editDetails);

        deleteStudent = findViewById(R.id.deleteStudent);


        // LAB GROUP OPTIONS

        String[] groups = {
                "GO1 (10/15)",
                "GO2 (11/15)",
                "GO3 (8/15)",
                "GO4 (12/15)",
                "Unassigned"
        };

        ArrayAdapter<String> groupAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        groups
                );

        spinnerLabGroup.setAdapter(groupAdapter);

        spinnerLabGroup.setSelection(1);


        // PROGRAMME OPTIONS

        String[] programmes = {
                "Computer Science",
                "Information Technology",
                "Data Science",
                "Software Engineering"
        };

        ArrayAdapter<String> programmeAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        programmes
                );

        spinnerProgramme.setAdapter(programmeAdapter);

        spinnerProgramme.setSelection(0);


        // BACK BUTTON

        findViewById(R.id.btnBack).setOnClickListener(v -> {
            finish();
        });


        // CHANGE GROUP

        changeGroup.setOnClickListener(v -> {

            Toast.makeText(
                    EditStudentActivity.this,
                    "Change group selected",
                    Toast.LENGTH_SHORT
            ).show();

        });


        // CORRECT STUDENT NUMBER

        correctStudentNumber.setOnClickListener(v -> {

            Toast.makeText(
                    EditStudentActivity.this,
                    "Correct student number selected",
                    Toast.LENGTH_SHORT
            ).show();

        });


        // EDIT DETAILS

        editDetails.setOnClickListener(v -> {

            Toast.makeText(
                    EditStudentActivity.this,
                    "Edit details selected",
                    Toast.LENGTH_SHORT
            ).show();

        });


        // DELETE STUDENT

        deleteStudent.setOnClickListener(v -> {

            Toast.makeText(
                    EditStudentActivity.this,
                    "Student deleted",
                    Toast.LENGTH_SHORT
            ).show();

        });
    }
}