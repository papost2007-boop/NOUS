package com.nous.tutoringapp;

import com.nous.tutoringapp.BuildConfig;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import android.widget.AutoCompleteTextView;

public class MainActivity extends AppCompatActivity {

    private Button btnQuickPay;
    private Button btnTodayPayments;
    private Button btnStudentList;
    private Button btnRegisterStudent;
    private Button btnEditStudents;
    private Button btnStatistics;
    private Button btnSettings;
    private Button btnUnpaidStudents;

    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        // Σύνδεση των Buttons με τα IDs του XML
        btnQuickPay = findViewById(R.id.btnQuickPay);
        btnTodayPayments = findViewById(R.id.btnTodayPayments);
        btnStudentList = findViewById(R.id.btnStudentList);
        btnRegisterStudent = findViewById(R.id.btnRegisterStudent);
        btnEditStudents = findViewById(R.id.btnEditStudents);
        btnStatistics = findViewById(R.id.btnStatistics);
        btnSettings = findViewById(R.id.btnSettings);
        btnUnpaidStudents = findViewById(R.id.btnUnpaidStudents);

        ImageView imgLogo = findViewById(R.id.ivAppLogo);
        TextView tvAppName = findViewById(R.id.tvAppName);

        // 1. Εμφάνιση / Απόκρυψη Λογότυπου
        if (imgLogo != null) {
            if (BuildConfig.SHOW_LOGO) {
                imgLogo.setVisibility(View.VISIBLE);
            } else {
                imgLogo.setVisibility(View.GONE);
            }
        }

        // 2. Αλλαγή Τίτλου
        if (tvAppName != null) {
            if (BuildConfig.SHOW_LOGO) {
                tvAppName.setText("ΝΟΥΣ");
            } else {
                tvAppName.setText("EduManager");
            }
        }

        // ⚡ 0. Γρήγορη Πληρωμή
        if (btnQuickPay != null) {
            btnQuickPay.setOnClickListener(v -> showQuickPayDialog());
        }

