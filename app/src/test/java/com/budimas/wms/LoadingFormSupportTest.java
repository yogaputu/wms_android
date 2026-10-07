package com.budimas.wms;

import org.junit.Test;
import java.util.Locale;
import static org.junit.Assert.*;

public class LoadingFormSupportTest {
    @Test public void malformedRestoredDateCannotCrashTheForm() {
        for (String value : new String[]{null, "", "null", "2026", "2026-02-30", "2026-13-02", "2026-1-02"})
            assertEquals("", LoadingFormSupport.validIsoDate(value));
        assertEquals("2026-10-06", LoadingFormSupport.validIsoDate("2026-10-06"));
    }
    @Test public void calendarProducesIsoDateWithZeroBasedMonth() {
        assertEquals("2026-10-02", LoadingFormSupport.isoDate(2026, 9, 2));
        assertEquals("2028-02-29", LoadingFormSupport.isoDate(2028, 1, 29));
        assertEquals("2026-01-01", LoadingFormSupport.isoDate(2026, 0, 1));
        assertEquals("2026-12-31", LoadingFormSupport.isoDate(2026, 11, 31));
    }
    @Test public void invalidDatesCannotRollIntoAnotherMonth() {
        for (int[] value : new int[][]{{2026,1,29},{2026,1,30},{2026,12,1},{2026,0,0},{0,0,1}}) {
            try { LoadingFormSupport.isoDate(value[0],value[1],value[2]); fail("Invalid date accepted"); }
            catch (IllegalArgumentException expected) { }
        }
    }
    @Test public void dateDigitsDoNotDependOnDeviceLocale() {
        Locale previous = Locale.getDefault();
        try { Locale.setDefault(new Locale("ar")); assertEquals("2026-10-02", LoadingFormSupport.isoDate(2026,9,2)); }
        finally { Locale.setDefault(previous); }
    }
    @Test public void searchMatchesPlateNameCodeBranchAndMultipleWords() {
        assertTrue(LoadingFormSupport.matches("b123 solo", "B1234 AA - Truk", "Solo", "FLEET-01"));
        assertTrue(LoadingFormSupport.matches("fleet-01", "B1234 AA", "Solo", "FLEET-01"));
        assertTrue(LoadingFormSupport.matches("damar solo", "Damar", "Solo"));
        assertFalse(LoadingFormSupport.matches("damar jakarta", "Damar", "Solo"));
        assertTrue(LoadingFormSupport.matches("  ", "Damar", null));
        assertTrue(LoadingFormSupport.matches(null, "Damar"));
        assertFalse(LoadingFormSupport.matches("missing", "Damar"));
    }
    @Test public void searchCanReachEveryRowBeyondLegacyDropdownCaps() {
        int found = 0;
        for (int i=1; i<=500; i++) if (LoadingFormSupport.matches("500", "Armada " + i)) found++;
        assertEquals(1, found);
    }
}
