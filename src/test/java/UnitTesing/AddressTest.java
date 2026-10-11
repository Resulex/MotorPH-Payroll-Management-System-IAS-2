package UnitTesing;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import models.Address;

/**
 * Unit tests for the Address class.
 */
class AddressTest {

    @Test
    void testParameterizedConstructor() {
        Address address = new Address("123 Main St", "San Isidro", "Makati", "Metro Manila", "1234");
        assertEquals("123 Main St", address.getStreet());
        assertEquals("San Isidro", address.getBarangay());
        assertEquals("Makati", address.getCity());
        assertEquals("Metro Manila", address.getProvince());
        assertEquals("1234", address.getZipcode());
    }

    @Test
    void testDefaultConstructorAndSetters() {
        Address address = new Address();
        assertNull(address.getStreet());
        assertNull(address.getBarangay());
        assertNull(address.getCity());
        assertNull(address.getProvince());
        assertNull(address.getZipcode());

        address.setStreet("456 Oak Ave");
        address.setBarangay("Poblacion");
        address.setCity("Cebu City");
        address.setProvince("Cebu");
        address.setZipcode("6000");

        assertEquals("456 Oak Ave", address.getStreet());
        assertEquals("Poblacion", address.getBarangay());
        assertEquals("Cebu City", address.getCity());
        assertEquals("Cebu", address.getProvince());
        assertEquals("6000", address.getZipcode());
    }
}
