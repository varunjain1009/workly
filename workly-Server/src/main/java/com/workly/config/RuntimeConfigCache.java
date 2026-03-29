package com.workly.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class RuntimeConfigCache {
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public String get(String key, String defaultValue) {
        String value = cache.getOrDefault(key, defaultValue);
        log.debug("RuntimeConfigCache: get - key: {}, found: {}", key, cache.containsKey(key));
        return value;
    }

    public void put(String key, String value) {
        log.debug("RuntimeConfigCache: put - key: {}", key);
        cache.put(key, value);
    }

    public int getInt(String key, int defaultValue) {
        try {
            int result = Integer.parseInt(cache.getOrDefault(key, String.valueOf(defaultValue)));
            log.debug("RuntimeConfigCache: getInt - key: {}, value: {}", key, result);
            return result;
        } catch (NumberFormatException e) {
            log.debug("RuntimeConfigCache: getInt - key: {} has non-integer value, using default: {}", key, defaultValue);
            return defaultValue;
        }
    }
}
