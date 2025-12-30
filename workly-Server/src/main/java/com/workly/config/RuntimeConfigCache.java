package com.workly.config;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RuntimeConfigCache {
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public String get(String key, String defaultValue) {
        return cache.getOrDefault(key, defaultValue);
    }

    public void put(String key, String value) {
        cache.put(key, value);
    }

    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(cache.getOrDefault(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
