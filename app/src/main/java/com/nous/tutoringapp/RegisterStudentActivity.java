package com.nous.tutoringapp;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RegisterStudentActivity extends AppCompatActivity {

    private EditText etFirstName, etLastName, etMonthlyFee;
    private Spinner spinnerGrade, spDirection;
    private TextView tvDirectionLabel;
    private Button btnSaveStudent, btnCancel, btnToggleMaterial;

    private DatabaseHelper dbHelper;

    // Μεταβλητές για την καταγραφή πληρωμής υλικού
    private boolean isMaterialPaid = false;
    private double materialTotalCost = 0.0;      // 🎯 Συνολική αξία υλικού (π.χ. 50€)
    private double materialPaymentAmount = 0.0;   // 🎯 Ποσό που πληρώνεται τώρα (π.χ. 20€)
    private String materialPaymentMethod = "Μετρητά";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_student);

        dbHelper = new DatabaseHelper(this);

        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etMonthlyFee = findViewById(R.id.etMonthlyFee);
        spinnerGrade = findViewById(R.id.spinnerGrade);
        spDirection = findViewById(R.id.spDirection);
        tvDirectionLabel = findViewById(R.id.tvDirectionLabel);
        btnSaveStudent = findViewById(R.id.btnSaveStudent);
        btnCancel = findViewById(R.id.btnCancel);
        btnToggleMaterial = findViewById(R.id.btnToggleMaterial);

        // Ρύθμιση για το Spinner της Τάξης
        String[] grades = {"Α' Γυμνασίου", "Β' Γυμνασίου", "Γ' Γυμνασίου", "Α' Λυκείου", "Β' Λυκείου", "Γ' Λυκείου"};
        ArrayAdapter<String> gradeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, grades);
        gradeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGrade.setAdapter(gradeAdapter);

        // Ρύθμιση για το Spinner της Κατεύθυνσης
        String[] directions = {"— Χωρίς Κατεύθυνση —", "📖 Θεωρητική", "🔬 Θετική", "🩺 Υγείας", "💼 Οικονομικά"};
        ArrayAdapter<String> directionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, directions);
        directionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDirection.setAdapter(directionAdapter);

        spinnerGrade.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedGrade = spinnerGrade.getSelectedItem().toString();
                if (selectedGrade.equals("Β' Λυκείου") || selectedGrade.equals("Γ' Λυκείου")) {
                    if (tvDirectionLabel != null) tvDirectionLabel.setVisibility(View.VISIBLE);
                    if (spDirection != null) spDirection.setVisibility(View.VISIBLE);
                } else {
                    if (tvDirectionLabel != null) tvDirectionLabel.setVisibility(View.GONE);
                    if (spDirection != null) {
                        spDirection.setVisibility(View.GONE);
                        spDirection.setSelection(0);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // 🎯 Πάτημα κουμπιού Εποπτικού Υλικού (Χρήση του dialog_book_payment.xml)
        btnToggleMaterial.setOnClickListener(v -> {
            if (!isMaterialPaid) {
                showCustomBookPaymentDialog();
            } else {
                isMaterialPaid = false;
                materialTotalCost = 0.0;
                materialPaymentAmount = 0.0;
                btnToggleMaterial.setText("⏳ ΜΗ ΠΛΗΡΩΜΕΝΟ (ΕΚΚΡΕΜΕΪ)");
                btnToggleMaterial.setBackgroundTintList(ColorStateList.valueOf(0xFF475569)); // Γκρι/Μπλε
            }
        });

        // 💾 Αποθήκευση Μαθητή & Αυτόματη Καταχώρηση Πληρωμής
        btnSaveStudent.setOnClickListener(v -> {
            String first = etFirstName.getText().toString().trim();
            String last = etLastName.getText().toString().trim();
            String feeStr = etMonthlyFee.getText().toString().trim();
            String grade = spinnerGrade.getSelectedItem().toString();

            // 🎯 Σωστός υπολογισμός κατάστασης υλικού
            String materialStatus;
            if (!isMaterialPaid || materialPaymentAmount <= 0) {
                materialStatus = "Εκκρεμεί";
            } else if (materialPaymentAmount >= materialTotalCost) {
                materialStatus = "Πληρωμένο";
            } else {
                materialStatus = "Μερική πληρωμή";
            }

            String direction = "";
            if ((grade.equals("Β' Λυκείου") || grade.equals("Γ' Λυκείου")) && spDirection != null) {
                String selectedDir = spDirection.getSelectedItem().toString();
                if (!selectedDir.startsWith("—")) {
                    direction = selectedDir;
                }
            }

            if (first.isEmpty() || last.isEmpty() || feeStr.isEmpty()) {
                Toast.makeText(this, "❌ Όλα τα πεδία είναι υποχρεωτικά!", Toast.LENGTH_SHORT).show();
                return;
            }

            double fee;
            try {
                fee = Double.parseDouble(feeStr.replace(",", "."));
            } catch (NumberFormatException e) {
                Toast.makeText(this, "❌ Παρακαλώ εισάγετε έγκυρο αριθμό στα δίδακτρα!", Toast.LENGTH_SHORT).show();
                return;
            }

            // 1. Αποθήκευση μαθητή στη βάση με το σωστό status
            Student newStudent = new Student(first, last, fee, grade, materialStatus, direction);
            long insertedId = dbHelper.addStudent(newStudent);

            if (insertedId != -1) {
                // 🎯 2. Αποθήκευση της συνολικής αξίας υλικού (π.χ. 50€)
                if (materialTotalCost > 0) {
                    dbHelper.updateStudentMaterialCost((int) insertedId, materialTotalCost);
                }

                // 🎯 3. Εάν πληρώθηκε ποσό, καταχωρείται η πληρωμή (π.χ. 20€)
                if (isMaterialPaid && materialPaymentAmount > 0) {
                    String today = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
                    String currentYear = dbHelper.getCurrentAcademicYear();

                    dbHelper.addPaymentForMonth(
                            (int) insertedId,
                            materialPaymentAmount,
                            today,
                            "Εποπτικό Υλικό",
                            materialPaymentMethod,
                            "Εποπτικό Υλικό",
                            currentYear
                    );
                }

                Toast.makeText(this, "✅ Ο μαθητής αποθηκεύτηκε επιτυχώς!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "❌ Σφάλμα κατά την αποθήκευση στη βάση δεδομένων!", Toast.LENGTH_SHORT).show();
            }
        });

        // Κουμπί Ακύρωση
        btnCancel.setOnClickListener(v -> finish());
    }

    private void showCustomBookPaymentDialog() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_book_payment);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvSubtitle = dialog.findViewById(R.id.tvBookDialogSubtitle);
        EditText etTotalAmount = dialog.findViewById(R.id.etBookAmount);
        android.widget.RadioGroup rgPaymentType = dialog.findViewById(R.id.rgPaymentType);
        android.widget.RadioButton rbPartial = dialog.findViewById(R.id.rbPartialPayment);
        View llPartialContainer = dialog.findViewById(R.id.llPartialContainer);
        EditText etPartialAmount = dialog.findViewById(R.id.etPartialAmount);

        // 📅 Ημερομηνία & Container
        View rlDateContainer = dialog.findViewById(R.id.rlBookDateContainer);
        TextView tvDate = dialog.findViewById(R.id.tvBookPaymentDate);

        Spinner spMethod = dialog.findViewById(R.id.spBookPaymentMethod);
        Button btnCancelDialog = dialog.findViewById(R.id.btnCancelBookDialog);
        Button btnSave = dialog.findViewById(R.id.btnSaveBookPayment);

        // Υπότιτλος με όνομα μαθητή
        String currentName = etLastName.getText().toString().trim() + " " + etFirstName.getText().toString().trim();
        if (tvSubtitle != null) {
            tvSubtitle.setText("Μαθητής: " + (currentName.trim().isEmpty() ? "Νέος Μαθητής" : currentName));
        }

        // Χωρίς προεπιλεγμένο ποσό
        if (etTotalAmount != null) {
            etTotalAmount.setText("");
            etTotalAmount.setHint("0.0");
        }

        if (rgPaymentType != null && llPartialContainer != null) {
            rgPaymentType.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.rbPartialPayment) {
                    llPartialContainer.setVisibility(View.VISIBLE);
                } else {
                    llPartialContainer.setVisibility(View.GONE);
                }
            });
        }

        // 📅 DatePickerDialog στο κλικ
        final java.util.Calendar calendar = java.util.Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        if (tvDate != null) {
            tvDate.setText(sdf.format(calendar.getTime()));

            View.OnClickListener dateClickListener = v -> {
                new android.app.DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    tvDate.setText(sdf.format(calendar.getTime()));
                }, calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH), calendar.get(java.util.Calendar.DAY_OF_MONTH)).show();
            };

            tvDate.setOnClickListener(dateClickListener);
            if (rlDateContainer != null) {
                rlDateContainer.setOnClickListener(dateClickListener);
            }
        }

        String[] paymentMethods = {"Μετρητά", "POS", "IRIS", "Alpha Bank", "Eurobank", "Τράπεζα Πειραιώς"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, paymentMethods);
        if (spMethod != null) spMethod.setAdapter(adapter);

        if (btnCancelDialog != null) btnCancelDialog.setOnClickListener(d -> dialog.dismiss());

        if (btnSave != null) {
            btnSave.setOnClickListener(d -> {
                String totalStr = (etTotalAmount != null) ? etTotalAmount.getText().toString().trim().replace(",", ".") : "";

                if (totalStr.isEmpty()) {
                    Toast.makeText(this, "Παρακαλώ συμπληρώστε τη συνολική αξία!", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    materialTotalCost = Double.parseDouble(totalStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Μη έγκυρο ποσό!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (rbPartial != null && rbPartial.isChecked() && etPartialAmount != null) {
                    String partialStr = etPartialAmount.getText().toString().trim().replace(",", ".");
                    materialPaymentAmount = partialStr.isEmpty() ? materialTotalCost : Double.parseDouble(partialStr);
                } else {
                    materialPaymentAmount = materialTotalCost;
                }

                if (spMethod != null && spMethod.getSelectedItem() != null) {
                    materialPaymentMethod = spMethod.getSelectedItem().toString();
                }

                isMaterialPaid = true;

                if (materialPaymentAmount >= materialTotalCost) {
                    btnToggleMaterial.setText("✅ ΠΛΗΡΩΘΗΚΕ (" + materialPaymentAmount + "€)");
                    btnToggleMaterial.setBackgroundTintList(ColorStateList.valueOf(0xFF10B981));
                } else {
                    btnToggleMaterial.setText("🟡 ΜΕΡΙΚΗ ΠΛΗΡΩΜΗ (" + materialPaymentAmount + "€ / " + materialTotalCost + "€)");
                    btnToggleMaterial.setBackgroundTintList(ColorStateList.valueOf(0xFFF59E0B));
                }

                dialog.dismiss();
            });
        }

        dialog.show();
    }
}