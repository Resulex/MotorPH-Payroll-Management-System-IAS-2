/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package UnitTesing;
import models.Address;
import models.Benefits;
import models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;

class UserTest {
    private TestUser user; // Concrete subclass for testing

    // Concrete subclass since User is abstract
    static class TestUser extends User {
        public TestUser(int employeeID) {
            super(employeeID);
        }
    }

    @BeforeEach
    void setUp() {
        user = new TestUser(101);
        user.setFirstName("Juan");
        user.setLastName("Dela Cruz");
        user.setBirthday(new Date());
        user.setPosition("Software Developer");
//        user.setAddress("Manila, Philippines");
        user.setPhoneNumber("09123456789");
        user.setSssNumber("12-3456789-0");
        user.setPhilhealthNumber("1234-5678-9012");
        user.setTinNumber("123-456-789");
        user.setPagibigNumber("1234-5678-9012");
    }

    @Test
    void testSetAndGetEmployeeID() {
        assertEquals(101, user.getEmployeeID());
    }

    @Test
    void testSetAndGetFirstName() {
        assertEquals("Juan", user.getFirstName());
    }

    @Test
    void testSetAndGetLastName() {
        assertEquals("Dela Cruz", user.getLastName());
    }

    @Test
    void testSetAndGetBirthday() {
        Date newDate = new Date();
        user.setBirthday(newDate);
        assertEquals(newDate, user.getBirthday());
    }

    @Test
    void testSetAndGetPosition() {
        assertEquals("Software Developer", user.getPosition());
    }

    @Test
    void testSetAndGetPhoneNumber() {
        assertEquals("09123456789", user.getPhoneNumber());
    }

    @Test
    void testSetAndGetSssNumber() {
        assertEquals("12-3456789-0", user.getSssNumber());
    }

    @Test
    void testSetAndGetPhilhealthNumber() {
        assertEquals("1234-5678-9012", user.getPhilhealthNumber());
    }

    @Test
    void testSetAndGetTinNumber() {
        assertEquals("123-456-789", user.getTinNumber());
    }

    @Test
    void testSetAndGetPagibigNumber() {
        assertEquals("1234-5678-9012", user.getPagibigNumber());
    }

    @Test
    void testSetAndGetStatus() {
        user.setStatus("Active");
        assertEquals("Active", user.getStatus());
    }

    @Test
    void testSetAndGetImmediateSupervisor() {
        user.setImmediateSupervisor(10005);
        assertEquals(10005, user.getImmediateSupervisor());
    }

    @Test
    void testSetAndGetDateHired() {
        Date hired = new Date();
        user.setDateHired(hired);
        assertEquals(hired, user.getDateHired());
    }

    @Test
    void testSetAndGetDepartment() {
        user.setDepartment("Engineering");
        assertEquals("Engineering", user.getDepartment());
    }

    @Test
    void testSetAndGetAddress() {
        Address address = new Address("1 Rizal St", "Bgy 1", "Manila", "NCR", "1000");
        user.setAddress(address);
        assertEquals(address, user.getAddress());
    }

    @Test
    void testBenefitsAccessors() {
        user.setBenefits(new Benefits(30000, 15000, 200, 2000, 1000, 500));
        assertEquals(30000, user.getBenefitsBasicSalary(), 0.01);
        assertEquals(15000, user.getBenefitsGrossSemiMonthlyRate(), 0.01);
        assertEquals(200, user.getBenefitsHourlyRate(), 0.01);
        assertEquals(2000, user.getBenefitsRiceSubsidy(), 0.01);
        assertEquals(1000, user.getBenefitsPhoneAllowance(), 0.01);
        assertEquals(500, user.getBenefitsClothingAllowance(), 0.01);

        user.setBenefitsBasicSalary(31000);
        user.setBenefitsGrossSemiMonthlyRate(15500);
        user.setBenefitsHourlyRate(210);
        user.setBenefitsRiceSubsidy(2100);
        user.setBenefitsPhoneAllowance(1100);
        user.setBenefitsClothingAllowance(600);

        assertEquals(31000, user.getBenefitsBasicSalary(), 0.01);
        assertEquals(15500, user.getBenefitsGrossSemiMonthlyRate(), 0.01);
        assertEquals(210, user.getBenefitsHourlyRate(), 0.01);
        assertEquals(2100, user.getBenefitsRiceSubsidy(), 0.01);
        assertEquals(1100, user.getBenefitsPhoneAllowance(), 0.01);
        assertEquals(600, user.getBenefitsClothingAllowance(), 0.01);
    }

    @Test
    void testCalculateTotalBenefits() {
        user.setBenefits(new Benefits(30000, 15000, 200, 2000, 1000, 500));
        assertEquals(3500, user.calculateTotalBenefits(), 0.01);
    }
}
