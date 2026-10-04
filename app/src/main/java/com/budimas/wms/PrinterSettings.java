package com.budimas.wms;

import android.content.Context;
import android.content.SharedPreferences;

public class PrinterSettings {
    private static final String PREF_NAME = "budimas_wms_printer";
    private static final String KEY_MODE = "mode";
    private static final String KEY_NAME = "name";
    private static final String KEY_ADDRESS = "address";
    private static final String KEY_PAPER_WIDTH = "paper_width";
    private static final String KEY_AUTO_PRINT = "auto_print";

    private final SharedPreferences prefs;

    public PrinterSettings(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void save(String mode, String name, String address, int paperWidth, boolean autoPrint) {
        prefs.edit()
                .putString(KEY_MODE, mode)
                .putString(KEY_NAME, name)
                .putString(KEY_ADDRESS, address)
                .putInt(KEY_PAPER_WIDTH, paperWidth)
                .putBoolean(KEY_AUTO_PRINT, autoPrint)
                .apply();
    }

    public String getMode() {
        return prefs.getString(KEY_MODE, "Android Print");
    }

    public String getName() {
        return prefs.getString(KEY_NAME, "");
    }

    public String getAddress() {
        return prefs.getString(KEY_ADDRESS, "");
    }

    public int getPaperWidth() {
        return prefs.getInt(KEY_PAPER_WIDTH, 58);
    }

    public boolean isAutoPrint() {
        return prefs.getBoolean(KEY_AUTO_PRINT, false);
    }

    public String summary() {
        String name = getName();
        if (name.isEmpty()) {
            return "Belum dipilih";
        }
        String address = getAddress();
        return address.isEmpty() ? name : name + " (" + address + ")";
    }
}
