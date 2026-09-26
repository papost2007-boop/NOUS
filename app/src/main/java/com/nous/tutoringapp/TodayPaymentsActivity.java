package com.nous.tutoringapp;

import android.app.DatePickerDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TodayPaymentsActivity extends AppCompatActivity {

    RecyclerView rvTodayPayments;
    private Button btnBack, btnPrevDay, btnNextDay;
    private TextView tvSelectedDate;
    private DatabaseHelper dbHelper;

    private Calendar calendar = Calendar.getInstance();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private String currentDateString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_today_payments);

        dbHelper = new DatabaseHelper(this);

        // 1. ΠΡΩΤΑ κάνουμε findViewById
        rvTodayPayments = findViewById(R.id.rvTodayPayments);
        // 2. ΜΕΤΑ θέτουμε τον LayoutManager
        rvTodayPayments.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));

        btnBack = findViewById(R.id.btnBackToMenu);
        btnPrevDay = findViewById(R.id.btnPrevDay);
        btnNextDay = findViewById(R.id.btnNextDay);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        Button btnPrint = findViewById(R.id.btnPrintTodayPayments);

        // Αρχική ημερομηνία: Σήμερα
        updateDateDisplayAndList();

        // ◄ Προηγούμενη Μέρα
        btnPrevDay.setOnClickListener(v -> {
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            updateDateDisplayAndList();
        });

        // ► Επόμενη Μέρα
        btnNextDay.setOnClickListener(v -> {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            updateDateDisplayAndList();
        });

        // 📅 Πάτημα στην ημερομηνία για επιλογή από Ημερολόγιο
        tvSelectedDate.setOnClickListener(v -> {
            DatePickerDialog datePicker = new DatePickerDialog(this,
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, month);
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        updateDateDisplayAndList();
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
            datePicker.show();
        });

        btnBack.setOnClickListener(v -> finish());

        if (btnPrint != null) {
            btnPrint.setOnClickListener(v -> printPaymentsForDate());
        }
    }

    private void updateDateDisplayAndList() {
        currentDateString = dateFormat.format(calendar.getTime());
        tvSelectedDate.setText(currentDateString);

        // 1. Παίρνουμε τα αντικείμενα Payment από τη βάση
        ArrayList<Payment> paymentsList = dbHelper.getPaymentsByDate(currentDateString);

        double dailyTotal = 0.0;

        // 2. Υπολογισμός συνόλου
        if (paymentsList != null) {
            for (Payment p : paymentsList) {
                dailyTotal += p.getAmount();
            }
        }

        // 3. Σύνδεση με τον νέο PaymentAdapter
        PaymentAdapter adapter = new PaymentAdapter(paymentsList);
        rvTodayPayments.setAdapter(adapter);

        // 4. Εμφάνιση του συνόλου
        TextView tvDailyTotalAmount = findViewById(R.id.tvDailyTotalAmount);
        if (tvDailyTotalAmount != null) {
            tvDailyTotalAmount.setText(String.format(Locale.getDefault(), "%.2f €", dailyTotal));
        }
    }

    public void printPaymentsForDate() {
        // 1. Παίρνουμε κατευθείαν τα αντικείμενα από τη βάση
        ArrayList<Payment> paymentsList = dbHelper.getPaymentsByDate(currentDateString);

        if (paymentsList == null || paymentsList.isEmpty()) {
            Toast.makeText(this, "⚠️ Δεν υπάρχουν πληρωμές στις " + currentDateString + " για εκτύπωση!", Toast.LENGTH_SHORT).show();
            return;
        }

        PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
        if (printManager == null) {
            Toast.makeText(this, "❌ Η υπηρεσία εκτύπωσης δεν είναι διαθέσιμη στη συσκευή.", Toast.LENGTH_SHORT).show();
            return;
        }

        String jobName = "Plirwmes_" + currentDateString.replace("/", "_");

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

                    int margin = 36;

                    // 🎨 Χρώματα
                    int colorPrimary = 0xFF1E3A8A;
                    int colorAccent = 0xFF0284C7;
                    int colorDark = 0xFF0F172A;
                    int colorMuted = 0xFF64748B;
                    int colorBgCard = 0xFFF1F5F9;
                    int colorRowAlt = 0xFFF8FAFC;
                    int colorBorder = 0xFFE2E8F0;

                    Map<String, Double> totalsPerMethod = new HashMap<>();
                    double grandTotal = 0.0;
                    double tuitionTotal = 0.0;
                    double materialTotal = 0.0;

                    // 🛡️ ΝΕΟΣ, ΚΑΘΑΡΟΣ ΥΠΟΛΟΓΙΣΜΟΣ (Χωρίς string parsing!)
                    for (Payment p : paymentsList) {
                        double amount = p.getAmount();
                        String method = p.getPaymentMethod();
                        String details = (p.getTargetMonth() != null && !p.getTargetMonth().isEmpty()) ? p.getTargetMonth() : p.getComments();
                        if (details == null) details = "";

                        // Διαχωρισμός Εποπτικών - Διδάκτρων
                        if (details.toLowerCase().contains("εποπτικ")) {
                            materialTotal += amount;
                        } else {
                            tuitionTotal += amount;
                        }

                        totalsPerMethod.put(method, totalsPerMethod.getOrDefault(method, 0.0) + amount);
                        grandTotal += amount;
                    }

                    // ==========================================
                    // 1️⃣ ΚΕΦΑΛΙΔΑ
                    // ==========================================
                    int y = 50;
                    paint.setColor(colorPrimary);
                    paint.setTextSize(18f);
                    paint.setFakeBoldText(true);
                    canvas.drawText("ΦΡΟΝΤΙΣΤΗΡΙΟ «ΝΟΥΣ»", margin, y, paint);

                    String timeNow = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
                    paint.setColor(colorMuted);
                    paint.setTextSize(9.5f);
                    paint.setFakeBoldText(false);
                    paint.setTextAlign(Paint.Align.RIGHT);
                    canvas.drawText("Εκτύπωση: " + timeNow, pageWidth - margin, y - 4, paint);
                    paint.setTextAlign(Paint.Align.LEFT);

                    y += 18;
                    paint.setColor(colorDark);
                    paint.setTextSize(12f);
                    paint.setFakeBoldText(true);
                    canvas.drawText("Ημερήσιο Ταμείο Εισπράξεων: " + currentDateString, margin, y, paint);

                    y += 12;
                    paint.setColor(colorAccent);
                    paint.setStrokeWidth(2f);
                    canvas.drawLine(margin, y, pageWidth - margin, y, paint);
                    paint.setStrokeWidth(0);

                    // ==========================================
                    // 2️⃣ ΚΑΡΤΑ ΣΥΝΟΨΗΣ (DASHBOARD)
                    // ==========================================
                    y += 18;
                    int cardHeight = 72;
                    paint.setColor(colorBgCard);
                    RectF cardRect = new RectF(margin, y, pageWidth - margin, y + cardHeight);
                    canvas.drawRoundRect(cardRect, 8, 8, paint);

                    paint.setStyle(Paint.Style.STROKE);
                    paint.setColor(colorBorder);
                    paint.setStrokeWidth(1f);
                    canvas.drawRoundRect(cardRect, 8, 8, paint);
                    paint.setStyle(Paint.Style.FILL);

                    paint.setColor(colorPrimary);
                    paint.setTextSize(10f);
                    paint.setFakeBoldText(true);
                    canvas.drawText("ΓΕΝΙΚΟ ΣΥΝΟΛΟ", margin + 16, y + 26, paint);

                    paint.setTextSize(20f);
                    canvas.drawText(String.format(Locale.getDefault(), "%.2f €", grandTotal), margin + 16, y + 54, paint);

                    paint.setColor(colorDark);
                    paint.setTextSize(9.5f);
                    paint.setFakeBoldText(false);
                    canvas.drawText(String.format(Locale.getDefault(), "• Δίδακτρα: %.2f €", tuitionTotal), margin + 160, y + 30, paint);
                    canvas.drawText(String.format(Locale.getDefault(), "• Εποπτικό: %.2f €", materialTotal), margin + 160, y + 50, paint);

                    int mX = margin + 320;
                    int mY = y + 24;
                    paint.setTextSize(9f);
                    for (Map.Entry<String, Double> entry : totalsPerMethod.entrySet()) {
                        if (mY > y + cardHeight - 8) break;
                        canvas.drawText(String.format(Locale.getDefault(), "%s: %.2f €", entry.getKey(), entry.getValue()), mX, mY, paint);
                        mY += 15;
                    }

                    // ==========================================
                    // 3️⃣ ΠΙΝΑΚΑΣ ΣΥΝΑΛΛΑΓΩΝ
                    // ==========================================
                    y += cardHeight + 22;

                    int colIndex = margin + 8;
                    int colName = margin + 32;
                    int colDetails = margin + 240;
                    int colMethod = margin + 370;
                    int colAmount = pageWidth - margin - 10;

                    paint.setColor(colorPrimary);
                    canvas.drawRect(margin, y, pageWidth - margin, y + 22, paint);

                    paint.setColor(Color.WHITE);
                    paint.setTextSize(9f);
                    paint.setFakeBoldText(true);
                    canvas.drawText("#", colIndex, y + 15, paint);
                    canvas.drawText("Μαθητής", colName, y + 15, paint);
                    canvas.drawText("Περιγραφή", colDetails, y + 15, paint);
                    canvas.drawText("Τρόπος", colMethod, y + 15, paint);
                    paint.setTextAlign(Paint.Align.RIGHT);
                    canvas.drawText("Ποσό", colAmount, y + 15, paint);
                    paint.setTextAlign(Paint.Align.LEFT);

                    y += 22;

                    paint.setTextSize(9f);
                    int rowH = 20;
                    int index = 1;

                    // 🛡️ ΝΕΟΣ ΒΡΟΧΟΣ ΖΩΓΡΑΦΙΚΗΣ (Χρησιμοποιεί απευθείας το Payment)
                    for (Payment p : paymentsList) {
                        if (y > pageHeight - margin - 20) break;

                        if (index % 2 == 0) {
                            paint.setColor(colorRowAlt);
                            canvas.drawRect(margin, y, pageWidth - margin, y + rowH, paint);
                        }

                        paint.setColor(colorDark);
                        paint.setFakeBoldText(false);
                        canvas.drawText(String.valueOf(index), colIndex, y + 14, paint);

                        String displayName = p.getStudentName();
                        if (displayName != null && displayName.length() > 30) displayName = displayName.substring(0, 28) + "...";
                        canvas.drawText(displayName != null ? displayName : "-", colName, y + 14, paint);

                        String details = (p.getTargetMonth() != null && !p.getTargetMonth().isEmpty()) ? p.getTargetMonth() : p.getComments();
                        canvas.drawText(details != null ? details : "-", colDetails, y + 14, paint);
                        canvas.drawText(p.getPaymentMethod(), colMethod, y + 14, paint);

                        paint.setFakeBoldText(true);
                        paint.setTextAlign(Paint.Align.RIGHT);
                        canvas.drawText(String.format(Locale.getDefault(), "%.2f €", p.getAmount()), colAmount, y + 14, paint);
                        paint.setTextAlign(Paint.Align.LEFT);

                        paint.setColor(colorBorder);
                        paint.setStrokeWidth(0.5f);
                        canvas.drawLine(margin, y + rowH, pageWidth - margin, y + rowH, paint);
                        paint.setStrokeWidth(0);

                        y += rowH;
                        index++;
                    }

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
                    callback.onWriteFailed(e.getMessage() != null ? e.getMessage() : "Σφάλμα εκτύπωσης");
                } finally {
                    pdfDocument.close();
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException ignored) {}
                    }
                }
            }
        }, null);
    }
}