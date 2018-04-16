package org.crazycake.shiro;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

import java.util.HashSet;
import java.util.Set;

/**
 * Abstract class of RedisManager.
 */
public abstract class BaseRedisManager implements IRedisManager {

    /**
     * We are going to operate redis by acquiring Jedis object.
     * The subclass should realizes the way to get Jedis objects by implement the getJedis().
     * @return Jedis
     */
    protected abstract Jedis getJedis();

    /**
     * Expire time in seconds.
     */
    protected static final int DEFAULT_EXPIRE = -1;

    /**
     * Default value of count.
     */
    protected static final int DEFAULT_COUNT = 100;

    /**
     * The number of elements returned at every iteration.
     */
    private int count = DEFAULT_COUNT;

    /**
     * JedisPoolConfig used to initialize JedisPool.
     */
    private JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();

    /**
     * get value from redis
     * @param key
     * @return value
     */
    @Override
    public byte[] get(byte[] key) {
        if (key == null) {
            return null;
        }
        byte[] value = null;
        Jedis jedis = getJedis();
        try {
            value = jedis.get(key);
        } finally {
            jedis.close();
        }
        return value;
    }

    /**
     * set
     * @param key
     * @param value
     * @param exipreTime
     * @return value
     */
    @Override
    public byte[] set(byte[] key, byte[] value, int exipreTime) {
        if (key == null) {
            return null;
        }
        Jedis jedis = getJedis();
        try {
            jedis.set(key, value);
            if (exipreTime >= 0) {
                jedis.expire(key, exipreTime);
            }
         } finally {
            jedis.close();
        }
        return value;
    }

    /**
     * Delete a key-value pair.
     * @param key
     */
    @Override
    public void del(byte[] key) {
        if (key == null) {
            return;
        }
        Jedis jedis = getJedis();
        try {
            jedis.del(key);
        } finally {
            jedis.close();
        }
    }

    /**
     * Return the size of redis db.
     */
    @Override
    public Long dbSize() {
        Long dbSize = 0L;
        Jedis jedis = getJedis();
        try {
            dbSize = jedis.dbSize();
        } finally {
            jedis.close();
        }
        return dbSize;
    }

    /**
     * Return all the keys of Redis db. Filtered by pattern.
     *
     * @param pattern
     * @return
     */
    public Set<byte[]> keys(byte[] pattern) {
        Set<byte[]> keys = new HashSet<byte[]>();
        Jedis jedis = getJedis();

        try {
            ScanParams params = new ScanParams();
            params.count(count);
            params.match(pattern);
            byte[] cursor = ScanParams.SCAN_POINTER_START_BINARY;
            ScanResult<byte[]> scanResult;
            do {
                scanResult = jedis.scan(cursor, params);
                keys.addAll(scanResult.getResult());
                cursor = scanResult.getCursorAsBytes();
            } while (scanResult.getStringCursor().compareTo(ScanParams.SCAN_POINTER_START) > 0);
        } finally {
            jedis.close();
        }
        return keys;

    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public JedisPoolConfig getJedisPoolConfig() {
        return jedisPoolConfig;
    }

    public void setJedisPoolConfig(JedisPoolConfig jedisPoolConfig) {
        this.jedisPoolConfig = jedisPoolConfig;
    }
}
