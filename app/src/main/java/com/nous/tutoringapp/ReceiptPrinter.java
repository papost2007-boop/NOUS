package com.nous.tutoringapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

public class ReceiptPrinter {

    public static void printReceipt(Context context, Payment payment) {
        PrintManager printManager = (PrintManager) context.getSystemService(Context.PRINT_SERVICE);
        if (printManager == null) {
            Toast.makeText(context, "❌ Η υπηρεσία εκτύπωσης δεν είναι διαθέσιμη.", Toast.LENGTH_SHORT).show();
            return;
        }

        String safeName = payment.getStudentName() != null ? payment.getStudentName().replace(" ", "_") : "Unknown";
        String jobName = "APV_" + safeName + "_" + payment.getPaymentDate().replace("/", "");

        printManager.print(jobName, new PrintDocumentAdapter() {
            @Override
            public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes,
                                 CancellationSignal cancellationSignal, LayoutResultCallback callback, Bundle extras) {
                if (cancellationSignal != null && cancellationSignal.isCanceled()) {
                    callback.onLayoutCancelled();
                    return;
                }
                callback.onLayoutFinished(new PrintDocumentInfo.Builder(jobName)
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(1)
                        .build(), true);
            }

            @Override
            public void onWrite(PageRange[] pages, ParcelFileDescriptor destination,
                                CancellationSignal cancellationSignal, WriteResultCallback callback) {

                if (cancellationSignal != null && cancellationSignal.isCanceled()) {
                    callback.onWriteCancelled();
                    return;
                }

                PdfDocument pdfDocument = new PdfDocument();
                FileOutputStream out = null;

                try {
                    int pageWidth = 595;
                    int pageHeight = 842;
                    PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
                    PdfDocument.Page page = pdfDocument.startPage(pageInfo);

                    Canvas canvas = page.getCanvas();
                    Paint paint = new Paint();
                    paint.setAntiAlias(true);

                    int margin = 20;
                    int y = 50;

                    int colorDark = 0xFF000000;
                    int colorBorder = 0xFF000000;

                    // ==========================================
                    // 1. ΣΤΟΙΧΕΙΑ ΕΚΔΟΤΗ
                    // ==========================================
                    paint.setColor(colorDark);
                    paint.setTextSize(12f);
                    paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                    canvas.drawText("Αποστολόπουλος Παναγιώτης", margin, y, paint);

                    paint.setTextSize(9f);
                    paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                    y += 14;
                    canvas.drawText(" ", margin, y, paint);
                    y += 14;
                    canvas.drawText("", margin, y, paint);
                    y += 14;
                    canvas.drawText("", margin, y, paint);
                    y += 14;
                    canvas.drawText("", margin, y, paint);

                    // ==========================================
                    // 2. ΠΙΝΑΚΑΚΙ ΠΑΡΑΣΤΑΤΙΚΟΥ
                    // ==========================================
                    int boxW = 350;
                    int boxH = 45;
                    int boxX = pageWidth - margin - boxW;
                    int boxY = 40;

                    paint.setStyle(Paint.Style.STROKE);
                    paint.setColor(colorBorder);
                    paint.setStrokeWidth(0.5f);
                    canvas.drawRect(boxX, boxY, boxX + boxW, boxY + boxH, paint);

                    int[] colX = {boxX + 130, boxX + 175, boxX + 225, boxX + 295};
                    for (int x : colX) {
                        canvas.drawLine(x, boxY, x, boxY + boxH, paint);
                    }
                    canvas.drawLine(boxX, boxY + 22, boxX + boxW, boxY + 22, paint);

                    paint.setStyle(Paint.Style.FILL);
                    paint.setTextSize(8f);
                    paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

                    canvas.drawText("ΕΙΔΟΣ ΠΑΡΑΣΤΑΤΙΚΟΥ", boxX + 5, boxY + 14, paint);
                    canvas.drawText("ΣΕΙΡΑ", colX[0] + 5, boxY + 14, paint);
                    canvas.drawText("ΑΡΙΘΜΟΣ", colX[1] + 5, boxY + 14, paint);
                    canvas.drawText("ΗΜΕΡΟΜΗΝΙΑ", colX[2] + 5, boxY + 14, paint);
                    canvas.drawText("ΩΡΑ ΑΠΟΣΤ.", colX[3] + 5, boxY + 14, paint);

                    paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                    // ΑΛΛΑΓΗ 1 & 2: "Απόδειξη Είσπραξης" και κενή η Σειρά (ΑΠΥ)
                    canvas.drawText("Απόδειξη Είσπραξης", boxX + 5, boxY + 36, paint);
                    canvas.drawText("", colX[0] + 5, boxY + 36, paint);
                    canvas.drawText(String.valueOf(payment.getId() + 2300), colX[1] + 5, boxY + 36, paint);
                    canvas.drawText(payment.getPaymentDate(), colX[2] + 5, boxY + 36, paint);

                    // ==========================================
                    // 3. ΣΤΟΙΧΕΙΑ ΣΥΜΒΑΛΛΟΜΕΝΟΥ
                    // ==========================================
                    y = 135;
                    paint.setTextSize(10f);
                    paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                    canvas.drawText("ΣΤΟΙΧΕΙΑ ΣΥΜΒΑΛΛΟΜΕΝΟΥ", margin, y, paint);

                    y += 8;
                    int clientBoxH = 80;
                    paint.setStyle(Paint.Style.STROKE);
                    canvas.drawRect(margin, y, pageWidth - margin, y + clientBoxH, paint);
                    paint.setStyle(Paint.Style.FILL);

                    paint.setTextSize(9f);
                    paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                    String studentName = payment.getStudentName() != null ? payment.getStudentName() : "";

                    int labelX = margin + 5;
                    int colonX = margin + 65;
                    int valueX = margin + 75;
                    int cy = y + 15;

                    String[] leftLabels = {"ΕΠΩΝΥΜΙΑ", "ΕΠΑΓΓΕΛΜΑ", "ΔΙΕΥΘΥΝΣΗ", "ΠΟΛΗ - Τ.Κ.", "ΤΗΛ."};
                    String[] leftValues = {studentName, "", "", "", ""};

                    for (int i = 0; i < leftLabels.length; i++) {
                        canvas.drawText(leftLabels[i], labelX, cy, paint);
                        canvas.drawText(":", colonX, cy, paint);
                        canvas.drawText(leftValues[i], valueX, cy, paint);
                        cy += 13;
                    }

                    int rightLabelX = pageWidth / 2 + 30;
                    int rightColonX = rightLabelX + 45;
                    int rightValueX = rightColonX + 10;
                    int cyR = y + 15;

                    String pelCode = String.format(Locale.getDefault(), "ΠΕΛ%07d", payment.getStudentId());
                    canvas.drawText("ΚΩΔΙΚΟΣ", rightLabelX, cyR, paint);
                    canvas.drawText(":", rightColonX, cyR, paint);
                    canvas.drawText(pelCode, rightValueX, cyR, paint);

                    cyR += 52;
                    canvas.drawText("Α.Φ.Μ.", rightLabelX, cyR, paint);
                    canvas.drawText(":", rightColonX, cyR, paint);
                    cyR += 13;
                    canvas.drawText("Δ.Ο.Υ.", rightLabelX, cyR, paint);
                    canvas.drawText(":", rightColonX, cyR, paint);

                    // ==========================================
                    // 4. ΛΟΙΠΑ ΣΤΟΙΧΕΙΑ
                    // ==========================================
                    y += clientBoxH + 15;
                    paint.setTextSize(9f);
                    paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                    canvas.drawText("ΣΧΕΤΙΚΑ ΠΑΡΑΣΤΑΤΙΚΑ :", margin, y, paint);
                    y += 12;
                    canvas.drawText("ΣΚΟΠΟΣ ΔΙΑΚΙΝΗΣΗΣ :", margin, y, paint);
                    y += 12;
                    canvas.drawText("ΤΡΟΠΟΣ ΠΛΗΡΩΜΗΣ : " + payment.getPaymentMethod(), margin, y, paint);

                    // ΑΛΛΑΓΗ 3: Αφαιρέθηκε το "Άρθρο 27 (Προηγ. 22) του Κώδικα ΦΠΑ"

                    // ==========================================
                    // 5. ΠΙΝΑΚΑΣ ΕΙΔΩΝ
                    // ==========================================
                    y += 20;
                    paint.setStyle(Paint.Style.STROKE);
                    canvas.drawRect(margin, y, pageWidth - margin, y + 20, paint);
                    canvas.drawRect(margin, y + 20, pageWidth - margin, y + 45, paint);

                    int[] tCol = {margin + 65, margin + 215, margin + 240, margin + 275, margin + 335, margin + 395, margin + 465, margin + 515};
                    for(int x : tCol) {
                        canvas.drawLine(x, y, x, y + 45, paint);
                    }
                    paint.setStyle(Paint.Style.FILL);

                    paint.setTextSize(8f);
                    paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

                    canvas.drawText("ΚΩΔΙΚΟΣ", margin + 3, y + 13, paint);
                    canvas.drawText("ΠΕΡΙΓΡΑΦΗ", tCol[0] + 3, y + 13, paint);
                    canvas.drawText("MM", tCol[1] + 3, y + 13, paint);
                    canvas.drawText("ΠΟΣ.", tCol[2] + 3, y + 13, paint);
                    canvas.drawText("ΤΙΜΗ ΜΟΝ.", tCol[3] + 3, y + 13, paint);
                    canvas.drawText("ΑΞΙΑ", tCol[4] + 3, y + 13, paint);
                    canvas.drawText("ΕΚΠΤΩΣΗ", tCol[5] + 3, y + 13, paint);
                    canvas.drawText("ΑΞΙΑ Μ. ΕΚΠΤ.", tCol[6] + 2, y + 13, paint);
                    canvas.drawText("ΦΠΑ%", tCol[7] + 3, y + 13, paint);

                    paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                    y += 35;
                    canvas.drawText("ΥΠΡ0000002", margin + 3, y, paint);

                    String perigrafi = "ΜΑΘΗΜΑΤΑ";
                    String details = (payment.getTargetMonth() != null && !payment.getTargetMonth().isEmpty()) ? payment.getTargetMonth() : payment.getComments();
                    if (details != null && details.toLowerCase().contains("εποπτικ")) {
                        perigrafi = "ΕΠΟΠΤΙΚΟ ΥΛΙΚΟ";
                    }
                    canvas.drawText(perigrafi, tCol[0] + 3, y, paint);

                    paint.setTextAlign(Paint.Align.RIGHT);
                    String formattedAmount = String.format(Locale.getDefault(), "%.2f", payment.getAmount());

                    canvas.drawText("1,000", tCol[3] - 4, y, paint);
                    canvas.drawText(formattedAmount, tCol[4] - 4, y, paint);
                    canvas.drawText(formattedAmount, tCol[5] - 4, y, paint);
                    canvas.drawText("0,00", tCol[6] - 4, y, paint);
                    canvas.drawText(formattedAmount, tCol[7] - 4, y, paint);
                    canvas.drawText("0,00", pageWidth - margin - 4, y, paint);
                    paint.setTextAlign(Paint.Align.LEFT);

                    // ==========================================
                    // 6. ΠΑΡΑΤΗΡΗΣΕΙΣ
                    // ==========================================
                    y += 25;
                    paint.setTextSize(9f);
                    canvas.drawText("Σελίδα :1/1", margin, y, paint);
                    y += 15;
                    if (details == null) details = "";
                    canvas.drawText("ΠΑΡΑΤΗΡΗΣΕΙΣ: " + details, margin, y, paint);
                    // ΑΛΛΑΓΗ 4: Αφαιρέθηκε το "ΕΝΙΑΙΟ ΜΗΧΑΝΟΓΡΑΦΙΚΟ ΕΝΤΥΠΟ ΠΟΛΛΑΠΛΗΣ ΧΡΗΣΗΣ"

                    // ==========================================
                    // 7. ΣΥΝΟΛΑ ΚΑΙ ΦΠΑ
                    // ==========================================
                    y += 30;

                    int fpaBoxW = 220;
                    int fpaBoxH = 45;
                    paint.setStyle(Paint.Style.STROKE);
                    canvas.drawRect(margin, y, margin + fpaBoxW, y + fpaBoxH, paint);
                    canvas.drawLine(margin, y + 15, margin + fpaBoxW, y + 15, paint);
                    canvas.drawLine(margin + 75, y + 15, margin + 75, y + fpaBoxH, paint);
                    canvas.drawLine(margin + 140, y + 15, margin + 140, y + fpaBoxH, paint);
                    paint.setStyle(Paint.Style.FILL);

                    paint.setTextSize(8f);
                    paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                    canvas.drawText("ΑΝΑΛΥΣΗ ΥΠΟΛΟΓΙΣΜΟΥ ΦΠΑ", margin + 5, y - 5, paint);
                    canvas.drawText("ΚΑΘΑΡΗ ΑΞΙΑ", margin + 5, y + 11, paint);
                    canvas.drawText("% Φ.Π.Α.", margin + 80, y + 11, paint);
                    canvas.drawText("ΑΞΙΑ Φ.Π.Α.", margin + 145, y + 11, paint);

                    paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                    paint.setTextAlign(Paint.Align.RIGHT);
                    canvas.drawText(formattedAmount, margin + 70, y + 32, paint);
                    canvas.drawText("0,00", margin + 135, y + 32, paint);
                    canvas.drawText("0,00", margin + fpaBoxW - 5, y + 32, paint);
                    paint.setTextAlign(Paint.Align.LEFT);

                    // ΑΛΛΑΓΗ 5: Καθαρίσαμε όλη την ανάλυση συνόλων και κρατήσαμε μόνο το ΠΛΗΡΩΤΕΟ
                    int totXLabel = pageWidth - margin - 150;
                    int totXValue = pageWidth - margin - 20;

                    // Τοποθετούμε το ΠΛΗΡΩΤΕΟ κάθετα ευθυγραμμισμένο με το κουτάκι του ΦΠΑ (y + 25)
                    int ty = y + 25;

                    paint.setTextAlign(Paint.Align.RIGHT);
                    paint.setTextSize(14f); // Το έκανα λίγο μεγαλύτερο για να ξεχωρίζει περισσότερο!
                    paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                    canvas.drawText("ΠΛΗΡΩΤΕΟ:", totXLabel, ty, paint);
                    canvas.drawText(formattedAmount + " €", totXValue, ty, paint);

                    paint.setTextAlign(Paint.Align.LEFT);

                    pdfDocument.finishPage(page);

                    if (cancellationSignal != null && cancellationSignal.isCanceled()) {
                        callback.onWriteCancelled();
                        return;
                    }

                    out = new FileOutputStream(destination.getFileDescriptor());
                    pdfDocument.writeTo(out);
                    out.flush();
                    callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});

                } catch (Exception e) {
                    e.printStackTrace();
                    callback.onWriteFailed("Σφάλμα δημιουργίας PDF: " + e.getMessage());
                } finally {
                    pdfDocument.close();
                    if (out != null) {
                        try { out.close(); } catch (IOException ignored) {}
                    }
                }
            }
        }, null);
    }
}