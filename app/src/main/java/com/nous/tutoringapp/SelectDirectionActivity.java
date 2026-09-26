package com.nous.tutoringapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SelectDirectionActivity extends AppCompatActivity {

    private Button btnTheoretical, btnPositive, btnHealth, btnEconomics, btnAllStudents, btnBackToGradeSelect;
    private TextView tvDirectionSubtitle;
    private String mode = "view";
    private String selectedGrade = "Γ' Λυκείου"; // Προεπιλογή

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_direction);

        if (getIntent().hasExtra("mode")) {
            mode = getIntent().getStringExtra("mode");
        }

        // 🎯 Λήψη της επιλεγμένης τάξης (Β' Λυκείου ή Γ' Λυκείου)
        if (getIntent().hasExtra("selected_grade")) {
            selectedGrade = getIntent().getStringExtra("selected_grade");
        }

        // 🎯 Σύνδεση των στοιχείων με το XML
        tvDirectionSubtitle = findViewById(R.id.tvDirectionSubtitle);
        btnTheoretical = findViewById(R.id.btnTheoretical);
        btnPositive = findViewById(R.id.btnPositive);
        btnHealth = findViewById(R.id.btnHealth);
        btnEconomics = findViewById(R.id.btnEconomics);
        btnAllStudents = findViewById(R.id.btnAllStudents);
        btnBackToGradeSelect = findViewById(R.id.btnBackToGradeSelect);

        // 🎯 ΔΥΝΑΜΙΚΗ ΑΛΛΑΓΗ ΚΕΙΜΕΝΩΝ ΑΝΑΛΟΓΑ ΜΕ ΤΗΝ ΤΑΞΗ
        if (tvDirectionSubtitle != null) {
            tvDirectionSubtitle.setText("Επιλέξτε κατεύθυνση " + selectedGrade + " για προβολή μαθητών");
        }
        if (btnAllStudents != null) {
            btnAllStudents.setText("👥  ΟΛΟΙ ΟΙ ΜΑΘΗΤΕΣ " + selectedGrade.toUpperCase());
        }

        // Click Listeners
        btnTheoretical.setOnClickListener(v -> openStudentListWithDirection("Θεωρητική"));
        btnPositive.setOnClickListener(v -> openStudentListWithDirection("Θετική"));
        btnHealth.setOnClickListener(v -> openStudentListWithDirection("Υγείας"));
        btnEconomics.setOnClickListener(v -> openStudentListWithDirection("Οικονομικά"));
        btnAllStudents.setOnClickListener(v -> openStudentListWithDirection(""));

        btnBackToGradeSelect.setOnClickListener(v -> finish());
    }

    private void openStudentListWithDirection(String direction) {
        Intent intent = new Intent(SelectDirectionActivity.this, StudentListActivity.class);
        intent.putExtra("selected_grade", selectedGrade);
        intent.putExtra("selected_direction", direction);
        intent.putExtra("mode", mode);
        startActivity(intent);
    }
}