package com.nous.tutoringapp;

/**
 * Κλάση μοντέλου που αναπαριστά μια πληρωμή στη βάση δεδομένων.
 */
public class Payment {
    private int id;
    private int studentId;
    private double amount;
    private String paymentDate;
    private String comments;
    private String paymentMethod; // 🎯 Νέο πεδίο για τον τρόπο πληρωμής (POS, Μετρητά, Τράπεζα)

    public Payment(int id, int studentId, double amount, String paymentDate, String comments, String paymentMethod) {
        this.id = id;
        this.studentId = studentId;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.comments = comments;
        this.paymentMethod = paymentMethod;
    }

    public int getId() { return id; }
    public int getStudentId() { return studentId; }
    public double getAmount() { return amount; }
    public String getPaymentDate() { return paymentDate; }
    public String getComments() { return comments; }

    public String getPaymentMethod() { return paymentMethod; } // 🎯 Getter για τον τρόπο πληρωμής
    // 🎯 ΝΕΑ ΠΕΔΙΑ για τις ανάγκες της διεπαφής (UI)
    private String studentName;
    private String targetMonth;

    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getStudentName() { return studentName; }

    public void setTargetMonth(String targetMonth) { this.targetMonth = targetMonth; }
    public String getTargetMonth() { return targetMonth; }
    // Κενός κατασκευαστής (ΑΠΑΡΑΙΤΗΤΟΣ)
    public Payment() {
    }

    // Setters για να σταματήσει να χτυπάει
    public void setId(int id) { this.id = id; }

    public void setAmount(double amount) { this.amount = amount; }
    public void setPaymentDate(String paymentDate) { this.paymentDate = paymentDate; }
    public void setComments(String comments) { this.comments = comments; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

}
