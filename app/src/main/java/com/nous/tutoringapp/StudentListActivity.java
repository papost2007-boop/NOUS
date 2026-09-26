package com.nous.tutoringapp;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import android.widget.RadioGroup;
import android.widget.RadioButton;

public class StudentListActivity extends AppCompatActivity {

    private ListView lvStudents;
    private Button btnBack;

    private DatabaseHelper dbHelper;
    private ArrayList<Student> studentsList;
    private StudentAdapter adapter;

    // Μεταβλητή για τη λειτουργία της οθόνης (view ή edit)
    private String mode = "view";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_list);

        lvStudents = findViewById(R.id.lvStudents);
        btnBack = findViewById(R.id.btnBack);

        // 🚪 ΚΟΥΜΠΙ ΕΠΙΣΤΡΟΦΗΣ
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        dbHelper = new DatabaseHelper(this);

        // Λήψη της λειτουργίας (mode) από το Intent
        if (getIntent().hasExtra("mode")) {
            mode = getIntent().getStringExtra("mode");
        }

        // Δυναμική αλλαγή του τίτλου αν είμαστε σε Edit Mode
        if ("edit".equals(mode)) {
            try {
                LinearLayout root = (LinearLayout) lvStudents.getParent();
                LinearLayout header = (LinearLayout) root.getChildAt(0);
                TextView tvTitle = (TextView) header.getChildAt(0);
                tvTitle.setText("✏️ Επεξεργασία Μαθητών");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Φόρτωση της λίστας μαθητών
        loadStudentsFromDatabase();

        // 🎯 ΕΛΕΓΧΟΣ: Αν ήρθαμε από τους Οφειλέτες για συγκεκριμένο μαθητή
        int targetStudentId = getIntent().getIntExtra("target_student_id", -1);
        if (targetStudentId != -1 && studentsList != null) {
            for (Student s : studentsList) {
                if (s.getId() == targetStudentId) {
                    openStudentProfileDialog(s);
                    break;
                }
            }
        }

        // 👤 Απλό πάτημα (Click) για άνοιγμα του Προφίλ ή της Επεξεργασίας
        lvStudents.setOnItemClickListener((parent, view, position, id) -> {
            if (studentsList != null && position < studentsList.size()) {
                Student selectedStudent = studentsList.get(position);

                if ("edit".equals(mode)) {
                    openEditStudentDialog(selectedStudent);
                } else {
                    openStudentProfileDialog(selectedStudent);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStudentsFromDatabase();
    }

    /**
     * Αντλεί τους μαθητές από τη SQLite και τους φιλτράρει με βάση την επιλεγμένη τάξη, κατεύθυνση και αναζήτηση.
     */
    private void loadStudentsFromDatabase() {
        String selectedGrade = getIntent().getStringExtra("selected_grade");
        String selectedDirection = getIntent().getStringExtra("selected_direction");
        if (selectedDirection == null) {
            selectedDirection = getIntent().getStringExtra("direction");
        }
        String searchQuery = getIntent().getStringExtra("search_query");

        // 1. Φόρτωση από τη βάση δεδομένων με βάση Τάξη ΚΑΙ Κατεύθυνση
        if (selectedDirection != null && !selectedDirection.trim().isEmpty() && !selectedDirection.equalsIgnoreCase("Όλες") && !selectedDirection.equalsIgnoreCase("Όλοι")) {
            studentsList = dbHelper.getStudentsByGradeAndDirection(selectedGrade, selectedDirection);
        } else if (selectedGrade != null && !selectedGrade.trim().isEmpty() && !selectedGrade.equals("Όλοι")) {
            studentsList = dbHelper.getStudentsByGrade(selectedGrade);
        } else {
            studentsList = dbHelper.getStudentsByGrade("Όλοι");
        }

        if (studentsList == null) {
            studentsList = new ArrayList<>();
        }

        // 🔍 2. Φιλτράρισμα ΜΟΝΟ αν ο χρήστης έγραψε πραγματικά κάτι στο Search Bar!
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            ArrayList<Student> filteredList = new ArrayList<>();
            String queryLower = searchQuery.trim().toLowerCase();

            for (Student s : studentsList) {
                String fullName = (s.getFirstName() + " " + s.getLastName()).toLowerCase();
                if (fullName.contains(queryLower)) {
                    filteredList.add(s);
                }
            }
            studentsList = filteredList;
        }

        // 🎯 3. ΕΝΕΡΓΟΠΟΙΗΜΕΝΟΣ ADAPTER (Σύνδεση με το ListView!)
        if (adapter == null) {
            adapter = new StudentAdapter(this, studentsList);
            lvStudents.setAdapter(adapter);
        } else {
            adapter.clear();
            adapter.addAll(studentsList);
            adapter.notifyDataSetChanged();
        }
    }
    /**
     * 👤 Ανοίγει το παράθυρο επεξεργασίας στοιχείων του μαθητή.
     */
    private void openEditStudentDialog(Student student) {
        Dialog editDialog = new Dialog(this);
        editDialog.setContentView(R.layout.dialog_edit_student);
        if (editDialog.getWindow() != null) {
            editDialog.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        }

        EditText etFirst = editDialog.findViewById(R.id.etEditFirstName);
        EditText etLast = editDialog.findViewById(R.id.etEditLastName);
        EditText etFee = editDialog.findViewById(R.id.etEditMonthlyFee);
        Spinner spinnerGrade = editDialog.findViewById(R.id.spinnerEditGrade);

        TextView tvDirectionLabel = editDialog.findViewById(R.id.tvEditDirectionLabel);
        Spinner spinnerDirection = editDialog.findViewById(R.id.spinnerEditDirection);

        Button btnSave = editDialog.findViewById(R.id.btnEditSave);
        Button btnDelete = editDialog.findViewById(R.id.btnEditDelete);
        Button btnCancel = editDialog.findViewById(R.id.btnEditCancel);

        etFirst.setText(student.getFirstName());
        etLast.setText(student.getLastName());
        etFee.setText(String.valueOf(student.getFee()));

        String[] directions = {"— Χωρίς Κατεύθυνση —", "📖 Θεωρητική", "🔬 Θετική", "🩺 Υγείας", "💼 Οικονομικά"};
        ArrayAdapter<String> directionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, directions);
        directionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (spinnerDirection != null) {
            spinnerDirection.setAdapter(directionAdapter);
        }

        String[] grades = {"Α' Γυμνασίου", "Β' Γυμνασίου", "Γ' Γυμνασίου", "Α' Λυκείου", "Β' Λυκείου", "Γ' Λυκείου"};
        ArrayAdapter<String> gradeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, grades);
        gradeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGrade.setAdapter(gradeAdapter);

        for (int i = 0; i < grades.length; i++) {
            if (grades[i].equals(student.getGrade())) {
                spinnerGrade.setSelection(i);
                break;
            }
        }

        spinnerGrade.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedGrade = spinnerGrade.getSelectedItem().toString();
                if (selectedGrade.equals("Β' Λυκείου") || selectedGrade.equals("Γ' Λυκείου")) {
                    if (tvDirectionLabel != null) tvDirectionLabel.setVisibility(View.VISIBLE);
                    if (spinnerDirection != null) spinnerDirection.setVisibility(View.VISIBLE);
                } else {
                    if (tvDirectionLabel != null) tvDirectionLabel.setVisibility(View.GONE);
                    if (spinnerDirection != null) {
                        spinnerDirection.setVisibility(View.GONE);
                        spinnerDirection.setSelection(0);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        if ((student.getGrade().equals("Β' Λυκείου") || student.getGrade().equals("Γ' Λυκείου")) && spinnerDirection != null) {
            String currentDir = student.getDirection();
            for (int i = 0; i < directions.length; i++) {
                if (!currentDir.isEmpty() && directions[i].contains(currentDir.replaceAll("[^\\p{L}\\p{Nd}]", "").trim())) {
                    spinnerDirection.setSelection(i);
                    break;
                }
            }
        }

        btnSave.setOnClickListener(v -> {
            String first = etFirst.getText().toString().trim();
            String last = etLast.getText().toString().trim();
            String feeStr = etFee.getText().toString().trim();
            String grade = spinnerGrade.getSelectedItem().toString();

            // Διατηρούμε αυτόματα τις τρέχουσες τιμές υλικού του μαθητή
            String material = student.getMaterialStatus();
            double materialCost = student.getMaterialCost();

            String direction = "";
            if ((grade.equals("Β' Λυκείου") || grade.equals("Γ' Λυκείου")) && spinnerDirection != null) {
                String selectedDir = spinnerDirection.getSelectedItem().toString();
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

            Student updatedStudent = new Student(student.getId(), first, last, fee, grade, material, direction, materialCost);
            dbHelper.updateStudent(updatedStudent);

            Toast.makeText(this, "✅ Τα στοιχεία του μαθητή ενημερώθηκαν!", Toast.LENGTH_SHORT).show();
            editDialog.dismiss();
            loadStudentsFromDatabase();
        });

        btnDelete.setOnClickListener(v -> {
            editDialog.dismiss();
            showDeleteConfirmation(student);
        });

        btnCancel.setOnClickListener(v -> editDialog.dismiss());

        editDialog.show();
    }
    /**
     * 👤 Ανοίγει το αναλυτικό παράθυρο (Custom Dialog) προφίλ του μαθητή.
     */
    private void openStudentProfileDialog(Student student) {
        if (student == null) return;

        try {
            Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.dialog_student_profile);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            }

            TextView tvName = dialog.findViewById(R.id.tvProfileName);
            TextView tvGrade = dialog.findViewById(R.id.tvProfileGrade);
            TextView tvFee = dialog.findViewById(R.id.tvProfileFee);
            TextView tvMaterial = dialog.findViewById(R.id.tvProfileMaterial);

            // 🎯 Σύνδεση του κουμπιού Εποπτικού Υλικού
            View btnMaterialsAction = dialog.findViewById(R.id.btnMaterialsAction);

            Button btnHistory = dialog.findViewById(R.id.btnProfileHistory);
            Button btnNotes = dialog.findViewById(R.id.btnProfileNotes);
            Button btnClose = dialog.findViewById(R.id.btnProfileClose);

            if (tvName != null) tvName.setText(student.getLastName() + " " + student.getFirstName());

            if (tvGrade != null) {
                String gradeInfo = "Τάξη: " + student.getGrade();
                if (student.getDirection() != null && !student.getDirection().isEmpty()) {
                    gradeInfo += " (" + student.getDirection() + ")";
                }
                tvGrade.setText(gradeInfo);
            }

            if (tvFee != null) tvFee.setText(String.format(Locale.getDefault(), "%.2f €", student.getFee()));

            if (tvMaterial != null) {
                String mat = student.getMaterialStatus() != null ? student.getMaterialStatus() : "";
                tvMaterial.setText(mat);

                if ("Πληρωμένο".equals(mat)) {
                    tvMaterial.setTextColor(Color.WHITE);
                    if (btnMaterialsAction != null) {
                        btnMaterialsAction.setBackgroundResource(R.drawable.bg_btn_materials_paid);
                    }
                } else {
                    tvMaterial.setTextColor(0xFFF59E0B);
                }
            }
            btnMaterialsAction.setOnClickListener(v -> {
                // 1. Κλείνουμε το προφίλ
                dialog.dismiss();

                // 2. Φωνάζουμε τον Manager για το ΕΠΟΠΤΙΚΟ ΥΛΙΚΟ (showBookPaymentDialog)
                PaymentDialogManager.showBookPaymentDialog(this, dbHelper, student, () -> {
                    // 3. Τι να κάνει μόλις τελειώσει: Ξανανοίγει το προφίλ!
                    openStudentProfileDialog(student);
                });
            });

            setupMonthButtons(dialog, student);

            if (btnHistory != null) {
                btnHistory.setText("📜  ΙΣΤΟΡΙΚΟ");
                btnHistory.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF3B82F6));
                btnHistory.setOnClickListener(v -> {
                    // 1. Κλείνουμε το προφίλ
                    dialog.dismiss();

                    // 2. Ανοίγουμε το Ιστορικό και του λέμε τι να κάνει όταν κλείσει
                    PaymentHistoryManager.showDialog(this, dbHelper, student, () -> {

                        // Όταν κλείσει το ιστορικό: Ανανεώνουμε τους μαθητές (για να τραβήξει φρέσκο status Εποπτικού Υλικού)
                        loadStudentsFromDatabase();

                        // Ψάχνουμε τον μαθητή με τα νέα δεδομένα και ξανανοίγουμε το προφίλ
                        for (Student s : studentsList) {
                            if (s.getId() == student.getId()) {
                                openStudentProfileDialog(s);
                                break;
                            }
                        }
                    });
                });
            }

            if (btnNotes != null) {
                btnNotes.setText("📝  ΣΗΜΕΙΩΣΕΙΣ");
                btnNotes.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF10B981));
                // Η ΝΕΑ ΚΛΗΣΗ:
                btnNotes.setOnClickListener(v -> StudentNotesManager.showDialog(this, dbHelper, student));
            }

            if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());

            dialog.show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "❌ Σφάλμα προφίλ: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setupMonthButtons(Dialog dialog, Student student) {
        try {
            String currentAcademicYear = dbHelper.getCurrentAcademicYear();

            int[] buttonIds = {
                    R.id.btnMonthSep, R.id.btnMonthOct, R.id.btnMonthNov, R.id.btnMonthDec,
                    R.id.btnMonthJan, R.id.btnMonthFeb, R.id.btnMonthMar, R.id.btnMonthApr,
                    R.id.btnMonthMay, R.id.btnMonthJun, R.id.btnMonthJul
            };

            String[] monthNames = {
                    "Σεπτέμβριος", "Οκτώβριος", "Νοέμβριος", "Δεκέμβριος",
                    "Ιανουάριος", "Φεβρουάριος", "Μάρτιος", "Απρίλιος",
                    "Μάιος", "Ιούνιος", "Ιούλιος"
            };

            for (int i = 0; i < buttonIds.length; i++) {
                Button btnMonth = dialog.findViewById(buttonIds[i]);
                if (btnMonth == null) continue;

                String monthName = monthNames[i];
                double amountPaid = 0.0;
                boolean isFullyPaid = false;

                try {
                    amountPaid = dbHelper.getAmountPaidForMonth(student.getId(), monthName, currentAcademicYear);
                    isFullyPaid = dbHelper.isMonthFullyPaid(student.getId(), monthName, currentAcademicYear, student.getFee());
                } catch (Exception ignored) {}

                if (isFullyPaid) {
                    btnMonth.setText(monthName + " ✔️");
                    btnMonth.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF064E3B));
                    btnMonth.setTextColor(0xFF34D399);
                } else if (amountPaid > 0) {
                    btnMonth.setText(monthName + " ⏳");
                    btnMonth.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF78350F));
                    btnMonth.setTextColor(0xFFFBBF24);
                } else {
                    btnMonth.setText(monthName);
                    btnMonth.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF1E293B));
                    btnMonth.setTextColor(0xFF94A3B8);
                }

                btnMonth.setOnClickListener(v -> {
                    // 1. Κλείνουμε το τρέχον παράθυρο του προφίλ
                    dialog.dismiss();

                    // 2. Φωνάζουμε τον νέο Manager και του περνάμε το "τι θα γίνει μετά" (Runnable)
                    PaymentDialogManager.openAddPaymentDialog(this, dbHelper, student, monthName, () -> {
                        // 3. Μόλις κλείσει η πληρωμή, ξανανοίγουμε το προφίλ αστραπιαία!
                        openStudentProfileDialog(student);
                    });
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void showDeleteConfirmation(Student student) {
        new AlertDialog.Builder(this)
                .setTitle("⚠️ Διαγραφή Μαθητή")
                .setMessage("Είστε σίγουροι ότι θέλετε να διαγράψετε οριστικά τον/την "
                        + student.getLastName() + " " + student.getFirstName() )
                .setPositiveButton("Ναι, Διαγραφή", (dialog, which) -> {
                    dbHelper.deleteStudent(student.getId());
                    Toast.makeText(this, "✅ Ο μαθητής διαγράφηκε επιτυχώς!", Toast.LENGTH_SHORT).show();
                    loadStudentsFromDatabase();
                })
                .setNegativeButton("Ακύρωση", null)
                .show();
    }
    /**
     * 🎯 CUSTOM ADAPTER ΓΙΑ ΤΟΥΣ ΜΑΘΗΤΕΣ
     */
    private class StudentAdapter extends ArrayAdapter<Student> {

        public StudentAdapter(Context context, ArrayList<Student> students) {
            super(context, 0, students);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_student, parent, false);
            }

            Student student = getItem(position);

            TextView tvInitials = convertView.findViewById(R.id.tvStudentInitials);
            TextView tvFullName = convertView.findViewById(R.id.tvStudentFullName);
            TextView tvGrade = convertView.findViewById(R.id.tvStudentGrade);
            TextView tvFee = convertView.findViewById(R.id.tvStudentFee);

            if (student != null) {
                String firstCharLast = student.getLastName().isEmpty() ? "" : String.valueOf(student.getLastName().charAt(0));
                String firstCharFirst = student.getFirstName().isEmpty() ? "" : String.valueOf(student.getFirstName().charAt(0));
                String initials = (firstCharLast + firstCharFirst).toUpperCase();
                if (tvInitials != null) tvInitials.setText(initials);

                if (tvFullName != null) tvFullName.setText(student.getLastName() + " " + student.getFirstName());

                String displayGrade = student.getGrade();
                if (!student.getDirection().isEmpty()) {
                    displayGrade += " (" + student.getDirection() + ")";
                }
                if (tvGrade != null) tvGrade.setText(displayGrade);

                if (tvFee != null) tvFee.setText(String.format(Locale.getDefault(), "%.2f €", student.getFee()));

                int badgeColor = 0xFF4F46E5;
                if (student.getGrade().contains("Λυκείου")) {
                    badgeColor = 0xFF8B5CF6;
                } else if (student.getGrade().contains("Γυμνασίου")) {
                    badgeColor = 0xFF3B82F6;
                }

                if (tvInitials != null && tvInitials.getBackground() != null) {
                    tvInitials.getBackground().setColorFilter(badgeColor, android.graphics.PorterDuff.Mode.SRC_IN);
                }
            }

            return convertView;
        }
    }
}