        // 1. Προβολή Σημερινών Πληρωμών
        if (btnTodayPayments != null) {
            btnTodayPayments.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, TodayPaymentsActivity.class);
                startActivity(intent);
            });
        }

        // 2. Προβολή Μαθητολογίου
        if (btnStudentList != null) {
            btnStudentList.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, GradeSelectionActivity.class);
                startActivity(intent);
            });
        }

        // 3. Εγγραφή Μαθητή
        if (btnRegisterStudent != null) {
            btnRegisterStudent.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, RegisterStudentActivity.class);
                startActivity(intent);
            });
        }

        // 4. Επεξεργασία Μαθητών
        if (btnEditStudents != null) {
            btnEditStudents.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, GradeSelectionActivity.class);
                intent.putExtra("mode", "edit");
                startActivity(intent);
            });
        }

        // 📋 5. Οφειλέτες Μήνα
        if (btnUnpaidStudents != null) {
            btnUnpaidStudents.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, UnpaidStudentsActivity.class);
                startActivity(intent);
            });
        }

        // 6. Στατιστικά & Οικονομικά (Με κωδικό πρόσβασης)
        if (btnStatistics != null) {
            btnStatistics.setOnClickListener(v -> {
                if (!PasswordManager.isPasswordSet(MainActivity.this)) {
                    showCreatePasswordDialog();
                } else {
                    showEnterPasswordDialog();
                }
            });
        }

        // 7. Διαχείριση & Ρυθμίσεις
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            });
        }

        // 🔄 Προγραμματισμός αυτόματου Backup
        scheduleDailyBackup();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUnpaidButtonBadge();
    }

    private void showQuickPayDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_quick_pay);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        }

        // 🎯 1. Αντιστοίχιση των πεδίων
        AutoCompleteTextView etSearch = dialog.findViewById(R.id.etQuickSearch);
        LinearLayout layoutStudentSelected = dialog.findViewById(R.id.layoutStudentSelected);
        TextView txtSelectedStudentName = dialog.findViewById(R.id.txtSelectedStudentName);
        ImageButton btnClearSelection = dialog.findViewById(R.id.btnClearSelection);

        Spinner spinnerMonth = dialog.findViewById(R.id.spinnerQuickMonth);
        TextView tvDate = dialog.findViewById(R.id.tvQuickPayDate);
        EditText etAmount = dialog.findViewById(R.id.etQuickAmount);
        Spinner spinnerMethod = dialog.findViewById(R.id.spinnerQuickMethod);
        CheckBox cbFinalSettlement = dialog.findViewById(R.id.cbQuickFinalSettlement);
        Button btnSubmit = dialog.findViewById(R.id.btnQuickSubmit);
        Button btnCancel = dialog.findViewById(R.id.btnQuickCancel);

        // 📅 2. Αρχικοποίηση Ημερολογίου
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        if (tvDate != null) {
            tvDate.setText(sdf.format(calendar.getTime()));
            tvDate.setOnClickListener(v -> {
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH);
                int day = calendar.get(Calendar.DAY_OF_MONTH);

                new android.app.DatePickerDialog(this, (view, year1, monthOfYear, dayOfMonth) -> {
                    calendar.set(year1, monthOfYear, dayOfMonth);
                    tvDate.setText(sdf.format(calendar.getTime()));
                }, year, month, day).show();
            });
        }

        // 👨‍🎓 3. Δεδομένα για Μαθητές, Μήνες, Τρόπους Πληρωμής
        final ArrayList<Student> allStudents = dbHelper.getStudentsByGrade("Όλοι");
        ArrayList<String> studentNames = new ArrayList<>();
        for (Student s : allStudents) {
            studentNames.add(s.getLastName() + " " + s.getFirstName());
        }
        ArrayAdapter<String> searchAdapter = new ArrayAdapter<>(this, R.layout.item_student_dropdown, studentNames);

        final Student[] selectedStudent = {null}; // Εδώ αποθηκεύουμε ποιος επιλέχθηκε

        String[] monthNames = {
                "Σεπτέμβριος", "Οκτώβριος", "Νοέμβριος", "Δεκέμβριος",
                "Ιανουάριος", "Φεβρουάριος", "Μάρτιος", "Απρίλιος",
                "Μάιος", "Ιούνιος", "Ιούλιος"
        };
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(this, R.layout.custom_spinner_item, monthNames);
        monthAdapter.setDropDownViewResource(R.layout.custom_spinner_item);
        if (spinnerMonth != null) spinnerMonth.setAdapter(monthAdapter);

        String[] methods = {"Μετρητά", "POS", "IRIS", "Alpha Bank", "Eurobank", "Τράπεζα Πειραιώς"};
        ArrayAdapter<String> methodAdapter = new ArrayAdapter<>(this, R.layout.custom_spinner_item, methods);
        methodAdapter.setDropDownViewResource(R.layout.custom_spinner_item);
        if (spinnerMethod != null) spinnerMethod.setAdapter(methodAdapter);

        // 🎯 4. HELPER LOGIC: Υπολογισμός Υπολοίπου (Δηλώνεται πριν τα κλικ)
        Runnable updateRemainingAmount = () -> {
            if (selectedStudent[0] != null) {
                Student selected = selectedStudent[0];
                String selectedMonth = (spinnerMonth != null && spinnerMonth.getSelectedItem() != null)
                        ? spinnerMonth.getSelectedItem().toString() : "";

                if (!selectedMonth.isEmpty()) {
                    String academicYear = dbHelper.getCurrentAcademicYear();

                    double totalPaid = dbHelper.getTotalPaidForMonth(selected.getId(), selectedMonth, academicYear);
                    double monthlyFee = selected.getFee();
                    double remaining = monthlyFee - totalPaid;
                    boolean isFullyPaid = dbHelper.isMonthFullyPaid(selected.getId(), selectedMonth, academicYear, monthlyFee);

                    if (isFullyPaid || remaining <= 0) {
                        if (etAmount != null) {
                            etAmount.setText("0.0");
                            etAmount.setEnabled(false);
                        }
                        if (btnSubmit != null) {
                            btnSubmit.setText("Εξοφλημένος ✔️");
                            btnSubmit.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF064E3B));
                            btnSubmit.setEnabled(false);
                        }
                    } else {
                        if (etAmount != null) {
                            etAmount.setText(String.valueOf(remaining));
                            etAmount.setEnabled(true);
                        }
                        if (btnSubmit != null) {
                            btnSubmit.setText("Καταχώρηση (" + remaining + "€)");
                            btnSubmit.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF10B981));
                            btnSubmit.setEnabled(true);
                        }
                    }
                }
            } else {
                if (etAmount != null) etAmount.setText("");
                if (btnSubmit != null) {
                    btnSubmit.setText("Καταχώρηση");
                    btnSubmit.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF10B981));
                    btnSubmit.setEnabled(true);
                }
            }
        };

        // 🌟 5. Λειτουργία Αναζήτησης Μαθητή
        if (etSearch != null) {
            etSearch.setAdapter(searchAdapter);

            // Να πετάει τη λίστα με το που πατάς το πεδίο
            etSearch.setOnClickListener(v -> etSearch.showDropDown());
            etSearch.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) etSearch.showDropDown();
            });

            // Όταν επιλέγει μαθητή
            etSearch.setOnItemClickListener((parent, view, position, id) -> {
                String selectedName = (String) parent.getItemAtPosition(position);

                for (Student s : allStudents) {
                    if ((s.getLastName() + " " + s.getFirstName()).equals(selectedName)) {
                        selectedStudent[0] = s;
                        break;
                    }
                }

                etSearch.setVisibility(View.GONE);
                txtSelectedStudentName.setText(selectedName);
                layoutStudentSelected.setVisibility(View.VISIBLE);

                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);

                updateRemainingAmount.run();
            });
        }

        // ❌ Ακύρωση επιλογής Μαθητή (Το κόκκινο Χ)
        if (btnClearSelection != null) {
            btnClearSelection.setOnClickListener(v -> {
                selectedStudent[0] = null;
                layoutStudentSelected.setVisibility(View.GONE);
                etSearch.setVisibility(View.VISIBLE);

                // 🛡️ ΚΟΛΠΟ: Αφαιρούμε προσωρινά τα δεδομένα για να μην ανοίξει ακαριαία
                etSearch.setAdapter(null);

                etSearch.setText("");
                etSearch.requestFocus();

                // Ανοίγουμε το πληκτρολόγιο
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(etSearch, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                }

                // ⏳ Μόλις ανέβει το πληκτρολόγιο (μετά από 250ms), επαναφέρουμε τα δεδομένα και ανοίγουμε τη λίστα
                etSearch.postDelayed(() -> {
                    etSearch.setAdapter(searchAdapter);
                    etSearch.showDropDown();
                }, 250);

                updateRemainingAmount.run();
            });
        }

        // 7. Αλλαγή Μήνα
        if (spinnerMonth != null) {
            spinnerMonth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    updateRemainingAmount.run();
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        // ✅ 8. Κουμπί Καταχώρησης
        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> {
                if (selectedStudent[0] == null) {
                    Toast.makeText(this, "❌ Παρακαλώ επιλέξτε μαθητή", Toast.LENGTH_SHORT).show();
                    return;
                }

                String selectedMonth = (spinnerMonth != null) ? spinnerMonth.getSelectedItem().toString() : "";
                String method = (spinnerMethod != null) ? spinnerMethod.getSelectedItem().toString() : "Μετρητά";
                String amtStr = (etAmount != null) ? etAmount.getText().toString().trim().replace(",", ".") : "";
                String selectedDate = (tvDate != null) ? tvDate.getText().toString() : sdf.format(new Date());

                if (amtStr.isEmpty()) {
                    Toast.makeText(this, "❌ Εισάγετε ποσό", Toast.LENGTH_SHORT).show();
                    return;
                }

                double amount;
                try {
                    amount = Double.parseDouble(amtStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "❌ Μη έγκυρο ποσό", Toast.LENGTH_SHORT).show();
                    return;
                }

                long res = dbHelper.addPaymentForMonth(
                        selectedStudent[0].getId(),
                        amount,
                        selectedDate,
                        "Δίδακτρα " + selectedMonth,
                        method,
                        selectedMonth,
                        dbHelper.getCurrentAcademicYear()
                );

                if (res != -1) {
                    // 1. Φτιάχνουμε το αντικείμενο Payment επί τόπου
                    Payment newPayment = new Payment();
                    newPayment.setId((int) res); // Το res είναι το ID που μόλις πήραμε από τη βάση

                    // Έχουμε ήδη τον επιλεγμένο μαθητή, οπότε παίρνουμε το όνομά του
                    newPayment.setStudentName(selectedStudent[0].getName());

                    // Περνάμε τις μεταβλητές ακριβώς όπως τις έχεις στο Quick Pay
                    newPayment.setAmount(amount);
                    newPayment.setPaymentDate(selectedDate);
                    newPayment.setComments("Δίδακτρα " + selectedMonth);
                    newPayment.setPaymentMethod(method);
                    newPayment.setTargetMonth(selectedMonth);

                    // 2. Πετάμε το ερώτημα για εκτύπωση αντί για ξερό κλείσιμο
                    new android.app.AlertDialog.Builder(v.getContext())
                            .setTitle("✅ Επιτυχής Αποθήκευση")
                            .setMessage("Η πληρωμή καταχωρήθηκε επιτυχώς.\nΘέλετε να εκτυπώσετε την απόδειξη τώρα;")
                            .setPositiveButton("🖨️ Εκτύπωση", (d, which) -> {
                                // Τυπώνουμε το PDF!
                                ReceiptPrinter.printReceipt(v.getContext(), newPayment);

                                // Κλείνουμε το παράθυρο του Quick Pay και ανανεώνουμε το UI
                                dialog.dismiss();
                                updateUnpaidButtonBadge();
                            })
                            .setNegativeButton("Όχι", (d, which) -> {
                                // Απλά κλείνουμε και ανανεώνουμε (ο παλιός σου κώδικας)
                                dialog.dismiss();
                                updateUnpaidButtonBadge();
                            })
                            .setCancelable(false)
                            .show();

                } else {
                    Toast.makeText(this, "❌ Σφάλμα βάσης", Toast.LENGTH_LONG).show();
                }
            });
        }

        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void updateUnpaidButtonBadge() {
        if (btnUnpaidStudents == null || dbHelper == null) return;

        try {
            String currentMonth = getCurrentMonthName();
            String academicYear = dbHelper.getCurrentAcademicYear();

            ArrayList<Student> unpaidList = dbHelper.getUnpaidStudentsForMonth(currentMonth, academicYear);
            int count = (unpaidList != null) ? unpaidList.size() : 0;

            if (count > 0) {
                btnUnpaidStudents.setText("⚠️   Οφειλέτες Μήνα (" + count + ")");
            } else {
                btnUnpaidStudents.setText("⚠️   Οφειλέτες Μήνα");
            }
        } catch (Exception e) {
            btnUnpaidStudents.setText("⚠️   Οφειλέτες Μήνα");
        }
    }

    private String getCurrentMonthName() {
        String[] months = {
                "Ιανουάριος", "Φεβρουάριος", "Μάρτιος", "Απρίλιος",
                "Μάιος", "Ιούνιος", "Ιούλιος", "Αύγουστος",
                "Σεπτέμβριος", "Οκτώβριος", "Νοέμβριος", "Δεκέμβριος"
        };
        Calendar cal = Calendar.getInstance();
        return months[cal.get(Calendar.MONTH)];
    }

    private void showCreatePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🔒 Ορισμός Κωδικού Ασφαλείας");
        builder.setMessage("Δεν έχετε ορίσει κωδικό για τα Στατιστικά. Παρακαλώ εισάγετε έναν νέο κωδικό:");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("Αποθήκευση", (dialog, which) -> {
            String newPass = input.getText().toString().trim();
            if (!newPass.isEmpty()) {
                PasswordManager.setPassword(MainActivity.this, newPass);
                Toast.makeText(MainActivity.this, "Ο κωδικός αποθηκεύτηκε!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MainActivity.this, StatisticsActivity.class));
            } else {
                Toast.makeText(MainActivity.this, "Ο κωδικός δεν μπορεί να είναι κενός", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Ακύρωση", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showEnterPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🔒 Απαιτείται Κωδικός");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("Είσοδος", (dialog, which) -> {
            String enteredPass = input.getText().toString().trim();
            if (PasswordManager.checkPassword(MainActivity.this, enteredPass)) {
                startActivity(new Intent(MainActivity.this, StatisticsActivity.class));
            } else {
                Toast.makeText(MainActivity.this, "Λάθος κωδικός!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Ακύρωση", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void scheduleDailyBackup() {
        androidx.work.PeriodicWorkRequest backupRequest =
                new androidx.work.PeriodicWorkRequest.Builder(
                        AutoBackupWorker.class,
                        24, java.util.concurrent.TimeUnit.HOURS)
                        .build();

        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "DailyAutoBackup",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                backupRequest
        );
    }
}