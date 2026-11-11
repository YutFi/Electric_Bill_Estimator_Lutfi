package com.example.electricbillestimator;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BillCalculatorTest {

    private static final double EPS = 1e-9;

    @Test
    public void zeroUnitsCostNothing() {
        assertEquals(0.0, BillCalculator.calculateTotalCharges(0), EPS);
    }

    @Test
    public void firstBlockOnly() {
        assertEquals(43.6, BillCalculator.calculateTotalCharges(200), EPS);
    }

    @Test
    public void spansTwoBlocks() {
        // 200 * 0.218 + 50 * 0.334
        assertEquals(60.3, BillCalculator.calculateTotalCharges(250), EPS);
    }

    @Test
    public void spansAllFourBlocks() {
        // 43.6 + 33.4 + 154.8 + 150 * 0.546
        assertEquals(313.7, BillCalculator.calculateTotalCharges(750), EPS);
    }

    @Test
    public void rebateIsAppliedAsPercentage() {
        assertEquals(59.094, BillCalculator.calculateFinalCost(60.3, 2), EPS);
        assertEquals(298.015, BillCalculator.calculateFinalCost(313.7, 5), EPS);
    }

    @Test
    public void blockUnitsAddUpToInput() {
        assertArrayEquals(new int[]{200, 50, 0, 0}, BillCalculator.blockUnits(250));
        assertArrayEquals(new int[]{200, 100, 300, 150}, BillCalculator.blockUnits(750));
        int sum = 0;
        for (int u : BillCalculator.blockUnits(12345)) sum += u;
        assertEquals(12345, sum);
    }

    @Test
    public void blockCostsMatchTotal() {
        double sum = 0;
        for (double c : BillCalculator.blockCosts(750)) sum += c;
        assertEquals(BillCalculator.calculateTotalCharges(750), sum, 1e-9);
    }
}
