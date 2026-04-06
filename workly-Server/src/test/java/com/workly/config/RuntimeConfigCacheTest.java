package com.workly.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RuntimeConfigCacheTest {

    private RuntimeConfigCache cache;

    @BeforeEach
    void setUp() {
        cache = new RuntimeConfigCache();
    }

    @Test
    void get_keyNotPresent_returnsDefault() {
        assertEquals("default", cache.get("missing", "default"));
    }

    @Test
    void put_thenGet_returnsValue() {
        cache.put("MAX_RADIUS", "50");
        assertEquals("50", cache.get("MAX_RADIUS", "100"));
    }

    @Test
    void getInt_validInt_returnsValue() {
        cache.put("RADIUS", "25");
        assertEquals(25, cache.getInt("RADIUS", 10));
    }

    @Test
    void getInt_keyNotPresent_returnsDefault() {
        assertEquals(10, cache.getInt("MISSING", 10));
    }

    @Test
    void getInt_invalidFormat_returnsDefault() {
        cache.put("INVALID", "not-a-number");
        assertEquals(10, cache.getInt("INVALID", 10));
    }

    @Test
    void put_overwritesExistingValue() {
        cache.put("KEY", "first");
        cache.put("KEY", "second");
        assertEquals("second", cache.get("KEY", "default"));
    }
}
