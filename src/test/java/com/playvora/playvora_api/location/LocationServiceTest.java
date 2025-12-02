package com.playvora.playvora_api.location;

import com.playvora.playvora_api.common.exception.BadRequestException;
import com.playvora.playvora_api.location.dtos.LocationResponse;
import com.playvora.playvora_api.location.services.impl.LocationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LocationServiceTest {

    @Autowired
    private LocationService locationService;

    @Test
    void testGetLocationByValidPostcode() {
        // Test with Buckingham Palace postcode
        String postcode = "SW1A2AA";
        
        LocationResponse response = locationService.getLocationByPostcode(postcode);
        
        assertNotNull(response);
        assertEquals("SW1A 2AA", response.getPostcode());
        assertNotNull(response.getLongitude());
        assertNotNull(response.getLatitude());
        assertEquals("England", response.getCountry());
        assertEquals("London", response.getRegion());
        
        // Verify coordinates are roughly correct for Buckingham Palace
        assertEquals(-0.12767, response.getLongitude(), 0.001);
        assertEquals(51.503541, response.getLatitude(), 0.001);
    }

    @Test
    void testGetLocationByValidPostcodeWithSpaces() {
        // Test with spaces in postcode
        String postcode = "M1 1AE";
        
        LocationResponse response = locationService.getLocationByPostcode(postcode);
        
        assertNotNull(response);
        assertEquals("M1 1AE", response.getPostcode());
        assertNotNull(response.getLongitude());
        assertNotNull(response.getLatitude());
    }

    @Test
    void testGetLocationByInvalidPostcode() {
        // Test with invalid postcode
        String postcode = "INVALID";
        
        assertThrows(BadRequestException.class, () -> {
            locationService.getLocationByPostcode(postcode);
        });
    }

    @Test
    void testGetLocationByEmptyPostcode() {
        // Test with empty postcode
        String postcode = "";
        
        assertThrows(BadRequestException.class, () -> {
            locationService.getLocationByPostcode(postcode);
        });
    }
}

