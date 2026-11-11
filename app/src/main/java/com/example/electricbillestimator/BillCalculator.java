package com.example.electricbillestimator;

/**
 * Tariff maths. The two original methods are unchanged, so every bill is calculated exactly as before.
 * Blocks (RM per kWh): first 200 = 0.218, next 100 = 0.334, next 300 = 0.516, everything above 600 = 0.546.
 */
public class BillCalculator {

    /** Largest number of units accepted (stops typos like an extra digit). */
    public static final int MAX_UNITS = 1000000;

    /** How many kWh each block covers; the last block takes everything that is left. */
    public static final int[] BLOCK_SIZES = {200, 100, 300, Integer.MAX_VALUE};

    /** RM per kWh for each block. */
    public static final double[] BLOCK_RATES = {0.218, 0.334, 0.516, 0.546};

    public static double calculateTotalCharges(int units) {
        double total = 0.0;
        int remaining = units;

        if (remaining > 0) {
            int block = Math.min(remaining, 200);
            total += block * 0.218;
            remaining -= block;
        }
        if (remaining > 0) {
            int block = Math.min(remaining, 100);
            total += block * 0.334;
            remaining -= block;
        }
        if (remaining > 0) {
            int block = Math.min(remaining, 300);
            total += block * 0.516;
            remaining -= block;
        }
        if (remaining > 0) {
            total += remaining * 0.546;
        }

        return total;
    }

    public static double calculateFinalCost(double totalCharges, int rebatePercent) {
        return totalCharges - (totalCharges * rebatePercent / 100.0);
    }

    /** How many kWh fall into each of the four blocks, e.g. 250 -> {200, 50, 0, 0}. */
    public static int[] blockUnits(int units) {
        int[] result = new int[BLOCK_SIZES.length];
        int remaining = Math.max(units, 0);
        for (int i = 0; i < BLOCK_SIZES.length; i++) {
            int used = Math.min(remaining, BLOCK_SIZES[i]);
            result[i] = used;
            remaining -= used;
        }
        return result;
    }

    /** Cost in RM of each block (before rebate). */
    public static double[] blockCosts(int units) {
        int[] used = blockUnits(units);
        double[] costs = new double[used.length];
        for (int i = 0; i < used.length; i++) {
            costs[i] = used[i] * BLOCK_RATES[i];
        }
        return costs;
    }
}
