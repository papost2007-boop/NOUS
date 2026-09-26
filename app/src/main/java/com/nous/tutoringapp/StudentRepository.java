package com.nous.tutoringapp;

import java.util.ArrayList;

/**
 * Κοινή στατική αποθήκη (Repository) για τη διατήρηση των μαθητών στη μνήμη της συσκευής
 * όσο η εφαρμογή είναι ανοιχτή.
 */
public class StudentRepository {
    // Η κοινή λίστα μαθητών προσβάσιμη από όλες τις οθόνες
    public static ArrayList<Student> students = new ArrayList<>();

    // Στατικό block για την προετοιμασία μερικών εικονικών μαθητών κατά την εκκίνηση
    static {
        // 🎯 Προσθέσαμε την 6η παράμετρο (direction) στους μαθητές!
        students.add(new Student("Κώστας", "Αλεξίου", 120.0, "Γ' Λυκείου", "Πληρωμένο", "💼 Οικονομικά"));
        students.add(new Student("Μαρία", "Γεωργίου", 90.0, "Α' Λυκείου", "Μη πληρωμένο", ""));
        students.add(new Student("Δημήτρης", "Δημητρίου", 80.0, "Γ' Γυμνασίου", "Πληρωμένο", ""));
        students.add(new Student("Νίκος", "Παπαδάκης", 110.0, "Β' Λυκείου", "Μη πληρωμένο", ""));
    }
}