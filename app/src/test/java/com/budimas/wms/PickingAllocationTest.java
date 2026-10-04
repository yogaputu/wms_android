package com.budimas.wms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PickingAllocationTest {
    @Test
    public void scansOnlyTheSuggestedRackBalanceWhenStockIsSplitAcrossRacks() {
        int remaining = PickingAllocation.remainingQuantity(25, 0);

        assertTrue(PickingAllocation.totalStockCoversRemaining(remaining, 25));
        assertEquals(10, PickingAllocation.suggestedRackScanQuantity(remaining, 10));
    }

    @Test
    public void refusesToTreatAnInsufficientTotalAsReady() {
        int remaining = PickingAllocation.remainingQuantity(25, 5);

        assertFalse(PickingAllocation.totalStockCoversRemaining(remaining, 19));
    }

    @Test
    public void keepsFullLineBehaviourForLegacyResponsesWithoutRackQuantity() {
        assertEquals(12, PickingAllocation.suggestedRackScanQuantity(12, -1));
    }
}
