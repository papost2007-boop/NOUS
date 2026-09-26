package com.nous.tutoringapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Η ενδιάμεση οθόνη επιλογής τάξης (Επιλογή Α).
 * Περιλαμβάνει και την έξυπνη μπάρα αναζήτησης μαθητή!
 */
public class GradeSelectionActivity extends AppCompatActivity {

    private Button btnA_Gym, btnB_Gym, btnC_Gym;
    private Button btnA_Lyk, btnB_Lyk, btnC_Lyk;
    private Button btnAll,btnBack;

    // 🎯 Μεταβλητή για να κρατάμε τη λειτουργία (προβολή ή επεξεργασία)
    private String mode = "view";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grade_selection);

        // 🎯 Λήψη της λειτουργίας (mode) από τη MainActivity
        if (getIntent().hasExtra("mode")) {
            mode = getIntent().getStringExtra("mode");
        }

        // Σύνδεση των Buttons με τα IDs του XML αρχείου
        btnA_Gym = findViewById(R.id.btnGradeA_Gym);
        btnB_Gym = findViewById(R.id.btnGradeB_Gym);
        btnC_Gym = findViewById(R.id.btnGradeC_Gym);

        btnA_Lyk = findViewById(R.id.btnGradeA_Lyk);
        btnB_Lyk = findViewById(R.id.btnGradeB_Lyk);
        btnC_Lyk = findViewById(R.id.btnGradeC_Lyk);

        btnAll = findViewById(R.id.btnGradeAll);
        btnBack = findViewById(R.id.btnGradeBack);
        EditText etSearchStudent = findViewById(R.id.etSearchStudent);

        if (etSearchStudent != null) {
            // 🎯 Ορισμός κουμπιού Enter στο πληκτρολόγιο σε "Αναζήτηση"
            etSearchStudent.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);

            // 1. Παρακολούθηση κειμένου για εμφάνιση/απόκρυψη του ❌
            etSearchStudent.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() > 0) {
                        // Εμφάνιση του ❌ δεξιά
                        etSearchStudent.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_clear, 0);
                    } else {
                        // Απόκρυψη του ❌ όταν είναι κενό
                        etSearchStudent.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                    }
                }

                @Override
                public void afterTextChanged(android.text.Editable s) {}
            });

            // 2. Click Listener για το ❌ (Touch στο δεξί μέρος του EditText)
            etSearchStudent.setOnTouchListener((v, event) -> {
                if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                    // Ελέγχουμε αν το πάτημα έγινε στην περιοχή του δεξιού εικονιδίου
                    if (etSearchStudent.getCompoundDrawables()[2] != null) {
                        if (event.getRawX() >= (etSearchStudent.getRight() - etSearchStudent.getCompoundDrawables()[2].getBounds().width() - etSearchStudent.getPaddingEnd())) {
                            etSearchStudent.setText(""); // Καθαρισμός κειμένου!
                            return true;
                        }
                    }
                }
                return false;
            });

            // 3. Εκτέλεση αναζήτησης με το κουμπί Enter/Search του πληκτρολογίου
            etSearchStudent.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
                        (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER)) {

                    String query = etSearchStudent.getText().toString().trim();
                    if (!query.isEmpty()) {
                        Intent intent = new Intent(GradeSelectionActivity.this, StudentListActivity.class);
                        intent.putExtra("selected_grade", "Όλοι");
                        intent.putExtra("search_query", query);
                        intent.putExtra("mode", mode);
                        startActivity(intent);
                    }
                    return true;
                }
                return false;
            });
        }

        // Ορισμός ενεργειών για τα κουμπιά Γυμνασίου & Α' Λυκείου
        btnA_Gym.setOnClickListener(v -> openStudentListWithFilter("Α' Γυμνασίου"));
        btnB_Gym.setOnClickListener(v -> openStudentListWithFilter("Β' Γυμνασίου"));
        btnC_Gym.setOnClickListener(v -> openStudentListWithFilter("Γ' Γυμνασίου"));
        btnA_Lyk.setOnClickListener(v -> openStudentListWithFilter("Α' Λυκείου"));

        // 🎯 Β' ΛΥΚΕΙΟΥ: Ανοίγει τη νέα οθόνη Επιλογής Κατεύθυνσης!
        btnB_Lyk.setOnClickListener(v -> {
            Intent intent = new Intent(GradeSelectionActivity.this, SelectDirectionActivity.class);
            intent.putExtra("selected_grade", "Β' Λυκείου");
            intent.putExtra("mode", mode);
            startActivity(intent);
        });

        // 🎯 Γ' ΛΥΚΕΙΟΥ: Ανοίγει τη νέα οθόνη Επιλογής Κατεύθυνσης!
        btnC_Lyk.setOnClickListener(v -> {
            Intent intent = new Intent(GradeSelectionActivity.this, SelectDirectionActivity.class);
            intent.putExtra("selected_grade", "Γ' Λυκείου");
            intent.putExtra("mode", mode);
            startActivity(intent);
        });

        btnAll.setOnClickListener(v -> openStudentListWithFilter("Όλοι"));
        btnBack.setOnClickListener(v -> finish());
    }
    private void openStudentListWithFilter(String gradeName) {
        Intent intent = new Intent(GradeSelectionActivity.this, StudentListActivity.class);
        intent.putExtra("selected_grade", gradeName);
        intent.putExtra("mode", mode);
        startActivity(intent);
    }
}