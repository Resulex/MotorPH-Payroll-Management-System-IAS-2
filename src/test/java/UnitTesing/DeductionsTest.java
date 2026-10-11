package UnitTesing;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import models.Deductions;

/**
 * Unit tests for the Deductions class.
 */
class DeductionsTest {
    
    private Deductions deductions;

    @BeforeEach
    void setUp() {
        deductions = new Deductions();
    }
    
    @Test
    void testCalculatePhilhealthDeduction() {
        deductions.setPhilhealthDeduction(0);
        deductions = new Deductions(30000);
        assertEquals(450, deductions.getPhilhealthDeduction(), 0.01);
    }

    @Test
    void testCalculatePagibigDeduction() {
        deductions = new Deductions(1600);
        assertEquals(32, deductions.getPagIbigDeduction(), 0.01);
    }

    @Test
    void testCalculatePagibigDeduction_CappedAt100() {
        deductions = new Deductions(10000);
        assertEquals(100, deductions.getPagIbigDeduction(), 0.01);
    }
    
    @Test
    void testCalculateTaxDeduction_NoTax() {
        deductions.calculateTaxDeduction(20000);
        assertEquals(0, deductions.getTaxDeduction(), 0.01);
    }
    
    @Test
    void testCalculateTaxDeduction_AboveThreshold() {
        deductions.calculateTaxDeduction(50000);
        assertEquals(12500, deductions.getTaxDeduction(), 0.01);
    }
    
    @Test
    void testCalculateTotalDeductions() {
        deductions = new Deductions(50000);
        deductions.calculateTaxDeduction(50000);
        double expectedTotal = deductions.getSssDeduction() + deductions.getPhilhealthDeduction() + deductions.getPagIbigDeduction() + deductions.getTaxDeduction();
        assertEquals(expectedTotal, deductions.calculateTotalDeductions(), 0.01);
    }

    @Test
    void testFourArgConstructorAndSetters() {
        Deductions d = new Deductions(100, 200, 300, 400);
        assertEquals(100, d.getSssDeduction(), 0.01);
        assertEquals(200, d.getPhilhealthDeduction(), 0.01);
        assertEquals(300, d.getPagIbigDeduction(), 0.01);
        assertEquals(400, d.getTaxDeduction(), 0.01);

        d.setSssDeduction(110);
        d.setPhilhealthDeduction(210);
        d.setPagIbigDeduction(310);
        d.setTaxDeduction(410);
        assertEquals(110, d.getSssDeduction(), 0.01);
        assertEquals(210, d.getPhilhealthDeduction(), 0.01);
        assertEquals(310, d.getPagIbigDeduction(), 0.01);
        assertEquals(410, d.getTaxDeduction(), 0.01);
    }

    @Test
    void testDefaultConstructorDefaultsToZero() {
        Deductions d = new Deductions();
        assertEquals(0, d.getSssDeduction(), 0.01);
        assertEquals(0, d.getPhilhealthDeduction(), 0.01);
        assertEquals(0, d.getPagIbigDeduction(), 0.01);
        assertEquals(0, d.getTaxDeduction(), 0.01);
    }

    @Test
    void testPagibigNoDeductionBelow1000() {
        Deductions d = new Deductions(500);
        assertEquals(0, d.getPagIbigDeduction(), 0.01);
    }

    @Test
    void testPagibigOnePercentBand() {
        Deductions d = new Deductions(1200);
        assertEquals(12, d.getPagIbigDeduction(), 0.01);
    }

    @Test
    void testPhilhealthZeroForNonPositiveSalary() {
        Deductions d = new Deductions(0);
        assertEquals(0, d.getPhilhealthDeduction(), 0.01);
    }

    @Test
    void testTaxBrackets() {
        Deductions d = new Deductions();
        d.calculateTaxDeduction(700000);
        assertEquals(700000 * 0.35, d.getTaxDeduction(), 0.01);

        d.calculateTaxDeduction(200000);
        assertEquals(200000 * 0.32, d.getTaxDeduction(), 0.01);

        d.calculateTaxDeduction(100000);
        assertEquals(100000 * 0.30, d.getTaxDeduction(), 0.01);

        d.calculateTaxDeduction(40000);
        assertEquals(40000 * 0.25, d.getTaxDeduction(), 0.01);

        d.calculateTaxDeduction(25000);
        assertEquals(25000 * 0.20, d.getTaxDeduction(), 0.01);
    }
}

