package com.budimas.wms;

import java.util.Locale;

/** Shipment-only QR contract shared by the scanner and manual input. */
final class ShipmentReference {
    private ShipmentReference() {}
    static String parse(String value) {
        String text = value == null ? "" : value.trim().toUpperCase(Locale.US);
        String prefix = "BUDIMAS-WMS|SHIPMENT|";
        if (text.startsWith(prefix)) text = text.substring(prefix.length());
        return text.matches("DRF-[1-9]\\d*-[1-9]\\d*") ? text : "";
    }
}
