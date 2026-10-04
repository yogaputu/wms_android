package com.budimas.wms;

/**
 * Pure quantity rules shared by the API picking screen.
 *
 * <p>The WMS API reports the total ready quantity across eligible racks and,
 * when available, the quantity in the rack it recommends next. A picker must
 * never submit more than the latter, even when the total is enough.</p>
 */
final class PickingAllocation {
    private PickingAllocation() {
    }

    static int remainingQuantity(int requiredQuantity, int pickedQuantity) {
        return Math.max(requiredQuantity - pickedQuantity, 0);
    }

    static boolean totalStockCoversRemaining(int remainingQuantity, int totalAvailableQuantity) {
        return remainingQuantity > 0
                && (totalAvailableQuantity < 0 || totalAvailableQuantity >= remainingQuantity);
    }

    static int suggestedRackScanQuantity(int remainingQuantity, int rackAvailableQuantity) {
        if (remainingQuantity <= 0) {
            return 0;
        }
        // A negative value means an older API did not return rack-level stock;
        // retain its full-line behaviour and let the server validate it.
        if (rackAvailableQuantity < 0) {
            return remainingQuantity;
        }
        return Math.min(remainingQuantity, rackAvailableQuantity);
    }
}
