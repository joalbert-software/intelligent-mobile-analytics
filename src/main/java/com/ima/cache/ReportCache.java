package com.ima.cache;

public interface ReportCache {
    String get(String key);

    void put(String key, String value, int ttlSeconds);
}
