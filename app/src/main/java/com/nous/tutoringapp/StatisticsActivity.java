package com.nous.tutoringapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Date;

public class StatisticsActivity extends AppCompatActivity {

    private Spinner spinnerMonth;
    private TextView tvStudentCount, tvExpectedRevenue, tvTotalCollected;
    private TextView tvCash, tvPos, tvIris, tvAlpha, tvEurobank, tvPiraeus;
    private Button btnBack;
    private Button btnExportStudents, btnExportPayments; // 🎯 Κουμπιά Εξαγωγής

    private DatabaseHelper dbHelper;
    private ArrayList<String> monthQueryValues = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        dbHelper = new DatabaseHelper(this);

        tvStudentCount = findViewById(R.id.tvStatsStudentCount);
        tvExpectedRevenue = findViewById(R.id.tvStatsExpectedRevenue);
        spinnerMonth = findViewById(R.id.spinnerStatsMonth);
        tvTotalCollected = findViewById(R.id.tvTotalCollected);
        tvCash = findViewById(R.id.tvStatsCash);
        tvPos = findViewById(R.id.tvStatsPos);
        tvIris = findViewById(R.id.tvStatsIris);
        tvAlpha = findViewById(R.id.tvStatsAlpha);
        tvEurobank = findViewById(R.id.tvStatsEurobank);
        tvPiraeus = findViewById(R.id.tvStatsPiraeus);
        btnBack = findViewById(R.id.btnStatsBack);

        // 🎯 Σύνδεση Κουμπιών Εξαγωγής
        btnExportStudents = findViewById(R.id.btnExportStudents);
        btnExportPayments = findViewById(R.id.btnExportPayments);

        if (btnExportStudents != null) {
            btnExportStudents.setOnClickListener(v -> exportStudentsToCSV());
        }

        if (btnExportPayments != null) {
            btnExportPayments.setOnClickListener(v -> exportPaymentsToCSV());
        }

        // 🎯 Φόρτωση Γενικών Στατιστικών (Μαθητές & Αναμενόμενα)
        loadGeneralStats();

        setupMonthSpinner();

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    /**
     * 👥 & 📈 Φορτώνει τον συνολικό αριθμό μαθητών και τα αναμενόμενα μηνιαία έσοδα.
     */
    private void loadGeneralStats() {
        int studentCount = dbHelper.getStudentsCount();
        double expectedRevenue = dbHelper.getExpectedMonthlyRevenue();

        tvStudentCount.setText(String.valueOf(studentCount));
        tvExpectedRevenue.setText(String.format(Locale.getDefault(), "%.2f €", expectedRevenue));
    }

