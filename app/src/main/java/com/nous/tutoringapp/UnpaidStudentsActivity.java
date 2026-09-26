package com.nous.tutoringapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.Calendar;

public class UnpaidStudentsActivity extends AppCompatActivity {

    private Spinner spinnerMonth;
    private ListView lvUnpaid;
    private TextView tvUnpaidCount;
    private Button btnBack;

    private DatabaseHelper dbHelper;
    private ArrayList<Student> unpaidList;

    private final String[] monthNames = {
            "Σεπτέμβριος", "Οκτώβριος", "Νοέμβριος", "Δεκέμβριος",
            "Ιανουάριος", "Φεβρουάριος", "Μάρτιος", "Απρίλιος",
            "Μάιος", "Ιούνιος", "Ιούλιος"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unpaid_students);

        spinnerMonth = findViewById(R.id.spinnerUnpaidMonth);
        lvUnpaid = findViewById(R.id.lvUnpaidStudents);
        tvUnpaidCount = findViewById(R.id.tvUnpaidCount);
        btnBack = findViewById(R.id.btnUnpaidBack);

        dbHelper = new DatabaseHelper(this);

        // 1. Ρύθμιση Spinner
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, monthNames);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMonth.setAdapter(monthAdapter);

        // 2. Επιλογή τρέχοντος μήνα
        int currentMonthIndex = getCurrentMonthIndex();
        spinnerMonth.setSelection(currentMonthIndex);

        // 3. Listener για αλλαγή μήνα
        spinnerMonth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadUnpaidStudents(monthNames[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 4. Click σε μαθητή -> Μετάβαση στο Μαθητολόγιο για τον συγκεκριμένο μαθητή
        lvUnpaid.setOnItemClickListener((parent, view, position, id) -> {
            if (unpaidList != null && position < unpaidList.size()) {
                Student selected = unpaidList.get(position);

                Intent intent = new Intent(UnpaidStudentsActivity.this, StudentListActivity.class);
                intent.putExtra("selected_grade", "Όλοι");
                intent.putExtra("mode", "view");

                // 🎯 Στέλνουμε το ID του επιλεγμένου μαθητή
                intent.putExtra("target_student_id", selected.getId());
                startActivity(intent);
            }
        });

        // 👈 5. ΚΟΥΜΠΙ ΕΠΙΣΤΡΟΦΗΣ (Αυτό έλειπε)
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (spinnerMonth != null && spinnerMonth.getSelectedItem() != null) {
            loadUnpaidStudents(spinnerMonth.getSelectedItem().toString());
        }
    }

    private void loadUnpaidStudents(String selectedMonth) {
        String academicYear = dbHelper.getCurrentAcademicYear();
        unpaidList = dbHelper.getUnpaidStudentsForMonth(selectedMonth, academicYear);

        if (unpaidList == null) {
            unpaidList = new ArrayList<>();
        }

        int count = unpaidList.size();

        // 🎯 Ενημέρωση του συνολικού αριθμού οφειλετών στην οθόνη
        if (tvUnpaidCount != null) {
            if (count > 0) {
                tvUnpaidCount.setText("⚠️ Σύνολο οφειλετών για τον " + selectedMonth + ": " + count + " μαθητές");
                tvUnpaidCount.setTextColor(0xFFF87171); // Κόκκινο
            } else {
                tvUnpaidCount.setText("✅ Καμία οφειλή για τον " + selectedMonth + "!");
                tvUnpaidCount.setTextColor(0xFF34D399); // Πράσινο
            }
        }

        // Adapter για τη λίστα
        ArrayAdapter<Student> adapter = new ArrayAdapter<Student>(this, android.R.layout.simple_list_item_2, android.R.id.text1, unpaidList) {
            @Override
            public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                android.view.View view = super.getView(position, convertView, parent);
                android.widget.TextView text1 = view.findViewById(android.R.id.text1);
                android.widget.TextView text2 = view.findViewById(android.R.id.text2);

                Student s = getItem(position);
                if (s != null) {
                    if (text1 != null) {
                        text1.setText(s.getLastName() + " " + s.getFirstName() + " (" + s.getGrade() + ")");
                        text1.setTextColor(0xFFFFFFFF);
                        text1.setTextSize(16);
                    }

                    if (text2 != null) {
                        double paid = dbHelper.getAmountPaidForMonth(s.getId(), selectedMonth, academicYear);
                        double remaining = s.getFee() - paid;
                        text2.setText(String.format("Οφειλή: %.2f € / Μηνιαίο: %.2f €", remaining, s.getFee()));
                        text2.setTextColor(0xFFF87171);
                    }
                }
                return view;
            }
        };

        lvUnpaid.setAdapter(adapter);
    }

    private int getCurrentMonthIndex() {
        Calendar cal = Calendar.getInstance();
        int month = cal.get(Calendar.MONTH);

        switch (month) {
            case Calendar.SEPTEMBER: return 0;
            case Calendar.OCTOBER: return 1;
            case Calendar.NOVEMBER: return 2;
            case Calendar.DECEMBER: return 3;
            case Calendar.JANUARY: return 4;
            case Calendar.FEBRUARY: return 5;
            case Calendar.MARCH: return 6;
            case Calendar.APRIL: return 7;
            case Calendar.MAY: return 8;
            case Calendar.JUNE: return 9;
            case Calendar.JULY: return 10;
            default: return 0;
        }
    }
}