package com.example.mobileappdev.viewmodel;

public class ProfileViewModel {
    private String studentName = "";
    private String studentEmail = "";
    private String studentId = "";

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public String getStudentId() {
        return studentId;
    }
}
