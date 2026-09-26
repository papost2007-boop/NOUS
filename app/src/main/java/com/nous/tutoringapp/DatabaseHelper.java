package com.nous.tutoringapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "frontistirio.db";
    private static final int DATABASE_VERSION = 10;

    private static final String KEY_DIRECTION = "direction";

    // Πίνακας Μαθητών
    private static final String TABLE_STUDENTS = "students";
    private static final String KEY_ID = "id";
    private static final String KEY_FIRST_NAME = "first_name";
    private static final String KEY_LAST_NAME = "last_name";
    private static final String KEY_MONTHLY_FEE = "monthly_fee";
    private static final String KEY_GRADE = "grade";
    private static final String KEY_MATERIAL_STATUS = "material_status";
    private static final String KEY_MATERIAL_COST = "material_cost";

    // Πίνακας Πληρωμών
    private static final String TABLE_PAYMENTS = "payments";
    private static final String KEY_PAY_ID = "pay_id";
    private static final String KEY_PAY_STUDENT_ID = "student_id";
    private static final String KEY_PAY_AMOUNT = "amount";
    private static final String KEY_PAY_DATE = "payment_date";
    private static final String KEY_PAY_COMMENTS = "comments";
    private static final String KEY_PAY_METHOD = "payment_method";

    // Πεδία για το Σύστημα Μηνών & Σεζόν
    private static final String KEY_PAY_TARGET_MONTH = "target_month";
    private static final String KEY_PAY_ACADEMIC_YEAR = "academic_year";

    // Πίνακας Σημειώσεων / Επικοινωνίας
    private static final String TABLE_NOTES = "student_notes";
    private static final String KEY_NOTE_ID = "note_id";
    private static final String KEY_NOTE_STUDENT_ID = "student_id";
    private static final String KEY_NOTE_DATE = "note_date";
    private static final String KEY_NOTE_CATEGORY = "category";
    private static final String KEY_NOTE_TEXT = "note_text";

    private String lastError = "Άγνωστο σφάλμα";
    private final Context mContext;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.mContext = context;
    }

    public String getLastError() {
        return lastError;
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_STUDENTS_TABLE = "CREATE TABLE " + TABLE_STUDENTS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_FIRST_NAME + " TEXT NOT NULL,"
                + KEY_LAST_NAME + " TEXT NOT NULL,"
                + KEY_MONTHLY_FEE + " REAL NOT NULL,"
                + KEY_GRADE + " TEXT NOT NULL,"
                + KEY_MATERIAL_STATUS + " TEXT,"
                + KEY_DIRECTION + " TEXT DEFAULT '',"
                + KEY_MATERIAL_COST + " REAL DEFAULT 0.0," // 🎯 ΝΕΑ ΣΤΗΛΗ
                + "is_deleted INTEGER DEFAULT 0)";
        db.execSQL(CREATE_STUDENTS_TABLE);

        String CREATE_PAYMENTS_TABLE = "CREATE TABLE " + TABLE_PAYMENTS + "("
                + KEY_PAY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_PAY_STUDENT_ID + " INTEGER NOT NULL,"
                + KEY_PAY_AMOUNT + " REAL NOT NULL,"
                + KEY_PAY_DATE + " TEXT NOT NULL,"
                + KEY_PAY_COMMENTS + " TEXT,"
                + KEY_PAY_METHOD + " TEXT NOT NULL DEFAULT 'Μετρητά',"
                + KEY_PAY_TARGET_MONTH + " TEXT NOT NULL DEFAULT '',"
                + KEY_PAY_ACADEMIC_YEAR + " TEXT NOT NULL DEFAULT '',"
                + "is_final_settlement INTEGER DEFAULT 0,"
                + "FOREIGN KEY(" + KEY_PAY_STUDENT_ID + ") REFERENCES " + TABLE_STUDENTS + "(" + KEY_ID + ") ON DELETE CASCADE)";
        db.execSQL(CREATE_PAYMENTS_TABLE);

        String CREATE_NOTES_TABLE = "CREATE TABLE " + TABLE_NOTES + "("
                + KEY_NOTE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_NOTE_STUDENT_ID + " INTEGER NOT NULL,"
                + KEY_NOTE_DATE + " TEXT NOT NULL,"
                + KEY_NOTE_CATEGORY + " TEXT NOT NULL,"
                + KEY_NOTE_TEXT + " TEXT NOT NULL,"
                + "FOREIGN KEY(" + KEY_NOTE_STUDENT_ID + ") REFERENCES " + TABLE_STUDENTS + "(" + KEY_ID + ") ON DELETE CASCADE)";
        db.execSQL(CREATE_NOTES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 4) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_PAYMENTS + " ADD COLUMN " + KEY_PAY_TARGET_MONTH + " TEXT NOT NULL DEFAULT ''");
                db.execSQL("ALTER TABLE " + TABLE_PAYMENTS + " ADD COLUMN " + KEY_PAY_ACADEMIC_YEAR + " TEXT NOT NULL DEFAULT ''");
            } catch (Exception ignored) {}
        }
        if (oldVersion < 5) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_PAYMENTS + " ADD COLUMN is_final_settlement INTEGER DEFAULT 0");
            } catch (Exception ignored) {}
        }
        if (oldVersion < 8) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_STUDENTS + " ADD COLUMN " + KEY_DIRECTION + " TEXT DEFAULT ''");
            } catch (Exception ignored) {}
        }
        // 🎯 Αναβάθμιση για το κόστος υλικού
        if (oldVersion < 9) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_STUDENTS + " ADD COLUMN " + KEY_MATERIAL_COST + " REAL DEFAULT 0.0");
            } catch (Exception ignored) {}
        }
    }

    public String getCurrentAcademicYear() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        if (month >= Calendar.SEPTEMBER) {
            return year + "-" + (year + 1);
        } else {
            return (year - 1) + "-" + year;
        }
    }

    public long addPaymentForMonth(int studentId, double amount, String date, String comments, String method, String targetMonth, String academicYear) {
        SQLiteDatabase db = this.getWritableDatabase();
        long id = -1;
        Cursor studentCursor = null;
        Cursor payCursor = null;

        try {
            // 1. Παίρνουμε τα δίδακτρα με ασφαλή διαχείριση του Cursor
            double studentFee = 0.0;
            studentCursor = db.rawQuery("SELECT monthly_fee FROM students WHERE id = ?", new String[]{String.valueOf(studentId)});
            if (studentCursor != null && studentCursor.moveToFirst()) {
                studentFee = studentCursor.getDouble(0);
            }
            if (studentCursor != null) {
                studentCursor.close();
            }

            // 2. Υπολογίζουμε πόσα έχουν δοθεί ήδη για αυτόν τον μήνα απευθείας με τον ίδιο db
            double alreadyPaid = 0.0;
            payCursor = db.rawQuery("SELECT SUM(amount) FROM payments WHERE student_id = ? AND target_month = ? AND academic_year = ?",
                    new String[]{String.valueOf(studentId), targetMonth, academicYear});
            if (payCursor != null && payCursor.moveToFirst()) {
                alreadyPaid = payCursor.getDouble(0);
            }
            if (payCursor != null) {
                payCursor.close();
            }

            double totalNow = alreadyPaid + amount;
            int isFinalSettlement = (studentFee > 0 && totalNow >= studentFee) ? 1 : 0;

            ContentValues values = new ContentValues();
            values.put(KEY_PAY_STUDENT_ID, studentId);
            values.put(KEY_PAY_AMOUNT, amount);
            values.put(KEY_PAY_DATE, (date != null && !date.isEmpty()) ? date : new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));
            values.put(KEY_PAY_COMMENTS, comments != null ? comments : "");
            values.put(KEY_PAY_METHOD, (method != null && !method.isEmpty()) ? method : "Μετρητά");
            values.put(KEY_PAY_TARGET_MONTH, targetMonth != null ? targetMonth : "");
            values.put(KEY_PAY_ACADEMIC_YEAR, academicYear != null ? academicYear : "");
            values.put("is_final_settlement", isFinalSettlement);

            id = db.insertOrThrow(TABLE_PAYMENTS, null, values);
        } catch (Exception e) {
            e.printStackTrace();
            lastError = e.getMessage();
        } finally {
            if (studentCursor != null) studentCursor.close();
            if (payCursor != null) payCursor.close();
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
        return id;
    }

    public double getAmountPaidForMonth(int studentId, String targetMonth, String academicYear) {
        SQLiteDatabase db = this.getReadableDatabase();
        double total = 0.0;
        Cursor cursor = null;

        try {
            String query = "SELECT SUM(" + KEY_PAY_AMOUNT + ") FROM " + TABLE_PAYMENTS +
                    " WHERE " + KEY_PAY_STUDENT_ID + " = ? AND " +
                    KEY_PAY_TARGET_MONTH + " = ? AND " +
                    KEY_PAY_ACADEMIC_YEAR + " = ?";

            cursor = db.rawQuery(query, new String[]{String.valueOf(studentId), targetMonth, academicYear});
            if (cursor != null && cursor.moveToFirst()) {
                total = cursor.getDouble(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return total;
    }

    public long addStudent(Student student) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_FIRST_NAME, student.getFirstName());
        values.put(KEY_LAST_NAME, student.getLastName());
        values.put(KEY_MONTHLY_FEE, student.getFee());
        values.put(KEY_GRADE, student.getGrade());
        values.put(KEY_MATERIAL_STATUS, student.getMaterialStatus());
        values.put(KEY_DIRECTION, student.getDirection());
        values.put(KEY_MATERIAL_COST, student.getMaterialCost()); // 🎯 Αποθήκευση κόστους
        values.put("is_deleted", 0);

        long id = db.insert(TABLE_STUDENTS, null, values);
        db.close();
        return id;
    }

    public void updateStudent(Student student) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_FIRST_NAME, student.getFirstName());
        values.put(KEY_LAST_NAME, student.getLastName());
        values.put(KEY_MONTHLY_FEE, student.getFee());
        values.put(KEY_GRADE, student.getGrade());
        values.put(KEY_MATERIAL_STATUS, student.getMaterialStatus());
        values.put(KEY_DIRECTION, student.getDirection());
        values.put(KEY_MATERIAL_COST, student.getMaterialCost()); // 🎯 Ενημέρωση κόστους

        db.update(TABLE_STUDENTS, values, KEY_ID + " = ?", new String[]{String.valueOf(student.getId())});
        db.close();
    }

    public void deleteStudent(int studentId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("is_deleted", 1);
        db.update(TABLE_STUDENTS, values, KEY_ID + " = ?", new String[]{String.valueOf(studentId)});
        db.close();
    }

    public ArrayList<Student> getStudentsByGrade(String grade) {
        ArrayList<Student> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            String query;
            String[] args;

            if (grade == null || grade.trim().isEmpty() || grade.equalsIgnoreCase("Όλοι")) {
                query = "SELECT * FROM " + TABLE_STUDENTS +
                        " WHERE (is_deleted = 0 OR is_deleted IS NULL)" +
                        " ORDER BY " + KEY_LAST_NAME + " ASC, " + KEY_FIRST_NAME + " ASC";
                args = null;
            } else {
                query = "SELECT * FROM " + TABLE_STUDENTS +
                        " WHERE TRIM(" + KEY_GRADE + ") LIKE TRIM(?) AND (is_deleted = 0 OR is_deleted IS NULL)" +
                        " ORDER BY " + KEY_LAST_NAME + " ASC, " + KEY_FIRST_NAME + " ASC";
                args = new String[]{grade.trim()};
            }

            cursor = db.rawQuery(query, args);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID));
                    String firstName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_FIRST_NAME));
                    String lastName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_LAST_NAME));
                    double fee = cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_MONTHLY_FEE));
                    String studentGrade = cursor.getString(cursor.getColumnIndexOrThrow(KEY_GRADE));

                    int matIndex = cursor.getColumnIndex(KEY_MATERIAL_STATUS);
                    String materialStatus = (matIndex != -1 && !cursor.isNull(matIndex)) ? cursor.getString(matIndex) : "";

                    int dirIndex = cursor.getColumnIndex(KEY_DIRECTION);
                    String direction = (dirIndex != -1 && !cursor.isNull(dirIndex)) ? cursor.getString(dirIndex) : "";

                    // 🎯 Διάβασμα κόστους υλικού
                    int costIndex = cursor.getColumnIndex(KEY_MATERIAL_COST);
                    double materialCost = (costIndex != -1 && !cursor.isNull(costIndex)) ? cursor.getDouble(costIndex) : 0.0;

                    // 🎯 Περνάμε το materialCost στον constructor
                    list.add(new Student(id, firstName, lastName, fee, studentGrade, materialStatus, direction, materialCost));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return list;
    }

    public ArrayList<Student> getStudentsByGradeAndDirection(String grade, String direction) {
        ArrayList<Student> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            String query;
            String[] args;
            String cleanDir = direction != null ? direction.replaceAll("[^\\p{L}\\p{Nd}]", "").trim() : "";

            if (cleanDir.isEmpty() || direction.equalsIgnoreCase("Όλες") || direction.equalsIgnoreCase("Όλοι")) {
                query = "SELECT * FROM " + TABLE_STUDENTS +
                        " WHERE " + KEY_GRADE + " = ? AND (is_deleted = 0 OR is_deleted IS NULL)" +
                        " ORDER BY " + KEY_LAST_NAME + " ASC, " + KEY_FIRST_NAME + " ASC";
                args = new String[]{grade};
            } else {
                query = "SELECT * FROM " + TABLE_STUDENTS +
                        " WHERE " + KEY_GRADE + " = ? AND " + KEY_DIRECTION + " LIKE ? AND (is_deleted = 0 OR is_deleted IS NULL)" +
                        " ORDER BY " + KEY_LAST_NAME + " ASC, " + KEY_FIRST_NAME + " ASC";
                args = new String[]{grade, "%" + cleanDir + "%"};
            }

            cursor = db.rawQuery(query, args);

            if (cursor != null && cursor.moveToFirst()) {
                do { // 🎯 Μόνο ΕΝΑ do { εδώ
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID));
                    String firstName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_FIRST_NAME));
                    String lastName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_LAST_NAME));
                    double fee = cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_MONTHLY_FEE));
                    String studentGrade = cursor.getString(cursor.getColumnIndexOrThrow(KEY_GRADE));

                    int matIndex = cursor.getColumnIndex(KEY_MATERIAL_STATUS);
                    String materialStatus = (matIndex != -1 && !cursor.isNull(matIndex)) ? cursor.getString(matIndex) : "";

                    int dirIndex = cursor.getColumnIndex(KEY_DIRECTION);
                    String studentDir = (dirIndex != -1 && !cursor.isNull(dirIndex)) ? cursor.getString(dirIndex) : "";

                    // 🎯 Διάβασμα κόστους υλικού
                    int costIndex = cursor.getColumnIndex(KEY_MATERIAL_COST);
                    double materialCost = (costIndex != -1 && !cursor.isNull(costIndex)) ? cursor.getDouble(costIndex) : 0.0;

                    // 🎯 Περνάμε το materialCost στον constructor
                    list.add(new Student(id, firstName, lastName, fee, studentGrade, materialStatus, studentDir, materialCost));
                } while (cursor.moveToNext()); // 🎯 Εδώ μπήκε η αγκύλη } που έλειπε
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return list;
    }

    public ArrayList<Payment> getPaymentsForStudent(int studentId) {
        ArrayList<Payment> paymentList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.query(
                    TABLE_PAYMENTS,
                    null,
                    KEY_PAY_STUDENT_ID + " = ?",
                    new String[]{String.valueOf(studentId)},
                    null,
                    null,
                    KEY_PAY_ID + " DESC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                int idIndex = cursor.getColumnIndex(KEY_PAY_ID);
                int amtIndex = cursor.getColumnIndex(KEY_PAY_AMOUNT);
                int dateIndex = cursor.getColumnIndex(KEY_PAY_DATE);
                int commIndex = cursor.getColumnIndex(KEY_PAY_COMMENTS);
                int methodIndex = cursor.getColumnIndex(KEY_PAY_METHOD);

                do {
                    int id = (idIndex != -1) ? cursor.getInt(idIndex) : 0;
                    double amount = (amtIndex != -1) ? cursor.getDouble(amtIndex) : 0.0;
                    String date = (dateIndex != -1 && !cursor.isNull(dateIndex)) ? cursor.getString(dateIndex) : "";
                    String comments = (commIndex != -1 && !cursor.isNull(commIndex)) ? cursor.getString(commIndex) : "";
                    String method = (methodIndex != -1 && !cursor.isNull(methodIndex)) ? cursor.getString(methodIndex) : "Μετρητά";

                    paymentList.add(new Payment(id, studentId, amount, date, comments, method));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            android.util.Log.e("DB_ERROR", "Error in getPaymentsForStudent: " + e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            // ΔΕΝ καλούμε db.close() εδώ
        }
        return paymentList;
    }

    public int getStudentsCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_STUDENTS + " WHERE is_deleted = 0 OR is_deleted IS NULL", null);
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        db.close();
        return count;
    }

    public double getExpectedMonthlyRevenue() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(" + KEY_MONTHLY_FEE + ") FROM " + TABLE_STUDENTS + " WHERE is_deleted = 0 OR is_deleted IS NULL", null);
        double total = 0.0;
        if (cursor != null && cursor.moveToFirst()) {
            total = cursor.getDouble(0);
            cursor.close();
        }
        db.close();
        return total;
    }

    public double getCollectedRevenueByMethod(String method, String monthYear) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT SUM(" + KEY_PAY_AMOUNT + ") FROM " + TABLE_PAYMENTS +
                " WHERE " + KEY_PAY_METHOD + " = ? AND " + KEY_PAY_DATE + " LIKE '%/' || ?";
        Cursor cursor = db.rawQuery(query, new String[]{method, monthYear});
        double total = 0.0;
        if (cursor != null && cursor.moveToFirst()) {
            total = cursor.getDouble(0);
            cursor.close();
        }
        db.close();
        return total;
    }

    public void promoteAllStudents() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            db.execSQL("DELETE FROM " + TABLE_STUDENTS + " WHERE " + KEY_GRADE + " = ?", new Object[]{"Γ' Λυκείου"});
            db.execSQL("UPDATE " + TABLE_STUDENTS + " SET " + KEY_GRADE + " = ? WHERE " + KEY_GRADE + " = ?", new Object[]{"Γ' Λυκείου", "Β' Λυκείου"});
            db.execSQL("UPDATE " + TABLE_STUDENTS + " SET " + KEY_GRADE + " = ? WHERE " + KEY_GRADE + " = ?", new Object[]{"Β' Λυκείου", "Α' Λυκείου"});
            db.execSQL("UPDATE " + TABLE_STUDENTS + " SET " + KEY_GRADE + " = ? WHERE " + KEY_GRADE + " = ?", new Object[]{"Α' Λυκείου", "Γ' Γυμνασίου"});
            db.execSQL("UPDATE " + TABLE_STUDENTS + " SET " + KEY_GRADE + " = ? WHERE " + KEY_GRADE + " = ?", new Object[]{"Γ' Γυμνασίου", "Β' Γυμνασίου"});
            db.execSQL("UPDATE " + TABLE_STUDENTS + " SET " + KEY_GRADE + " = ? WHERE " + KEY_GRADE + " = ?", new Object[]{"Β' Γυμνασίου", "Α' Γυμνασίου"});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public void clearAllPayments() {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.execSQL("DELETE FROM " + TABLE_PAYMENTS);
            db.execSQL("DELETE FROM sqlite_sequence WHERE name = ?", new String[]{TABLE_PAYMENTS});

            // Μηδενισμός του εποπτικού υλικού για όλους τους μαθητές
            db.execSQL("UPDATE students SET material_status = 'Εκκρεμεί'");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.close();
        }
    }

    public ArrayList<String> getMonthsWithPayments() {
        ArrayList<String> months = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String currentMonthYear = new SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(new Date());
        months.add(currentMonthYear);

        String query = "SELECT DISTINCT substr(" + KEY_PAY_DATE + ", 4) FROM " + TABLE_PAYMENTS +
                " ORDER BY " + KEY_PAY_ID + " DESC";

        Cursor cursor = db.rawQuery(query, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String monthYear = cursor.getString(0);
                if (monthYear != null && !monthYear.isEmpty() && !monthYear.equals(currentMonthYear)) {
                    months.add(monthYear);
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
        db.close();
        return months;
    }

    public boolean isMonthFullyPaid(int studentId, String targetMonth, String academicYear, double expectedFee) {
        SQLiteDatabase db = this.getReadableDatabase();
        boolean isSettled = false;
        Cursor cursor = null;

        try {
            String query = "SELECT COUNT(*) FROM " + TABLE_PAYMENTS +
                    " WHERE " + KEY_PAY_STUDENT_ID + " = ? AND " +
                    KEY_PAY_TARGET_MONTH + " = ? AND " +
                    KEY_PAY_ACADEMIC_YEAR + " = ? AND is_final_settlement = 1";

            cursor = db.rawQuery(query, new String[]{String.valueOf(studentId), targetMonth, academicYear});
            if (cursor != null && cursor.moveToFirst()) {
                if (cursor.getInt(0) > 0) {
                    isSettled = true;
                }
            }
        } catch (Exception e) {
            isSettled = false;
        } finally {
            if (cursor != null) cursor.close();
        }

        if (!isSettled) {
            double paid = getAmountPaidForMonth(studentId, targetMonth, academicYear);
            if (paid >= expectedFee && expectedFee > 0) {
                isSettled = true;
            }
        }
        db.close();
        return isSettled;
    }

    public ArrayList<Payment> getPaymentsByDate(String selectedDate) {
        ArrayList<Payment> paymentsList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Φέραμε και το p.id, p.student_id, p.payment_date για να χτίσουμε σωστά το Payment
        String query = "SELECT p." + KEY_PAY_ID + ", p.student_id, p.amount, p.payment_date, p.comments, p.payment_method, p.target_month, " +
                "s.last_name, s.first_name " +
                "FROM payments p " +
                "INNER JOIN students s ON p.student_id = s.id " +
                "WHERE p.payment_date = ?";

        Cursor cursor = db.rawQuery(query, new String[]{selectedDate});

        if (cursor != null && cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_PAY_ID));
                int studentId = cursor.getInt(cursor.getColumnIndexOrThrow("student_id"));
                double amount = cursor.getDouble(cursor.getColumnIndexOrThrow("amount"));
                String date = cursor.getString(cursor.getColumnIndexOrThrow("payment_date"));

                int commIdx = cursor.getColumnIndex("comments");
                String comments = (commIdx != -1 && !cursor.isNull(commIdx)) ? cursor.getString(commIdx) : "";

                int methIdx = cursor.getColumnIndex("payment_method");
                String method = (methIdx != -1 && !cursor.isNull(methIdx)) ? cursor.getString(methIdx) : "Μετρητά";

                // 1. Δημιουργούμε το καθαρό αντικείμενο
                Payment payment = new Payment(id, studentId, amount, date, comments, method);

                // 2. Του προσθέτουμε τις έξτρα πληροφορίες από το JOIN
                String lastName = cursor.getString(cursor.getColumnIndexOrThrow("last_name"));
                String firstName = cursor.getString(cursor.getColumnIndexOrThrow("first_name"));
                payment.setStudentName(lastName + " " + firstName);

                int monthIdx = cursor.getColumnIndex("target_month");
                String month = (monthIdx != -1 && !cursor.isNull(monthIdx)) ? cursor.getString(monthIdx) : "";
                payment.setTargetMonth(month);

                // 3. Το βάζουμε στη λίστα
                paymentsList.add(payment);
            } while (cursor.moveToNext());
            cursor.close();
        }
        db.close();
        return paymentsList;
    }

    public long addStudentNote(int studentId, String category, String noteText) {
        SQLiteDatabase db = this.getWritableDatabase();
        long id = -1;
        try {
            ContentValues values = new ContentValues();
            values.put(KEY_NOTE_STUDENT_ID, studentId);
            values.put(KEY_NOTE_DATE, new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date()));
            values.put(KEY_NOTE_CATEGORY, category);
            values.put(KEY_NOTE_TEXT, noteText);

            id = db.insert(TABLE_NOTES, null, values);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.close();
        }
        return id;
    }

    public ArrayList<StudentNote> getNotesForStudent(int studentId) {
        ArrayList<StudentNote> notesList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.query(TABLE_NOTES, null, KEY_NOTE_STUDENT_ID + " = ?",
                    new String[]{String.valueOf(studentId)}, null, null, KEY_NOTE_ID + " DESC");

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_NOTE_ID));
                    String date = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NOTE_DATE));
                    String category = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NOTE_CATEGORY));
                    String text = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NOTE_TEXT));

                    notesList.add(new StudentNote(id, studentId, date, category, text));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return notesList;
    }

    public void deleteStudentNote(int noteId) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.delete(TABLE_NOTES, KEY_NOTE_ID + " = ?", new String[]{String.valueOf(noteId)});
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.close();
        }
    }

    public void clearAllNotes() {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.execSQL("DELETE FROM student_notes");
            db.execSQL("DELETE FROM sqlite_sequence WHERE name = 'student_notes'");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.close();
        }
    }

    public ArrayList<Student> getUnpaidStudentsForMonth(String monthName, String academicYear) {
        ArrayList<Student> unpaidStudents = new ArrayList<>();
        if ("Αύγουστος".equalsIgnoreCase(monthName)) {
            return unpaidStudents;
        }

        android.content.SharedPreferences prefs = mContext.getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        ArrayList<Student> allStudents = getStudentsByGrade("Όλοι");

        for (Student student : allStudents) {
            if ("Ιούλιος".equalsIgnoreCase(monthName)) {
                String studentGrade = student.getGrade();
                boolean hasSummerClasses = prefs.getBoolean("summer_grade_" + studentGrade, "Β' Λυκείου".equals(studentGrade));
                if (!hasSummerClasses) {
                    continue;
                }
            }

            boolean isPaid = isMonthFullyPaid(student.getId(), monthName, academicYear, student.getFee());
            if (!isPaid) {
                unpaidStudents.add(student);
            }
        }
        return unpaidStudents;
    }

    public ArrayList<String[]> getAllPaymentsForExport() {
        ArrayList<String[]> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT p.*, s.last_name, s.first_name, s.grade " +
                "FROM payments p " +
                "INNER JOIN students s ON p.student_id = s.id " +
                "ORDER BY p.rowid DESC";

        Cursor cursor = db.rawQuery(query, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int dateIdx = cursor.getColumnIndex("payment_date");
                int lastIdx = cursor.getColumnIndex("last_name");
                int firstIdx = cursor.getColumnIndex("first_name");
                int gradeIdx = cursor.getColumnIndex("grade");
                int amountIdx = cursor.getColumnIndex("amount");
                int monthIdx = cursor.getColumnIndex("target_month");
                int methodIdx = cursor.getColumnIndex("payment_method");
                int commIdx = cursor.getColumnIndex("comments");

                String date = (dateIdx != -1 && !cursor.isNull(dateIdx)) ? cursor.getString(dateIdx) : "";
                String lastName = (lastIdx != -1 && !cursor.isNull(lastIdx)) ? cursor.getString(lastIdx) : "";
                String firstName = (firstIdx != -1 && !cursor.isNull(firstIdx)) ? cursor.getString(firstIdx) : "";
                String grade = (gradeIdx != -1 && !cursor.isNull(gradeIdx)) ? cursor.getString(gradeIdx) : "";
                String amount = (amountIdx != -1 && !cursor.isNull(amountIdx)) ? String.valueOf(cursor.getDouble(amountIdx)) : "0";
                String month = (monthIdx != -1 && !cursor.isNull(monthIdx)) ? cursor.getString(monthIdx) : "";
                String method = (methodIdx != -1 && !cursor.isNull(methodIdx)) ? cursor.getString(methodIdx) : "Μετρητά";
                String comments = (commIdx != -1 && !cursor.isNull(commIdx)) ? cursor.getString(commIdx) : "";

                list.add(new String[]{date, lastName, firstName, grade, amount, month, method, comments});
            } while (cursor.moveToNext());
            cursor.close();
        }
        db.close();
        return list;
    }

    /**
     * Επιστρέφει ΜΟΝΟ τους ΕΝΕΡΓΟΥΣ μαθητές για εξαγωγή σε Excel/CSV χωρίς σφάλματα στηλών.
     */
    public ArrayList<String[]> getAllStudentsForExport() {
        ArrayList<String[]> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM " + TABLE_STUDENTS +
                " WHERE (is_deleted = 0 OR is_deleted IS NULL)" +
                " ORDER BY " + KEY_LAST_NAME + " ASC, " + KEY_FIRST_NAME + " ASC";

        Cursor cursor = db.rawQuery(query, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int lastIdx = cursor.getColumnIndex(KEY_LAST_NAME);
                int firstIdx = cursor.getColumnIndex(KEY_FIRST_NAME);
                int gradeIdx = cursor.getColumnIndex(KEY_GRADE);
                int dirIdx = cursor.getColumnIndex(KEY_DIRECTION);
                int feeIdx = cursor.getColumnIndex(KEY_MONTHLY_FEE);
                int matIdx = cursor.getColumnIndex(KEY_MATERIAL_STATUS);

                String lastName = (lastIdx != -1 && !cursor.isNull(lastIdx)) ? cursor.getString(lastIdx) : "";
                String firstName = (firstIdx != -1 && !cursor.isNull(firstIdx)) ? cursor.getString(firstIdx) : "";
                String grade = (gradeIdx != -1 && !cursor.isNull(gradeIdx)) ? cursor.getString(gradeIdx) : "";
                String direction = (dirIdx != -1 && !cursor.isNull(dirIdx)) ? cursor.getString(dirIdx) : "";
                String fee = (feeIdx != -1 && !cursor.isNull(feeIdx)) ? String.valueOf(cursor.getDouble(feeIdx)) : "0";
                String material = (matIdx != -1 && !cursor.isNull(matIdx)) ? cursor.getString(matIdx) : "";

                list.add(new String[]{lastName, firstName, grade, direction, fee, material});
            } while (cursor.moveToNext());
            cursor.close();
        }
        db.close();
        return list;
    }
    /**
     * Υπολογίζει το συνολικό ποσό που έχει καταβάλει ένας μαθητής για έναν συγκεκριμένο μήνα.
     */
    public double getTotalPaidForMonth(int studentId, String monthName, String academicYear) {
        double totalPaid = 0.0;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            String query = "SELECT SUM(" + KEY_PAY_AMOUNT + ") FROM " + TABLE_PAYMENTS +
                    " WHERE " + KEY_PAY_STUDENT_ID + " = ? AND " +
                    KEY_PAY_TARGET_MONTH + " = ? AND " +
                    KEY_PAY_ACADEMIC_YEAR + " = ?";

            cursor = db.rawQuery(query, new String[]{String.valueOf(studentId), monthName, academicYear});

            if (cursor != null && cursor.moveToFirst()) {
                totalPaid = cursor.getDouble(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return totalPaid;
    }

    /**
     * Ενημερώνει μια υπάρχουσα πληρωμή αντί να δημιουργεί νέα.
     */
    public boolean updatePayment(int paymentId, double amount, String date, String comments, String method, int isFinalSettlement) {
        SQLiteDatabase db = this.getWritableDatabase();
        boolean success = false;

        try {
            ContentValues values = new ContentValues();
            values.put(KEY_PAY_AMOUNT, amount);
            values.put(KEY_PAY_DATE, (date != null && !date.isEmpty()) ? date : new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));
            values.put(KEY_PAY_COMMENTS, comments != null ? comments : "");
            values.put(KEY_PAY_METHOD, (method != null && !method.isEmpty()) ? method : "Μετρητά");
            values.put("is_final_settlement", isFinalSettlement);

            int rowsAffected = db.update(TABLE_PAYMENTS, values, KEY_PAY_ID + " = ?", new String[]{String.valueOf(paymentId)});
            success = (rowsAffected > 0);
        } catch (Exception e) {
            e.printStackTrace();
            lastError = e.getMessage();
        } finally {
            db.close();
        }
        return success;
    }
    public void deletePayment(int paymentId) {
        android.database.sqlite.SQLiteDatabase db = this.getWritableDatabase();
        int studentId = -1;
        String targetMonth = "";

        // 1. ΠΡΙΝ διαγράψουμε, διαβάζουμε τι αφορούσε αυτή η πληρωμή
        try {
            android.database.Cursor cursor = db.rawQuery("SELECT " + KEY_PAY_STUDENT_ID + ", " + KEY_PAY_TARGET_MONTH +
                    " FROM " + TABLE_PAYMENTS + " WHERE " + KEY_PAY_ID + " = ?", new String[]{String.valueOf(paymentId)});
            if (cursor != null && cursor.moveToFirst()) {
                studentId = cursor.getInt(0);
                targetMonth = cursor.getString(1);
            }
            if (cursor != null) cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 2. Τη διαγράφουμε οριστικά
        try {
            db.delete(TABLE_PAYMENTS, KEY_PAY_ID + " = ?", new String[]{String.valueOf(paymentId)});
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 3. Αν η πληρωμή ήταν για το Εποπτικό Υλικό, διορθώνουμε το Status του μαθητή!
        if (studentId != -1 && targetMonth != null && targetMonth.equals("Εποπτικό Υλικό")) {
            double remainingPaid = 0.0;
            double materialCost = 0.0;

            try {
                // Παίρνουμε το νέο άθροισμα (μετά τη διαγραφή)
                android.database.Cursor c1 = db.rawQuery("SELECT SUM(" + KEY_PAY_AMOUNT + ") FROM " + TABLE_PAYMENTS +
                                " WHERE " + KEY_PAY_STUDENT_ID + " = ? AND " + KEY_PAY_TARGET_MONTH + " = ?",
                        new String[]{String.valueOf(studentId), "Εποπτικό Υλικό"});
                if (c1 != null && c1.moveToFirst()) remainingPaid = c1.getDouble(0);
                if (c1 != null) c1.close();

                // Παίρνουμε το κόστος υλικού
                android.database.Cursor c2 = db.rawQuery("SELECT " + KEY_MATERIAL_COST + " FROM " + TABLE_STUDENTS +
                        " WHERE " + KEY_ID + " = ?", new String[]{String.valueOf(studentId)});
                if (c2 != null && c2.moveToFirst()) materialCost = c2.getDouble(0);
                if (c2 != null) c2.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Υπολογίζουμε το νέο status
            String newStatus = "";
            if (remainingPaid >= materialCost && materialCost > 0) {
                newStatus = "Πληρωμένο";
            } else if (remainingPaid > 0) {
                newStatus = "Μερική πληρωμή";
            } // Αν είναι 0, μένει κενό ("")

            // Ενημερώνουμε τον πίνακα Students
            android.content.ContentValues values = new android.content.ContentValues();
            values.put(KEY_MATERIAL_STATUS, newStatus);
            db.update(TABLE_STUDENTS, values, KEY_ID + " = ?", new String[]{String.valueOf(studentId)});
        }
        db.close();
    }
    public boolean isMaterialsPaid(int studentId, String academicYear) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        boolean paid = false;
        try {
            cursor = db.rawQuery(
                    "SELECT 1 FROM " + TABLE_PAYMENTS +
                            " WHERE " + KEY_PAY_STUDENT_ID + " = ? AND (" +
                            KEY_PAY_TARGET_MONTH + " = 'Εποπτικό Υλικό' OR " +
                            KEY_PAY_COMMENTS + " LIKE '%Εποπτικό Υλικό%') AND " +
                            KEY_PAY_ACADEMIC_YEAR + " = ? LIMIT 1",
                    new String[]{String.valueOf(studentId), academicYear}
            );
            paid = (cursor != null && cursor.moveToFirst());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
        return paid;
    }
    // Ενημέρωση του κόστους υλικού στον μαθητή
    public void updateStudentMaterialCost(int studentId, double cost) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_MATERIAL_COST, cost);
        db.update(TABLE_STUDENTS, values, KEY_ID + " = ?", new String[]{String.valueOf(studentId)});
        db.close();
    }

    // Υπολογισμός του συνόλου που έχει ήδη πληρώσει ο μαθητής για υλικό
    public double getTotalPaidForMaterials(int studentId) {
        SQLiteDatabase db = this.getReadableDatabase();
        double total = 0.0;
        Cursor cursor = db.rawQuery(
                "SELECT SUM(" + KEY_PAY_AMOUNT + ") FROM " + TABLE_PAYMENTS +
                        " WHERE " + KEY_PAY_STUDENT_ID + " = ? AND " + KEY_PAY_TARGET_MONTH + " = 'Εποπτικό Υλικό'",
                new String[]{String.valueOf(studentId)}
        );
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                total = cursor.getDouble(0);
            }
            cursor.close();
        }
        return total;
    }
    // Ενημέρωση της κατάστασης υλικού (Material Status) στον μαθητή
    public void updateStudentMaterialStatus(int studentId, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_MATERIAL_STATUS, status);
        db.update(TABLE_STUDENTS, values, KEY_ID + " = ?", new String[]{String.valueOf(studentId)});
        db.close();
    }
    public String getStudentNameById(int studentId) {
        String fullName = "Άγνωστος Μαθητής";
        android.database.sqlite.SQLiteDatabase db = this.getReadableDatabase();

        // Χρησιμοποιούμε τις δικές σου μεταβλητές για να μην χτυπήσει ΠΟΤΕ ξανά
        String query = "SELECT " + KEY_FIRST_NAME + ", " + KEY_LAST_NAME +
                " FROM " + TABLE_STUDENTS +
                " WHERE " + KEY_ID + " = ?";

        android.database.Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(studentId)});

        if (cursor.moveToFirst()) {
            String fName = cursor.getString(0);
            String lName = cursor.getString(1);

            if (fName == null) fName = "";
            if (lName == null) lName = "";

            // Τα ενώνουμε (πρώτα Επώνυμο, μετά Όνομα - άλλαξέ το αν θες το ανάποδο)
            fullName = (lName + " " + fName).trim();
        }
        cursor.close();
        return fullName;
    }
}
