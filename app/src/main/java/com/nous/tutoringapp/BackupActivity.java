package com.nous.tutoringapp;

import android.app.Activity;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Η οθόνη διαχείρισης Αντιγράφων Ασφαλείας (Backup & Restore).
 * Χρησιμοποιεί το Storage Access Framework (SAF) για απόλυτη συμβατότητα με Android 8+.
 */
public class BackupActivity extends AppCompatActivity {

    private Button btnExport, btnImport, btnBack;
    private static final String DB_NAME = "frontistirio.db";

    // Launcher για τη δημιουργία/εξαγωγή αρχείου (Save file picker)
    private final ActivityResultLauncher<Intent> exportLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        exportDatabaseToUri(uri);
                    }
                }
            });

    // Launcher για την επιλογή/εισαγωγή αρχείου (Open file picker)
    private final ActivityResultLauncher<Intent> importLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        confirmAndImportDatabase(uri);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup);

        btnExport = findViewById(R.id.btnExportBackup);
        btnImport = findViewById(R.id.btnImportBackup);
        btnBack = findViewById(R.id.btnBackupBack);

        // 📤 Ενέργεια Εξαγωγής (Αποστολή σε Drive, Gmail, Viber κλπ)
        btnExport.setOnClickListener(v -> shareDatabaseBackup());

        // 📥 Ενέργεια Εισαγωγής
        btnImport.setOnClickListener(v -> openImportPicker());

        // 🚪 Επιστροφή
        btnBack.setOnClickListener(v -> finish());
    }
    private void shareDatabaseBackup() {
        try {
            java.io.File dbFile = getDatabasePath("frontistirio.db");

            if (!dbFile.exists()) {
                Toast.makeText(this, "⚠️ Η βάση δεδομένων δεν βρέθηκε!", Toast.LENGTH_SHORT).show();
                return;
            }

            String timeStamp = new java.text.SimpleDateFormat("yyyy_MM_dd_HHmm", java.util.Locale.getDefault()).format(new java.util.Date());
            String backupFileName = "Backup_Frontistirio_" + timeStamp + ".db";
            java.io.File cacheBackupFile = new java.io.File(getCacheDir(), backupFileName);

            java.io.FileInputStream in = new java.io.FileInputStream(dbFile);
            java.io.FileOutputStream out = new java.io.FileOutputStream(cacheBackupFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            out.flush();
            out.close();
            in.close();

            android.net.Uri fileUri = androidx.core.content.FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".provider",
                    cacheBackupFile
            );

            android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
            shareIntent.setType("application/octet-stream");
            shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Αντίγραφο Ασφαλείας Φροντιστηρίου (" + timeStamp + ")");
            shareIntent.putExtra(android.content.Intent.EXTRA_STREAM, fileUri);
            shareIntent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(android.content.Intent.createChooser(shareIntent, "Επιλέξτε τρόπο εξαγωγής backup:"));

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "❌ Σφάλμα κατά την εξαγωγή: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Ανοίγει το παράθυρο διαλόγου του Android για επιλογή τοποθεσίας αποθήκευσης.
     */
    private void openExportPicker() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(new Date());
        String defaultFileName = "NOUS_backup_" + timeStamp + ".db";

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        intent.putExtra(Intent.EXTRA_TITLE, defaultFileName);

        exportLauncher.launch(intent);
    }

    /**
     * Ανοίγει το παράθυρο διαλόγου του Android για επιλογή αρχείου backup (.db).
     */
    private void openImportPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");

        importLauncher.launch(intent);
    }

    /**
     * Αντιγράφει τη βάση δεδομένων της εφαρμογής στην επιλεγμένη τοποθεσία (Uri).
     */
    private void exportDatabaseToUri(Uri targetUri) {
        File currentDb = getDatabasePath(DB_NAME);

        if (!currentDb.exists()) {
            Toast.makeText(this, "❌ Δεν βρέθηκε βάση δεδομένων για εξαγωγή!", Toast.LENGTH_SHORT).show();
            return;
        }

        try (InputStream in = new FileInputStream(currentDb);
             OutputStream out = getContentResolver().openOutputStream(targetUri)) {

            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            out.flush();

            Toast.makeText(this, "✅ Το αντίγραφο ασφαλείας δημιουργήθηκε επιτυχώς!", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "❌ Σφάλμα κατά την εξαγωγή: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Εμφανίζει παράθυρο επιβεβαίωσης πριν αντικατασταθεί η τρέχουσα βάση δεδομένων.
     */
    private void confirmAndImportDatabase(Uri sourceUri) {
        new AlertDialog.Builder(this)
                .setTitle("⚠️ Προσοχή: Επαναφορά Βάσης")
                .setMessage("Η εισαγωγή αυτού του αρχείου θα αντικαταστήσει ΟΛΟΥΣ τους τρέχοντες μαθητές και τις πληρωμές σας.\n\nΘέλετε να συνεχίσετε;")
                .setPositiveButton("Ναι, Επαναφορά", (dialog, which) -> importDatabaseFromUri(sourceUri))
                .setNegativeButton("Ακύρωση", null)
                .show();
    }

    /**
     * Εισάγει τη βάση δεδομένων και εκτελεί αυτόματες επιδιορθώσεις σε πίνακες/στήλες
     * ώστε να μην λείπει καμία στήλη από παλιά backup.
     */
    private void importDatabaseFromUri(Uri sourceUri) {
        File currentDb = getDatabasePath(DB_NAME);

        // 1. Κλείσιμο υπαρχουσών συνδέσεων
        try {
            DatabaseHelper oldDb = new DatabaseHelper(this);
            oldDb.close();
        } catch (Exception ignored) {}

        try {
            // 2. Αντιγραφή του Backup αρχείου
            try (InputStream in = getContentResolver().openInputStream(sourceUri);
                 OutputStream out = new FileOutputStream(currentDb, false)) {

                if (in == null) {
                    Toast.makeText(this, "❌ Αδυναμία ανάγνωσης του αρχείου!", Toast.LENGTH_SHORT).show();
                    return;
                }

                byte[] buffer = new byte[1024];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
                out.flush();
            }

            // 🎯 3. ΕΠΙΔΙΟΡΘΩΣΗ ΔΟΜΗΣ & ΔΕΔΟΜΕΝΩΝ (FIX MISSING COLUMNS & NULL VALUES)
            SQLiteDatabase db = null;
            try {
                db = SQLiteDatabase.openDatabase(currentDb.getPath(), null, SQLiteDatabase.OPEN_READWRITE);

                // Ρύθμιση version στο 7 για να ταιριάζει με τον DatabaseHelper
                db.setVersion(7);

                // 🎯 Προσθήκη στήλης direction στους μαθητές αν λείπει
                try {
                    db.execSQL("ALTER TABLE students ADD COLUMN direction TEXT DEFAULT ''");
                } catch (Exception ignored) {}

                // 🎯 Προσθήκη στήλης is_deleted στους μαθητές αν λείπει
                try {
                    db.execSQL("ALTER TABLE students ADD COLUMN is_deleted INTEGER DEFAULT 0");
                } catch (Exception ignored) {}

                // 🎯 ΠΡΟΣΘΗΚΗ ΣΤΗΛΗΣ is_final_settlement ΣΤΙΣ ΠΛΗΡΩΜΕΣ
                try {
                    db.execSQL("ALTER TABLE payments ADD COLUMN is_final_settlement INTEGER DEFAULT 0");
                } catch (Exception ignored) {}

                // 🎯 Προσθήκη target_month & academic_year στις πληρωμές αν λείπουν
                try {
                    db.execSQL("ALTER TABLE payments ADD COLUMN target_month TEXT NOT NULL DEFAULT ''");
                    db.execSQL("ALTER TABLE payments ADD COLUMN academic_year TEXT NOT NULL DEFAULT ''");
                } catch (Exception ignored) {}

                // 🎯 Καθαρισμός NULL τιμών
                db.execSQL("UPDATE students SET is_deleted = 0 WHERE is_deleted IS NULL");
                db.execSQL("UPDATE students SET direction = '' WHERE direction IS NULL");
                db.execSQL("UPDATE payments SET is_final_settlement = 0 WHERE is_final_settlement IS NULL");

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (db != null && db.isOpen()) {
                    db.close();
                }
            }

            // 🎯 4. ΕΛΕΓΧΟΣ: Επαλήθευση εγγραφών
            DatabaseHelper checkDb = new DatabaseHelper(this);
            int studentCount = checkDb.getStudentsCount();
            checkDb.close();

            if (studentCount > 0) {
                Toast.makeText(this, "🎉 Επαναφορά επιτυχής! Βρέθηκαν " + studentCount + " μαθητές!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "⚠️ Το backup διαβάστηκε, αλλά δεν βρέθηκαν ενεργοί μαθητές.", Toast.LENGTH_LONG).show();
            }

            // 🎯 5. ΕΠΑΝΕΚΚΙΝΗΣΗ ΕΠΑΡΜΟΓΗΣ
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "❌ Σφάλμα κατά την επαναφορά: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}