    /**
     * 📅 Γεμίζει το Spinner ΜΟΝΟ με τους μήνες που έχουν πραγματικές πληρωμές στη βάση!
     */
    private void setupMonthSpinner() {
        ArrayList<String> monthDisplayNames = new ArrayList<>();
        monthQueryValues.clear();

        ArrayList<String> dbMonths = dbHelper.getMonthsWithPayments();

        if (dbMonths.isEmpty()) {
            String currentMonth = new SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(new Date());
            dbMonths.add(currentMonth);
        }

        SimpleDateFormat queryFormat = new SimpleDateFormat("MM/yyyy", Locale.getDefault());
        SimpleDateFormat displayFormat = new SimpleDateFormat("MMMM yyyy", new Locale("el", "GR"));

        for (String monthYear : dbMonths) {
            try {
                Date date = queryFormat.parse(monthYear);
                if (date != null) {
                    String display = displayFormat.format(date);
                    display = display.substring(0, 1).toUpperCase() + display.substring(1);

                    monthDisplayNames.add(display);
                    monthQueryValues.add(monthYear);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, monthDisplayNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMonth.setAdapter(adapter);

        spinnerMonth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedMonthYear = monthQueryValues.get(position);
                loadStatisticsForMonth(selectedMonthYear);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    /**
     * 📊 Φορτώνει τα νούμερα εισπράξεων για τον επιλεγμένο μήνα.
     */
    private void loadStatisticsForMonth(String monthYear) {
        double cash = dbHelper.getCollectedRevenueByMethod("Μετρητά", monthYear);
        double pos = dbHelper.getCollectedRevenueByMethod("POS", monthYear);
        double iris = dbHelper.getCollectedRevenueByMethod("IRIS", monthYear);
        double alpha = dbHelper.getCollectedRevenueByMethod("Alpha Bank", monthYear);
        double eurobank = dbHelper.getCollectedRevenueByMethod("Eurobank", monthYear);
        double piraeus = dbHelper.getCollectedRevenueByMethod("Τράπεζα Πειραιώς", monthYear);

        double total = cash + pos + iris + alpha + eurobank + piraeus;

        tvTotalCollected.setText(String.format(Locale.getDefault(), "%.2f €", total));
        tvCash.setText(String.format(Locale.getDefault(), "%.2f €", cash));
        tvPos.setText(String.format(Locale.getDefault(), "%.2f €", pos));
        tvIris.setText(String.format(Locale.getDefault(), "%.2f €", iris));
        tvAlpha.setText(String.format(Locale.getDefault(), "%.2f €", alpha));
        tvEurobank.setText(String.format(Locale.getDefault(), "%.2f €", eurobank));
        tvPiraeus.setText(String.format(Locale.getDefault(), "%.2f €", piraeus));
    }

    // 📄 ΕΞΑΓΩΓΗ ΜΑΘΗΤΟΛΟΓΙΟΥ (SAFE VERSION)
    private void exportStudentsToCSV() {
        try {
            ArrayList<String[]> students = dbHelper.getAllStudentsForExport();
            if (students == null || students.isEmpty()) {
                Toast.makeText(this, "⚠️ Δεν υπάρχουν μαθητές για εξαγωγή", Toast.LENGTH_SHORT).show();
                return;
            }

            StringBuilder csvData = new StringBuilder();
            csvData.append("\uFEFF"); // BOM για ελληνικά στο Excel
            csvData.append("Επώνυμο;Όνομα;Τάξη;Μηνιαία Δίδακτρα (€);Τηλέφωνο Μαθητή;Τηλέφωνο Γονέα;Σημειώσεις\n");

            for (String[] s : students) {
                String lastName = (s != null && s.length > 0 && s[0] != null) ? s[0] : "";
                String firstName = (s != null && s.length > 1 && s[1] != null) ? s[1] : "";
                String grade = (s != null && s.length > 2 && s[2] != null) ? s[2] : "";
                String fee = (s != null && s.length > 3 && s[3] != null) ? s[3] : "0";
                String phone = (s != null && s.length > 4 && s[4] != null) ? s[4] : "";
                String parentPhone = (s != null && s.length > 5 && s[5] != null) ? s[5] : "";
                String notes = (s != null && s.length > 6 && s[6] != null) ? s[6].replace("\n", " ").replace(";", ",") : "";

                csvData.append(lastName).append(";")
                        .append(firstName).append(";")
                        .append(grade).append(";")
                        .append(fee).append(";")
                        .append(phone).append(";")
                        .append(parentPhone).append(";")
                        .append(notes).append("\n");
            }

            shareCSVFile("Mathitologio_NOUS.csv", csvData.toString());
        } catch (Exception e) {
            Toast.makeText(this, "❌ Σφάλμα εξαγωγής: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // 💰 ΕΞΑΓΩΓΗ ΠΛΗΡΩΜΩΝ (SAFE VERSION)
    private void exportPaymentsToCSV() {
        try {
            ArrayList<String[]> payments = dbHelper.getAllPaymentsForExport();
            if (payments == null || payments.isEmpty()) {
                Toast.makeText(this, "⚠️ Δεν υπάρχουν πληρωμές για εξαγωγή", Toast.LENGTH_SHORT).show();
                return;
            }

            StringBuilder csvData = new StringBuilder();
            csvData.append("\uFEFF"); // BOM για ελληνικά στο Excel
            csvData.append("Ημερομηνία;Επώνυμο;Όνομα;Τάξη;Ποσό (€);Μήνας;Τρόπος Πληρωμής;Σχόλια\n");

            for (String[] p : payments) {
                String date = (p != null && p.length > 0 && p[0] != null) ? p[0] : "";
                String lastName = (p != null && p.length > 1 && p[1] != null) ? p[1] : "";
                String firstName = (p != null && p.length > 2 && p[2] != null) ? p[2] : "";
                String grade = (p != null && p.length > 3 && p[3] != null) ? p[3] : "";
                String amount = (p != null && p.length > 4 && p[4] != null) ? p[4] : "0";
                String month = (p != null && p.length > 5 && p[5] != null) ? p[5] : "";
                String method = (p != null && p.length > 6 && p[6] != null) ? p[6] : "Μετρητά";
                String comments = (p != null && p.length > 7 && p[7] != null) ? p[7].replace("\n", " ").replace(";", ",") : "";

                csvData.append(date).append(";")
                        .append(lastName).append(";")
                        .append(firstName).append(";")
                        .append(grade).append(";")
                        .append(amount).append(";")
                        .append(month).append(";")
                        .append(method).append(";")
                        .append(comments).append("\n");
            }

            shareCSVFile("Pliromes_NOUS.csv", csvData.toString());
        } catch (Exception e) {
            Toast.makeText(this, "❌ Σφάλμα εξαγωγής: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // 📲 ΚΟΙΝΟΠΟΙΗΣΗ ΑΡΧΕΙΟΥ (SHARE SHEET)
    private void shareCSVFile(String fileName, String data) {
        try {
            java.io.File cachePath = new java.io.File(getCacheDir(), "exports");
            if (!cachePath.exists()) {
                cachePath.mkdirs();
            }
            java.io.File file = new java.io.File(cachePath, fileName);
            java.io.FileOutputStream stream = new java.io.FileOutputStream(file);
            stream.write(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            stream.close();

            android.net.Uri contentUri = androidx.core.content.FileProvider.getUriForFile(
                    this,
                    getApplicationContext().getPackageName() + ".provider",
                    file
            );

            if (contentUri != null) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/csv");
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(shareIntent, "Αποστολή αρχείου Excel/CSV μέσω:"));
            }
        } catch (Exception e) {
            Toast.makeText(this, "❌ Σφάλμα κοινοποίησης: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}