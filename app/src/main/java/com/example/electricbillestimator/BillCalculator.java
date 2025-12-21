package com.example.electricbillestimator;

public class BillCalculator {

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
}