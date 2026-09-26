package com.nous.tutoringapp;

import android.app.Dialog;
import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import java.util.ArrayList;

public class StudentNotesManager {

    public static void showDialog(Context context, DatabaseHelper dbHelper, Student student) {
        if (student == null) return;

        Dialog notesDialog = new Dialog(context);
        notesDialog.setContentView(R.layout.dialog_student_notes);
        if (notesDialog.getWindow() != null) {
            notesDialog.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        }

        Spinner spinnerCategory = notesDialog.findViewById(R.id.spinnerNoteCategory);
        EditText etText = notesDialog.findViewById(R.id.etNoteText);
        Button btnAdd = notesDialog.findViewById(R.id.btnAddNote);
        LinearLayout container = notesDialog.findViewById(R.id.llNotesContainer);
        Button btnClose = notesDialog.findViewById(R.id.btnCloseNotes);

        String[] categories = {
                "💳 Επικοινωνία για Δίδακτρα (Γονέας)",
                "📖 Αδιάβαστος / Αμελής",
                "📞 Τηλεφωνική Επικοινωνία",
                "⚠️ Συστάσεις / Συμπεριφορά",
                "💬 Γενική Σημείωση"
        };

        if (spinnerCategory != null) {
            ArrayAdapter<String> catAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, categories);
            catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerCategory.setAdapter(catAdapter);
        }

        loadNotesIntoContainer(context, dbHelper, student.getId(), container);

        if (btnAdd != null) {
            btnAdd.setOnClickListener(v -> {
                String cat = (spinnerCategory != null && spinnerCategory.getSelectedItem() != null) ?
                        spinnerCategory.getSelectedItem().toString() : "💬 Γενική Σημείωση";
                String text = (etText != null) ? etText.getText().toString().trim() : "";

                long id = dbHelper.addStudentNote(student.getId(), cat, text);
                if (id != -1) {
                    Toast.makeText(context, "✅ Η καταγραφή ολοκληρώθηκε!", Toast.LENGTH_SHORT).show();
                    if (etText != null) etText.setText("");
                    loadNotesIntoContainer(context, dbHelper, student.getId(), container);
                } else {
                    Toast.makeText(context, "❌ Σφάλμα αποθήκευσης!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnClose != null) btnClose.setOnClickListener(v -> notesDialog.dismiss());
        notesDialog.show();
    }

    private static void loadNotesIntoContainer(Context context, DatabaseHelper dbHelper, int studentId, LinearLayout container) {
        if (container == null) return;
        container.removeAllViews();

        ArrayList<StudentNote> notes = dbHelper.getNotesForStudent(studentId);

        if (notes.isEmpty()) {
            TextView tvEmpty = new TextView(context);
            tvEmpty.setText("Δεν υπάρχει καταγεγραμμένη επικοινωνία/σημείωση.");
            tvEmpty.setTextColor(0xFF94A3B8);
            tvEmpty.setPadding(10, 20, 10, 20);
            container.addView(tvEmpty);
            return;
        }

        for (StudentNote note : notes) {
            LinearLayout itemLayout = new LinearLayout(context);
            itemLayout.setOrientation(LinearLayout.VERTICAL);
            itemLayout.setBackgroundColor(0xFF0F172A);
            itemLayout.setPadding(16, 16, 16, 16);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 12);
            itemLayout.setLayoutParams(params);

            TextView tvHeader = new TextView(context);
            tvHeader.setText(note.getDate() + " — " + note.getCategory());
            tvHeader.setTextSize(12);
            tvHeader.setTypeface(null, android.graphics.Typeface.BOLD);

            if (note.getCategory().contains("Δίδακτρα")) {
                tvHeader.setTextColor(0xFFFBBF24);
            } else if (note.getCategory().contains("Αδιάβαστος")) {
                tvHeader.setTextColor(0xFFF87171);
            } else {
                tvHeader.setTextColor(0xFF60A5FA);
            }

            itemLayout.addView(tvHeader);

            if (note.getNoteText() != null && !note.getNoteText().isEmpty()) {
                TextView tvText = new TextView(context);
                tvText.setText(note.getNoteText());
                tvText.setTextColor(0xFFFFFFFF);
                tvText.setTextSize(13);
                tvText.setPadding(0, 6, 0, 0);
                itemLayout.addView(tvText);
            }

            itemLayout.setOnLongClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("🗑️ Διαγραφή Σημείωσης")
                        .setMessage("Θέλετε να διαγράψετε αυτή τη σημείωση;")
                        .setPositiveButton("Διαγραφή", (dialog, which) -> {
                            dbHelper.deleteStudentNote(note.getId());
                            loadNotesIntoContainer(context, dbHelper, studentId, container);
                        })
                        .setNegativeButton("Ακύρωση", null)
                        .show();
                return true;
            });

            container.addView(itemLayout);
        }
    }
}