package com.example.mobileappdev.model;

import java.util.Map;

public class Student {
    private final long id;
    private final String studentNumber;
    private final String name;
    private final String programmeName;
    private final String groupCode;
    private final int version;

    public Student(long id, String studentNumber, String name, String programmeName, String groupCode, int version) {
        this.id = id;
        this.studentNumber = studentNumber;
        this.name = name;
        this.programmeName = programmeName;
        this.groupCode = groupCode;
        this.version = version;
    }

    public static Student fromMap(Map<?, ?> map) {
        long id = map.get("student_id") instanceof Number ? ((Number) map.get("student_id")).longValue() : 0;
        String number = map.get("student_number") != null ? map.get("student_number").toString() : "";
        String name = map.get("student_name") != null ? map.get("student_name").toString() : "";
        String programme = map.get("programme_name") != null ? map.get("programme_name").toString() :
                (map.get("programme_code") != null ? map.get("programme_code").toString() : "");
        String group = map.get("group_code") != null ? map.get("group_code").toString() : "Unassigned";
        int version = map.get("version") instanceof Number ? ((Number) map.get("version")).intValue() : 1;
        return new Student(id, number, name, programme, group, version);
    }

    public long getId() { return id; }
    public String getStudentNumber() { return studentNumber; }
    public String getName() { return name; }
    public String getProgrammeName() { return programmeName; }
    public String getGroupCode() { return groupCode; }
    public int getVersion() { return version; }

    public String getInitials() {
        if (name == null || name.trim().isEmpty() || "null".equalsIgnoreCase(name.trim())) return "?";
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) sb.append(Character.toUpperCase(part.charAt(0)));
            if (sb.length() >= 2) break;
        }
        return sb.toString();
    }
}
