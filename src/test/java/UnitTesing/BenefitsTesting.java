/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package UnitTesing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import models.Benefits;

/**
 *
 * @author Alex Resurreccion
 */
public class BenefitsTesting {
    private Benefits benefits;

    @BeforeEach
    void setUp() {
        benefits = new Benefits(21000, 10500, 125, 1500, 1000, 800);
    }

    @Test
    void testGetAndSetBasicSalary() {
        assertEquals(21000, benefits.getBasicSalary());
        benefits.setBasicSalary(22000);
        assertEquals(22000, benefits.getBasicSalary());
    }

    @Test
    void testGetAndSetGrossSemiMonthlyRate() {
        assertEquals(10500, benefits.getGrossSemiMonthlyRate());
        benefits.setGrossSemiMonthlyRate(11000);
        assertEquals(11000, benefits.getGrossSemiMonthlyRate());
    }

    @Test
    void testGetAndSetHourlyRate() {
        assertEquals(125, benefits.getHourlyRate());
        benefits.setHourlyRate(130);
        assertEquals(130, benefits.getHourlyRate());
    }

    @Test
    void testGetAndSetRiceSubsidy() {
        assertEquals(1500, benefits.getRiceSubsidy());
        benefits.setRiceSubsidy(1600);
        assertEquals(1600, benefits.getRiceSubsidy());
    }

    @Test
    void testGetAndSetPhoneAllowance() {
        assertEquals(1000, benefits.getPhoneAllowance());
        benefits.setPhoneAllowance(1100);
        assertEquals(1100, benefits.getPhoneAllowance());
    }

    @Test
    void testGetAndSetClothingAllowance() {
        assertEquals(800, benefits.getClothingAllowance());
        benefits.setClothingAllowance(900);
        assertEquals(900, benefits.getClothingAllowance());
    }

    @Test
    void testCalculateHourlyRate() {
        assertEquals(125.0, benefits.calculateHourlyRate(), 0.01);
    }

    @Test
    void testCalculateTotalAllowance() {
        assertEquals(3300, benefits.calculateTotalAllowance(), 0.01);
    }

    @Test
    void testGetBenefitId() {
        assertEquals(0, benefits.getBenefitId());
    }

    @Test
    void testDefaultConstructor() {
        Benefits empty = new Benefits();
        assertEquals(0, empty.getBenefitId());
        assertEquals(0, empty.getBasicSalary(), 0.01);
        assertEquals(0, empty.getGrossSemiMonthlyRate(), 0.01);
        assertEquals(0, empty.getHourlyRate(), 0.01);
        assertEquals(0, empty.getRiceSubsidy(), 0.01);
        assertEquals(0, empty.getPhoneAllowance(), 0.01);
        assertEquals(0, empty.getClothingAllowance(), 0.01);
        assertEquals(0, empty.calculateHourlyRate(), 0.01);
        assertEquals(0, empty.calculateTotalAllowance(), 0.01);
    }
}
