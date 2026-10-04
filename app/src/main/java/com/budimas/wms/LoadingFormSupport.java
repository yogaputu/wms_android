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
