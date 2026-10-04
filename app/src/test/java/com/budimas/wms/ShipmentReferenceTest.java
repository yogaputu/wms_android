package com.budimas.wms;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class ShipmentReferenceTest {
    @Test public void acceptsPrintedShipmentQr() {
        assertEquals("DRF-2-100", ShipmentReference.parse("BUDIMAS-WMS|SHIPMENT|DRF-2-100"));
    }
    @Test public void acceptsManualShipmentReference() {
        assertEquals("DRF-2-100", ShipmentReference.parse(" drf-2-100 "));
    }
    @Test public void rejectsLegacyInvoiceAndProductQr() {
        for (String value : new String[]{"SO-12", "BUDIMAS-WMS|PICKING|1|R1|SO-12", "BUDIMAS-WMS|PALLET|PLT-123"}) {
            assertEquals("", ShipmentReference.parse(value));
        }
    }
    @Test public void rejectsMissingAndMalformedReferences() {
        for (String value : new String[]{null, "", "DRF-0-2", "DRF-2-0", "DRF-2-100|SO-12"}) {
            assertEquals("", ShipmentReference.parse(value));
        }
    }
}
