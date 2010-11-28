package com.ima.cache;

import com.ima.domain.Checks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;

/** Fail-open cache. Errors never turn accepted writes into failed writes. */
public final class RedisReportCache implements ReportCache {
    private static final Logger LOG = LoggerFactory.getLogger(RedisReportCache.class);
    private final JedisPool pool;

    public RedisReportCache(JedisPool pool) {
        this.pool = Checks.required(pool, "pool");
    }

    @Override
    public String get(String key) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.get(key);
        } catch (JedisException failure) {
            LOG.warn("report_cache_read_failed");
            return null;
        }
    }

    @Override
    public void put(String key, String value, int ttlSeconds) {
        Checks.range(ttlSeconds, "ttlSeconds", 1, 3600);
        Checks.required(value, "value");
        if (value.length() > 1000000) {
            return;
        }
        try (Jedis jedis = pool.getResource()) {
            jedis.setex(key, ttlSeconds, value);
        } catch (JedisException failure) {
            LOG.warn("report_cache_write_failed");
        }
    }
}
