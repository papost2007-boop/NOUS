package com.nous.tutoringapp;

import android.content.Context;
import android.content.SharedPreferences;

public class PasswordManager {

    private static final String PREF_NAME = "AppSecurityPrefs";
    private static final String KEY_PASSWORD = "stats_password";

    // 🎯 Έλεγχος αν ο χρήστης έχει ήδη ορίσει κωδικό
    public static boolean isPasswordSet(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.contains(KEY_PASSWORD);
    }

    // 🎯 Αποθήκευση/Αλλαγή κωδικού
    public static void setPassword(Context context, String newPassword) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_PASSWORD, newPassword).apply();
    }

    // 🎯 Επαλήθευση αν ο κωδικός που πληκτρολόγησε είναι σωστός
    public static boolean checkPassword(Context context, String inputPassword) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String savedPassword = prefs.getString(KEY_PASSWORD, "");
        return savedPassword.equals(inputPassword);
    }
}