package com.nous.tutoringapp;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class PaymentHistoryManager {

    // Προσθέσαμε το Runnable onCloseAction στο τέλος
    public static void showDialog(Context context, DatabaseHelper dbHelper, Student student, Runnable onCloseAction) {
        if (student == null) return;

        Dialog historyDialog = new Dialog(context);
        historyDialog.setContentView(R.layout.dialog_payment_history);
        if (historyDialog.getWindow() != null) {
            historyDialog.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        }

        LinearLayout container = historyDialog.findViewById(R.id.llHistoryContainer);
        Button btnClose = historyDialog.findViewById(R.id.btnHistoryClose);

        loadPaymentsIntoContainer(context, dbHelper, student.getId(), container);

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> historyDialog.dismiss());
        }

        // 🎯 ΑΥΤΟ ΕΙΝΑΙ ΤΟ ΜΥΣΤΙΚΟ: Όταν το παράθυρο κλείσει (με οποιονδήποτε τρόπο), τρέχει το callback!
        historyDialog.setOnDismissListener(dialog -> {
            if (onCloseAction != null) {
                onCloseAction.run();
            }
        });

        historyDialog.show();
    }

    private static void loadPaymentsIntoContainer(Context context, DatabaseHelper dbHelper, int studentId, LinearLayout container) {
        if (container == null) return;
        container.removeAllViews();
        ArrayList<Payment> payments = dbHelper.getPaymentsForStudent(studentId);

        if (payments == null || payments.isEmpty()) {
            TextView tvEmpty = new TextView(context);
            tvEmpty.setText("Δεν υπάρχουν καταγεγραμμένες πληρωμές.");
            tvEmpty.setTextSize(14);
            tvEmpty.setTextColor(0xFF94A3B8);
            tvEmpty.setPadding(0, 20, 0, 20);
            container.addView(tvEmpty);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(context);
        for (Payment p : payments) {
            View row = inflater.inflate(R.layout.item_payment, container, false);

            TextView tvComments = row.findViewById(R.id.tvItemComments);
            TextView tvDate = row.findViewById(R.id.tvItemDate);
            TextView tvAmount = row.findViewById(R.id.tvItemAmount);
            TextView tvMethod = row.findViewById(R.id.tvItemMethod);

            if (tvComments != null) {
                tvComments.setText((p.getComments() == null || p.getComments().isEmpty()) ? "Δίδακτρα" : p.getComments());
            }
            if (tvDate != null) tvDate.setText(p.getPaymentDate());
            if (tvAmount != null) tvAmount.setText(String.format(Locale.getDefault(), "+%.2f €", p.getAmount()));

            if (tvMethod != null) {
                String method = p.getPaymentMethod() != null ? p.getPaymentMethod() : "Μετρητά";
                tvMethod.setText(method);

                int textColor;
                int bgColor;

                switch (method) {
                    case "POS": textColor = 0xFF60A5FA; bgColor = 0xFF1E3A8A; break;
                    case "IRIS": textColor = 0xFFF43F5E; bgColor = 0xFF881337; break;
                    case "Alpha Bank": textColor = 0xFF38BDF8; bgColor = 0xFF0C4A6E; break;
                    case "Eurobank": textColor = 0xFFEF4444; bgColor = 0xFF7F1D1D; break;
                    case "Τράπεζα Πειραιώς": textColor = 0xFFFBBF24; bgColor = 0xFF78350F; break;
                    default: textColor = 0xFF34D399; bgColor = 0xFF064E3B; break;
                }

                tvMethod.setTextColor(textColor);
                tvMethod.setBackgroundResource(R.drawable.badge_bg);
                if (tvMethod.getBackground() != null) {
                    tvMethod.getBackground().setColorFilter(bgColor, android.graphics.PorterDuff.Mode.SRC_IN);
                }
            }

            row.setOnLongClickListener(v -> {
                CharSequence[] options = new CharSequence[]{"🖨️ Εκτύπωση Απόδειξης", "✏️ Επεξεργασία", "🗑️ Διαγραφή"};

                new AlertDialog.Builder(context)
                        .setTitle("Επιλογές Πληρωμής (" + p.getAmount() + " €)")
                        .setItems(options, (dialog, which) -> {
                            if (which == 0) {
                                String realName = dbHelper.getStudentNameById(studentId);
                                p.setStudentName(realName);
                                ReceiptPrinter.printReceipt(context, p);
                            } else if (which == 1) {
                                openEditPaymentDialog(context, dbHelper, studentId, p, container);
                            } else if (which == 2) {
                                new AlertDialog.Builder(context)
                                        .setTitle("🗑️ Διαγραφή Πληρωμής")
                                        .setMessage("Θέλετε σίγουρα να διαγράψετε αυτή την πληρωμή;\n\nΠοσό: " + p.getAmount() + " € (" + p.getPaymentMethod() + ")")
                                        .setPositiveButton("Ναι, Διαγραφή", (d, w) -> {
                                            dbHelper.deletePayment(p.getId());
                                            Toast.makeText(context, "✅ Η πληρωμή διαγράφηκε!", Toast.LENGTH_SHORT).show();
                                            loadPaymentsIntoContainer(context, dbHelper, studentId, container);
                                        })
                                        .setNegativeButton("Άκυρο", null)
                                        .show();
                            }
                        })
                        .show();
                return true;
            });
            container.addView(row);
        }
    }

    private static void openEditPaymentDialog(Context context, DatabaseHelper dbHelper, int studentId, Payment payment, LinearLayout container) {
        Dialog editDialog = new Dialog(context);
        editDialog.setContentView(R.layout.dialog_add_payment);
        if (editDialog.getWindow() != null) {
            editDialog.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        }

        EditText etAmount = editDialog.findViewById(R.id.etPayAmount);
        TextView tvPayDate = editDialog.findViewById(R.id.tvPayDate);
        Spinner spinnerMethod = editDialog.findViewById(R.id.spinnerPayMethod);
        EditText etComments = editDialog.findViewById(R.id.etPayComments);
        CheckBox cbFinalSettlement = editDialog.findViewById(R.id.cbFinalSettlement);
        Button btnSubmit = editDialog.findViewById(R.id.btnSubmitPayment);
        Button btnCancel = editDialog.findViewById(R.id.btnCancelPayment);

        if (etAmount != null) etAmount.setText(String.valueOf(payment.getAmount()));
        if (etComments != null) etComments.setText(payment.getComments());
        if (cbFinalSettlement != null) cbFinalSettlement.setChecked(false);

        if (tvPayDate != null) {
            tvPayDate.setText(payment.getPaymentDate());
            tvPayDate.setOnClickListener(v -> {
                Calendar calendar = Calendar.getInstance();
                new android.app.DatePickerDialog(context, (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    tvPayDate.setText(sdf.format(calendar.getTime()));
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
            });
        }

        String[] methods = {"Μετρητά", "POS", "IRIS", "Alpha Bank", "Eurobank", "Τράπεζα Πειραιώς"};
        if (spinnerMethod != null) {
            ArrayAdapter<String> methodAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, methods);
            methodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerMethod.setAdapter(methodAdapter);
            for (int i = 0; i < methods.length; i++) {
                if (methods[i].equalsIgnoreCase(payment.getPaymentMethod())) {
                    spinnerMethod.setSelection(i);
                    break;
                }
            }
        }

        if (btnSubmit != null) {
            btnSubmit.setText("Αποθήκευση Αλλαγών");
            btnSubmit.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF3B82F6));
            btnSubmit.setOnClickListener(v -> {
                String amtStr = (etAmount != null) ? etAmount.getText().toString().trim().replace(",", ".") : "";
                String comments = (etComments != null) ? etComments.getText().toString().trim() : "";
                String method = (spinnerMethod != null && spinnerMethod.getSelectedItem() != null)
                        ? spinnerMethod.getSelectedItem().toString() : "Μετρητά";
                String date = (tvPayDate != null) ? tvPayDate.getText().toString() : payment.getPaymentDate();
                int isFinal = (cbFinalSettlement != null && cbFinalSettlement.isChecked()) ? 1 : 0;

                if (amtStr.isEmpty()) {
                    Toast.makeText(context, "❌ Το ποσό είναι υποχρεωτικό!", Toast.LENGTH_SHORT).show();
                    return;
                }

                double newAmount;
                try {
                    newAmount = Double.parseDouble(amtStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(context, "❌ Μη έγκυρο ποσό!", Toast.LENGTH_SHORT).show();
                    return;
                }

                boolean updated = dbHelper.updatePayment(payment.getId(), newAmount, date, comments, method, isFinal);
                if (updated) {
                    Toast.makeText(context, "✅ Η πληρωμή ενημερώθηκε επιτυχώς!", Toast.LENGTH_SHORT).show();
                    editDialog.dismiss();
                    loadPaymentsIntoContainer(context, dbHelper, studentId, container);
                } else {
                    Toast.makeText(context, "❌ Σφάλμα ενημέρωσης: " + dbHelper.getLastError(), Toast.LENGTH_LONG).show();
                }
            });
        }

        if (btnCancel != null) btnCancel.setOnClickListener(v -> editDialog.dismiss());
        editDialog.show();
    }
}