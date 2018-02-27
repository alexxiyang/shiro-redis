package org.crazycake.shiro;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

import java.util.HashSet;
import java.util.Set;

/**
 * Whether JedisPool or JedisSentinelPool is used, we are going to operate redis by acquiring Jedis objects. The subclass
 * realizes the way to get Jedis objects by realizing the getJedis() method of JedisManager.
 *
 * @create 2018-02-26 16:30
 **/

abstract class RedisSingletonManager extends RedisPropertiesManager {

    protected abstract Jedis getJedis();

    /**
     * get value from redis
     *
     * @param key
     * @return
     */
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
     *
     * @param key
     * @param value
     * @return
     */
    public byte[] set(byte[] key, byte[] value) {
        if (key == null) {
            return null;
        }
        Jedis jedis = getJedis();
        try {
            jedis.set(key, value);
            if (this.expire != 0) {
                jedis.expire(key, this.expire);
            }
        } finally {
            jedis.close();
        }
        return value;
    }

    /**
     * set
     *
     * @param key
     * @param value
     * @param expire
     * @return
     */
    public byte[] set(byte[] key, byte[] value, int expire) {
        if (key == null) {
            return null;
        }
        Jedis jedis = getJedis();
        try {
            jedis.set(key, value);
            if (expire != 0) {
                jedis.expire(key, expire);
            }
        } finally {
            jedis.close();
        }
        return value;
    }

    /**
     * del
     *
     * @param key
     */
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
     * size
     */
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
     * keys
     *
     * @param pattern
     * @return
     */
    public Set<byte[]> keys(byte[] pattern) {
        Set<byte[]> keys = null;
        Jedis jedis = getJedis();

        try{
            keys = new HashSet<byte[]>();
            ScanParams params = new ScanParams();
            params.count(count);
            params.match(pattern);
            byte[] cursor = ScanParams.SCAN_POINTER_START_BINARY;
            ScanResult<byte[]> scanResult;
            do{
                scanResult = jedis.scan(cursor,params);
                keys.addAll(scanResult.getResult());
                cursor = scanResult.getCursorAsBytes();
            }while(scanResult.getStringCursor().compareTo(ScanParams.SCAN_POINTER_START) > 0);

        }finally{
            jedis.close();
        }
        return keys;

    }
}
