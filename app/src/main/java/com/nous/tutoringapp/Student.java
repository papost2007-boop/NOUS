package com.nous.tutoringapp;

public class Student {
    private int id;
    private String firstName;
    private String lastName;
    private double fee;
    private String grade;
    private String materialStatus;
    private String direction;
    private double materialCost; // 🎯 Συνολικό κόστος υλικού

    // Constructor με ID (όταν διαβάζουμε από τη βάση δεδομένων)
    public Student(int id, String firstName, String lastName, double fee, String grade, String materialStatus, String direction, double materialCost) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.fee = fee;
        this.grade = grade;
        this.materialStatus = materialStatus;
        this.direction = direction;
        this.materialCost = materialCost;
    }

    // Παλιός constructor (για συμβατότητα, θέτει default 0.0)
    public Student(int id, String firstName, String lastName, double fee, String grade, String materialStatus, String direction) {
        this(id, firstName, lastName, fee, grade, materialStatus, direction, 0.0);
    }

    // Constructor χωρίς ID (όταν δημιουργούμε νέο μαθητή)
    public Student(String firstName, String lastName, double fee, String grade, String materialStatus, String direction, double materialCost) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.fee = fee;
        this.grade = grade;
        this.materialStatus = materialStatus;
        this.direction = direction;
        this.materialCost = materialCost;
    }

    public Student(String firstName, String lastName, double fee, String grade, String materialStatus, String direction) {
        this(firstName, lastName, fee, grade, materialStatus, direction, 0.0);
    }

    // Getters & Setters
    public int getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public double getFee() { return fee; }
    public String getGrade() { return grade; }
    public String getMaterialStatus() { return materialStatus; }
    public void setMaterialStatus(String materialStatus) { this.materialStatus = materialStatus; }
    public String getDirection() { return direction != null ? direction : ""; }
    public void setDirection(String direction) { this.direction = direction; }

    public double getMaterialCost() { return materialCost; }
    public void setMaterialCost(double materialCost) { this.materialCost = materialCost; }

    public String getName() {
        return lastName + " " + firstName;
    }
}