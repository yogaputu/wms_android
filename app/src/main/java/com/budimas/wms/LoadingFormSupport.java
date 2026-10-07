package com.budimas.wms;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

/** Locale-independent request dates and shared picker search rules. */
final class LoadingFormSupport {
    private LoadingFormSupport() { }

    static String isoDate(int year, int zeroBasedMonth, int day) {
        if (year < 1 || year > 9999) throw new IllegalArgumentException("Invalid year");
        Calendar date = new GregorianCalendar();
        date.clear();
        date.setLenient(false);
        date.set(year, zeroBasedMonth, day);
        date.getTime(); // Reject rolled-over dates (e.g. February 30).
        return String.format(Locale.ROOT, "%04d-%02d-%02d", year, zeroBasedMonth + 1, day);
    }

    static String validIsoDate(String value) {
        if (value == null || !value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) return "";
        try {
            return isoDate(Integer.parseInt(value.substring(0, 4)),
                    Integer.parseInt(value.substring(5, 7)) - 1,
                    Integer.parseInt(value.substring(8, 10)));
        } catch (IllegalArgumentException invalid) { return ""; }
    }

    static boolean matches(String query, String... values) {
        StringBuilder text = new StringBuilder();
        for (String value : values) if (value != null) text.append(' ').append(value);
        String haystack = text.toString().toLowerCase(Locale.ROOT);
        for (String token : (query == null ? "" : query).trim().toLowerCase(Locale.ROOT).split("\\s+")) {
            if (!haystack.contains(token)) return false;
        }
        return true;
    }
}
