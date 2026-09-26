package com.nous.tutoringapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

/**
 * Η οθόνη Διαχείρισης & Ρυθμίσεων της εφαρμογής.
 */
public class SettingsActivity extends AppCompatActivity {

    private Button btnOpenBackup;
    private Button btnChangePassword;
    private Button btnPromoteStudents;
    private Button btnClearPayments;
    private Button btnSummerSettings;
    private Button btnBack;

    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        dbHelper = new DatabaseHelper(this);

        // Σύνδεση των Buttons με τα IDs του XML
        btnOpenBackup = findViewById(R.id.btnOpenBackup);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnPromoteStudents = findViewById(R.id.btnPromoteStudents);
        btnClearPayments = findViewById(R.id.btnClearPayments);
        btnSummerSettings = findViewById(R.id.btnSummerSettings);
        Button btnClearAllNotes = findViewById(R.id.btnClearAllNotes);
        btnBack = findViewById(R.id.btnSettingsBack);

        // 🛡️ 1. Άνοιγμα Οθόνης Backup
        if (btnOpenBackup != null) {
            btnOpenBackup.setOnClickListener(v -> {
                Intent intent = new Intent(SettingsActivity.this, BackupActivity.class);
                startActivity(intent);
            });
        }

        // 🔒 2. Αλλαγή Κωδικού
        if (btnChangePassword != null) {
            btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        }

        // ☀️ 3. Διαχείριση Θερινών Μαθημάτων
        if (btnSummerSettings != null) {
            btnSummerSettings.setOnClickListener(v -> showSummerGradesDialog());
        }

        // 🎓 4. Νέα Σχολική Χρονιά (Προαγωγή)
        if (btnPromoteStudents != null) {
            btnPromoteStudents.setOnClickListener(v -> confirmAndPromoteStudents());
        }

        // 🧹 5. Εκκαθάριση Πληρωμών
        if (btnClearPayments != null) {
            btnClearPayments.setOnClickListener(v -> confirmAndClearPayments());
        }

        // 🗑️ 6. Εκκαθάριση Σημειώσεων
        if (btnClearAllNotes != null) {
            btnClearAllNotes.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("⚠️ Διαγραφή Όλων των Σημειώσεων")
                        .setMessage("Είστε σίγουροι ότι θέλετε να διαγράψετε οριστικά ΟΛΕΣ τις σημειώσεις και το ιστορικό επικοινωνίας όλων των μαθητών;\n\nΗ ενέργεια αυτή δεν μπορεί να αναιρεθεί!")
                        .setPositiveButton("Ναι, Διαγραφή", (dialog, which) -> {
                            dbHelper.clearAllNotes();
                            Toast.makeText(this, "✅ Όλες οι σημειώσεις διαγράφηκαν επιτυχώς!", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Ακύρωση", null)
                        .show();
            });
        }

        // 🚪 7. Επιστροφή στο Μενού
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void showChangePasswordDialog() {
        if (!PasswordManager.isPasswordSet(this)) {
            Toast.makeText(this, "Δεν έχετε ορίσει ακόμα αρχικό κωδικό. Πατήστε στα Στατιστικά για να ορίσετε!", Toast.LENGTH_LONG).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🔑 Αλλαγή Κωδικού Ασφαλείας");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 10);

        final EditText inputOldPass = new EditText(this);
        inputOldPass.setHint("Παλιός Κωδικός");
        inputOldPass.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(inputOldPass);

        final EditText inputNewPass = new EditText(this);
        inputNewPass.setHint("Νέος Κωδικός");
        inputNewPass.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(inputNewPass);

        builder.setView(layout);

        builder.setPositiveButton("Ενημέρωση", (dialog, which) -> {
            String oldPass = inputOldPass.getText().toString().trim();
            String newPass = inputNewPass.getText().toString().trim();

            if (!PasswordManager.checkPassword(this, oldPass)) {
                Toast.makeText(this, "Ο παλιός κωδικός είναι λανθασμένος!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPass.isEmpty()) {
                Toast.makeText(this, "Ο νέος κωδικός δεν μπορεί να είναι κενός!", Toast.LENGTH_SHORT).show();
                return;
            }

            PasswordManager.setPassword(this, newPass);
            Toast.makeText(this, "Ο κωδικός άλλαξε επιτυχώς! 🔒", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Ακύρωση", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showSummerGradesDialog() {
        String[] grades = {"Α' Γυμνασίου", "Β' Γυμνασίου", "Γ' Γυμνασίου", "Α' Λυκείου", "Β' Λυκείου", "Γ' Λυκείου"};
        boolean[] checkedGrades = new boolean[grades.length];

        SharedPreferences prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);

        for (int i = 0; i < grades.length; i++) {
            checkedGrades[i] = prefs.getBoolean("summer_grade_" + grades[i], "Β' Λυκείου".equals(grades[i]));
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("☀️ Τάξεις με Θερινά (Ιούλιος)");

        builder.setMultiChoiceItems(grades, checkedGrades, (dialog, which, isChecked) -> {
            checkedGrades[which] = isChecked;
        });

        builder.setPositiveButton("Αποθήκευση", (dialog, which) -> {
            SharedPreferences.Editor editor = prefs.edit();
            for (int i = 0; i < grades.length; i++) {
                editor.putBoolean("summer_grade_" + grades[i], checkedGrades[i]);
            }
            editor.apply();
            Toast.makeText(this, "✅ Οι ρυθμίσεις θερινών αποθηκεύτηκαν!", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Ακύρωση", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(android.graphics.Color.parseColor("#F59E0B"));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(android.graphics.Color.parseColor("#94A3B8"));
    }

    private void confirmAndPromoteStudents() {
        new AlertDialog.Builder(this)
                .setTitle("🎓 Νέα Σχολική Χρονιά")
                .setMessage("Είστε σίγουροι ότι θέλετε να προάγετε όλους τους μαθητές στην επόμενη τάξη;\n\n• Οι μαθητές της Γ' Λυκείου θα ΔΙΑΓΡΑΦΟΥΝ.\n• Όλοι οι υπόλοιποι μαθητές θα ανέβουν μία τάξη.")
                .setPositiveButton("Ναι, Προαγωγή", (dialog, which) -> {
                    dbHelper.promoteAllStudents();
                    Toast.makeText(this, "🎓 Η προαγωγή των μαθητών ολοκληρώθηκε επιτυχώς!", Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Ακύρωση", null)
                .show();
    }

    private void confirmAndClearPayments() {
        new AlertDialog.Builder(this)
                .setTitle("🧹 Εκκαθάριση Πληρωμών")
                .setMessage("Είστε σίγουροι ότι θέλετε να διαγράψετε ΟΛΕΣ τις καταγεγραμμένες πληρωμές;\n\nΟι μαθητές θα παραμείνουν στη βάση δεδομένων, αλλά το ιστορικό πληρωμών θα μηδενιστεί.")
                .setPositiveButton("Ναι, Διαγραφή Πληρωμών", (dialog, which) -> {
                    dbHelper.clearAllPayments();
                    Toast.makeText(this, "🧹 Όλες οι πληρωμές διαγράφηκαν επιτυχώς!", Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Ακύρωση", null)
                .show();
    }

}