package com.budimas.wms;

import java.util.Arrays;

final class StockOpnameQuantityDefaults {
    private StockOpnameQuantityDefaults() {
    }

    static int[] distributeGoodQty(int totalPcs, double[] uomFactors) {
        double[] factors = uomFactors == null ? new double[0] : uomFactors;
        int[] quantities = new int[factors.length];
        Integer[] order = new Integer[factors.length];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }
        Arrays.sort(order, (left, right) -> {
            int factorOrder = Double.compare(factor(factors[right]), factor(factors[left]));
            return factorOrder != 0 ? factorOrder : Integer.compare(left, right);
        });

        double remainingPcs = Math.max(totalPcs, 0);
        for (int index : order) {
            double factor = factor(factors[index]);
            int quantity = (int) Math.floor((remainingPcs + 0.0000001d) / factor);
            quantities[index] = quantity;
            remainingPcs = Math.max(remainingPcs - quantity * factor, 0d);
        }
        return quantities;
    }

    private static double factor(double value) {
        return Double.isNaN(value) || Double.isInfinite(value) || value < 1d ? 1d : value;
    }
}