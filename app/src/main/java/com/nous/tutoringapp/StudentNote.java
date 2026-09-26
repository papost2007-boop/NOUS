package com.nous.tutoringapp;

public class StudentNote {
    private int id;
    private int studentId;
    private String date;
    private String category;
    private String noteText;

    public StudentNote(int id, int studentId, String date, String category, String noteText) {
        this.id = id;
        this.studentId = studentId;
        this.date = date;
        this.category = category;
        this.noteText = noteText;
    }

    public int getId() { return id; }
    public int getStudentId() { return studentId; }
    public String getDate() { return date; }
    public String getCategory() { return category; }
    public String getNoteText() { return noteText; }
}