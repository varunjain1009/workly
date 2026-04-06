package com.workly.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegionHelperTest {

    @Test
    void fromCoordinates_returnsExpectedKey() {
        assertEquals("19_72", RegionHelper.fromCoordinates(19.07, 72.87));
    }

    @Test
    void fromLocation_geoJsonOrder_returnsRegion() {
        // GeoJSON: [longitude, latitude]
        assertEquals("12_77", RegionHelper.fromLocation(new double[]{77.6, 12.9}));
    }

    @Test
    void fromLocation_null_returnsNull() {
        assertNull(RegionHelper.fromLocation(null));
    }

    @Test
    void fromLocation_tooShort_returnsNull() {
        assertNull(RegionHelper.fromLocation(new double[]{77.6}));
    }

    @Test
    void fromCoordinates_negativeCoords_returnsNegativeKey() {
        String result = RegionHelper.fromCoordinates(-33.9, -70.6);
        assertEquals("-34_-71", result);
    }
}
