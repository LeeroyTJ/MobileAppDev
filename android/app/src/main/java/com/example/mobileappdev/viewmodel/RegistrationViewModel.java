package com.example.mobileappdev.viewmodel;

public class RegistrationViewModel {
    private String studentName = "";
    private String studentEmail = "";

    public void setStudentName(String studentName) {
        this.studentName = studentName;}

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getStudentEmail() {
        return studentEmail;
    }
}
