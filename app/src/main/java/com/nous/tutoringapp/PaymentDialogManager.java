package com.nous.tutoringapp;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class PaymentDialogManager {

    /**
     * Ανοίγει το παράθυρο για Πληρωμή Μήνα (Δίδακτρα)
     */
    public static void openAddPaymentDialog(Context context, DatabaseHelper dbHelper, Student student, String selectedMonth, Runnable onSuccess) {
        if (student == null) return;

        try {
            Dialog payDialog = new Dialog(context);
            payDialog.setContentView(R.layout.dialog_add_payment);
            if (payDialog.getWindow() != null) {
                payDialog.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            }

            EditText etAmount = payDialog.findViewById(R.id.etPayAmount);
            TextView tvPayDate = payDialog.findViewById(R.id.tvPayDate);
            Spinner spinnerMethod = payDialog.findViewById(R.id.spinnerPayMethod);
            EditText etComments = payDialog.findViewById(R.id.etPayComments);
            CheckBox cbFinalSettlement = payDialog.findViewById(R.id.cbFinalSettlement);

            Button btnSubmit = payDialog.findViewById(R.id.btnSubmitPayment);
            Button btnCancel = payDialog.findViewById(R.id.btnCancelPayment);

            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            if (tvPayDate != null) {
                tvPayDate.setText(sdf.format(calendar.getTime()));
                tvPayDate.setOnClickListener(v -> {
                    new DatePickerDialog(context, (view, year1, monthOfYear, dayOfMonth) -> {
                        calendar.set(year1, monthOfYear, dayOfMonth);
                        tvPayDate.setText(sdf.format(calendar.getTime()));
                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
                });
            }

            String academicYear = dbHelper.getCurrentAcademicYear();
            double paidAlready = 0.0;
            boolean isFullyPaid = false;

            try {
                if (selectedMonth != null && !selectedMonth.isEmpty()) {
                    paidAlready = dbHelper.getTotalPaidForMonth(student.getId(), selectedMonth, academicYear);
                    isFullyPaid = dbHelper.isMonthFullyPaid(student.getId(), selectedMonth, academicYear, student.getFee());
                }
            } catch (Exception ignored) {}

            double remaining = student.getFee() - paidAlready;
            if (remaining < 0) remaining = 0;

            if (isFullyPaid || (paidAlready > 0 && remaining <= 0)) {
                if (etAmount != null) {
                    etAmount.setText("0.0");
                    etAmount.setEnabled(false);
                }
                if (btnSubmit != null) {
                    btnSubmit.setText("Εξοφλημένος ✔️");
                    btnSubmit.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF064E3B));
                    btnSubmit.setEnabled(false);
                }
                if (cbFinalSettlement != null) cbFinalSettlement.setEnabled(false);
            } else {
                if (etAmount != null) {
                    etAmount.setText(String.valueOf(remaining > 0 ? remaining : student.getFee()));
                    etAmount.setEnabled(true);
                }
                if (btnSubmit != null) {
                    btnSubmit.setText(remaining > 0 && paidAlready > 0 ? "Καταχώρηση Δόσης (" + remaining + "€)" : "Καταχώρηση");
                    btnSubmit.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF10B981));
                    btnSubmit.setEnabled(true);
                }
                if (cbFinalSettlement != null) cbFinalSettlement.setEnabled(true);
            }

            if (etComments != null && selectedMonth != null && !selectedMonth.isEmpty()) {
                etComments.setText(selectedMonth);
            }

            String[] methods = {"Μετρητά", "POS", "IRIS", "Alpha Bank", "Eurobank", "Τράπεζα Πειραιώς"};
            if (spinnerMethod != null) {
                ArrayAdapter<String> methodAdapter = new ArrayAdapter<>(context, R.layout.custom_spinner_item, methods);
                methodAdapter.setDropDownViewResource(R.layout.custom_spinner_item);
                spinnerMethod.setAdapter(methodAdapter);
            }

            if (btnSubmit != null) {
                btnSubmit.setOnClickListener(v -> {
                    try {
                        String amtStr = (etAmount != null) ? etAmount.getText().toString().trim() : "";
                        String comments = (etComments != null) ? etComments.getText().toString().trim() : "";
                        String method = (spinnerMethod != null && spinnerMethod.getSelectedItem() != null) ? spinnerMethod.getSelectedItem().toString() : "Μετρητά";

                        if (amtStr.isEmpty()) {
                            Toast.makeText(context, "❌ Το ποσό είναι υποχρεωτικό!", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        double amount;
                        try {
                            amtStr = amtStr.replace(",", ".");
                            amount = Double.parseDouble(amtStr);
                        } catch (NumberFormatException e) {
                            Toast.makeText(context, "❌ Παρακαλώ εισάγετε έγκυρο αριθμό!", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String paymentDate = (tvPayDate != null) ? tvPayDate.getText().toString() : sdf.format(new Date());

                        long payId = dbHelper.addPaymentForMonth(
                                student.getId(),
                                amount,
                                paymentDate,
                                comments,
                                method,
                                selectedMonth != null ? selectedMonth : "",
                                academicYear
                        );

                        if (payId != -1) {
                            Toast.makeText(context, "✅ Η πληρωμή καταχωρήθηκε!", Toast.LENGTH_SHORT).show();
                            payDialog.dismiss();
                            if (onSuccess != null) onSuccess.run();
                        } else {
                            Toast.makeText(context, "❌ Σφάλμα SQLite: " + dbHelper.getLastError(), Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception ex) {
                        Toast.makeText(context, "❌ Σφάλμα: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }

            if (btnCancel != null) {
                btnCancel.setOnClickListener(v -> {
                    payDialog.dismiss();
                    if (onSuccess != null) onSuccess.run(); // Καλεί το refresh (για να ξανανοίξει το προφίλ)
                });
            }

            payDialog.show();
        } catch (Exception e) {
            Toast.makeText(context, "❌ Σφάλμα παραθύρου πληρωμής: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Ανοίγει το παράθυρο για Πληρωμή Εποπτικού Υλικού
     */
    public static void showBookPaymentDialog(Context context, DatabaseHelper dbHelper, Student student, Runnable onSuccess) {
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_book_payment);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvSubtitle = dialog.findViewById(R.id.tvBookDialogSubtitle);
        EditText etTotalAmount = dialog.findViewById(R.id.etBookAmount);
        RadioGroup rgPaymentType = dialog.findViewById(R.id.rgPaymentType);
        RadioButton rbFull = dialog.findViewById(R.id.rbFullPayment);
        RadioButton rbPartial = dialog.findViewById(R.id.rbPartialPayment);
        View llPartialContainer = dialog.findViewById(R.id.llPartialContainer);
        EditText etPartialAmount = dialog.findViewById(R.id.etPartialAmount);
        View rlDateContainer = dialog.findViewById(R.id.rlBookDateContainer);
        TextView tvDate = dialog.findViewById(R.id.tvBookPaymentDate);
        Spinner spMethod = dialog.findViewById(R.id.spBookPaymentMethod);
        Button btnCancel = dialog.findViewById(R.id.btnCancelBookDialog);
        Button btnSave = dialog.findViewById(R.id.btnSaveBookPayment);

        double savedTotalCost = student.getMaterialCost();
        double alreadyPaid = dbHelper.getTotalPaidForMaterials(student.getId());

        String subtitleText = "Μαθητής: " + student.getLastName() + " " + student.getFirstName();
        if (alreadyPaid > 0) subtitleText += String.format(Locale.getDefault(), "\n(Έχουν δοθεί: %.2f €)", alreadyPaid);
        if (tvSubtitle != null) tvSubtitle.setText(subtitleText);

        if (savedTotalCost > 0) {
            etTotalAmount.setText(String.format(Locale.getDefault(), "%.2f", savedTotalCost));
            etTotalAmount.setEnabled(false);
            double remaining = savedTotalCost - alreadyPaid;
            if (remaining > 0) {
                rbFull.setText(String.format(Locale.getDefault(), "Εξόφληση υπολοίπου (%.2f €)", remaining));
            } else {
                rbFull.setText("Έχει εξοφληθεί πλήρως");
                btnSave.setEnabled(false);
            }
        }

        rgPaymentType.setOnCheckedChangeListener((group, checkedId) -> {
            llPartialContainer.setVisibility(checkedId == R.id.rbPartialPayment ? View.VISIBLE : View.GONE);
        });

        final Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        tvDate.setText(sdf.format(calendar.getTime()));

        View.OnClickListener dateClickListener = v -> new DatePickerDialog(context, (view, year, month, dayOfMonth) -> {
            calendar.set(year, month, dayOfMonth);
            tvDate.setText(sdf.format(calendar.getTime()));
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();

        rlDateContainer.setOnClickListener(dateClickListener);
        tvDate.setOnClickListener(dateClickListener);

        String[] paymentMethods = {"Μετρητά", "POS", "IRIS", "Alpha Bank", "Eurobank", "Τράπεζα Πειραιώς"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, paymentMethods);
        spMethod.setAdapter(adapter);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String totalStr = etTotalAmount.getText().toString().trim();
            if (totalStr.isEmpty()) {
                Toast.makeText(context, "Παρακαλώ συμπληρώστε τη συνολική αξία!", Toast.LENGTH_SHORT).show();
                return;
            }

            double currentTotalCost = Double.parseDouble(totalStr.replace(",", "."));
            if (student.getMaterialCost() != currentTotalCost) {
                student.setMaterialCost(currentTotalCost);
                dbHelper.updateStudentMaterialCost(student.getId(), currentTotalCost);
            }

            double amountToCollectNow = 0.0;
            if (rbPartial.isChecked()) {
                String partialStr = etPartialAmount.getText().toString().trim();
                if (partialStr.isEmpty()) return;
                amountToCollectNow = Double.parseDouble(partialStr.replace(",", "."));
            } else {
                amountToCollectNow = currentTotalCost - alreadyPaid;
            }

            long result = dbHelper.addPaymentForMonth(
                    student.getId(), amountToCollectNow, tvDate.getText().toString(),
                    "Εποπτικό Υλικό", spMethod.getSelectedItem().toString(),
                    "Εποπτικό Υλικό", dbHelper.getCurrentAcademicYear()
            );

            if (result != -1) {
                String newStatus = ((alreadyPaid + amountToCollectNow) >= currentTotalCost) ? "Πληρωμένο" : "Μερική πληρωμή";
                dbHelper.updateStudentMaterialStatus(student.getId(), newStatus);
                student.setMaterialStatus(newStatus);
                Toast.makeText(context, "Η πληρωμή καταχωρήθηκε!", Toast.LENGTH_SHORT).show();

                dialog.dismiss();
                if (onSuccess != null) onSuccess.run();
            }
        });

        dialog.show();
    }
}