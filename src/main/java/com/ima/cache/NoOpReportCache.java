package com.ima.cache;

public final class NoOpReportCache implements ReportCache {
    @Override
    public String get(String key) {
        return null;
    }

    @Override
    public void put(String key, String value, int ttlSeconds) {
        // Demo mode deliberately avoids hidden cache state.
    }
}
