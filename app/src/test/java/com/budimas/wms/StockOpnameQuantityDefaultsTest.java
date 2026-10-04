package com.budimas.wms;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

public class StockOpnameQuantityDefaultsTest {
    @Test
    public void splitsBalanceIntoCartonsAndRemainingPiecesWithoutDoubleCounting() {
        int[] quantities = StockOpnameQuantityDefaults.distributeGoodQty(125, new double[]{1, 24});

        assertArrayEquals(new int[]{5, 5}, quantities);
    }

    @Test
    public void usesLargestConfiguredUnitThenSmallerUnits() {
        int[] quantities = StockOpnameQuantityDefaults.distributeGoodQty(125, new double[]{1, 12, 144});

        assertArrayEquals(new int[]{5, 10, 0}, quantities);
    }

    @Test
    public void clampsNegativeBalancesAndInvalidFactorsToSafeDefaults() {
        assertArrayEquals(new int[]{0, 0},
                StockOpnameQuantityDefaults.distributeGoodQty(-3, new double[]{0, Double.NaN}));
    }
}