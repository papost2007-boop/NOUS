package com.nous.tutoringapp;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

public class AutoBackupWorker extends Worker {

    private static final String TAG = "AutoBackupWorker";

    public AutoBackupWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();

        try {
            // Εξετάζουμε αν υπάρχει η βάση δεδομένων
            File dbFile = context.getDatabasePath("frontistirio.db");

            Log.d(TAG, "DB Path: " + dbFile.getAbsolutePath() + " | Exists: " + dbFile.exists());

            if (!dbFile.exists()) {
                showToast("⚠️ Η βάση frontistirio.db δεν βρέθηκε!");
                Log.e(TAG, "Database file does not exist at path: " + dbFile.getAbsolutePath());
                return Result.failure();
            }

            // 1. Φάκελος προορισμού για τα backup
            File backupDir = new File(context.getExternalFilesDir(null), "AutoBackups");
            if (!backupDir.exists()) {
                boolean created = backupDir.mkdirs();
                Log.d(TAG, "Backup directory created: " + created);
            }

            // 2. Δημιουργία νέου αρχείου backup με σφραγίδα ώρας
            String timeStamp = new SimpleDateFormat("yyyy_MM_dd_HHmm", Locale.getDefault()).format(new Date());
            File backupFile = new File(backupDir, "auto_backup_" + timeStamp + ".db");

            // Αντιγραφή αρχείου
            InputStream in = new FileInputStream(dbFile);
            OutputStream out = new FileOutputStream(backupFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }

            out.flush();
            out.close();
            in.close();

            Log.d(TAG, "Backup successful! Saved at: " + backupFile.getAbsolutePath());
            showToast("✅ Αυτόματο Backup ολοκληρώθηκε!");

            // 🧹 3. Καθαρισμός: Διατήρηση ΜΟΝΟ των τελευταίων 4 αρχείων
            cleanOldBackups(backupDir, 4);

            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "Backup failed with exception: ", e);
            showToast("❌ Σφάλμα κατά το Backup: " + e.getMessage());
            return Result.retry();
        }
    }
    private void cleanOldBackups(File backupDir, int maxFiles) {
        File[] files = backupDir.listFiles((dir, name) -> name.startsWith("auto_backup_") && name.endsWith(".db"));

        if (files != null && files.length > maxFiles) {
            Arrays.sort(files, (f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()));
            int filesToDelete = files.length - maxFiles;
            for (int i = 0; i < filesToDelete; i++) {
                files[i].delete();
            }
        }
    }

    private void showToast(String message) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show()
        );
    }
    // 🎯 Μέθοδος για εξαγωγή και διαμοιρασμό (Drive, Gmail, Viber κλπ)

}