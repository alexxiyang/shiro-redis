package org.crazycake.shiro;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

import java.util.HashSet;
import java.util.Set;

/**
 * Whether JedisPool or JedisSentinelPool is used, we are going to operate redis by acquiring Jedis objects. The subclass
 * realizes the way to get Jedis objects by realizing the getJedis() method of JedisManager.
 */
public abstract class BaseRedisManager implements IRedisManager {

    protected abstract Jedis getJedis();

    // expire time in seconds
    protected static final int DEFAULT_EXPIRE = 3600;
    /**
     * Setting different expire times at RedisCacheManager or RedisSessionDAO instead of setting a global expire time for all redis cache.
     */
    @Deprecated
    protected int expire = DEFAULT_EXPIRE;

    // the number of elements returned at every iteration
    protected static final int DEFAULT_COUNT = 100;
    protected int count = DEFAULT_COUNT;

    protected JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();

    /**
     * get value from redis
     * @param key
     * @return
     */
    @Override
    public byte[] get(byte[] key){
        if (key == null) {
            return null;
        }
        byte[] value = null;
        Jedis jedis = getJedis();
        try{
            value = jedis.get(key);
        }finally{
            jedis.close();
        }
        return value;
    }

    /**
     * set
     * @param key
     * @param value
     * @return
     */
    @Override
    public byte[] set(byte[] key,byte[] value, int expire){
        if (key == null) {
            return null;
        }
        Jedis jedis = getJedis();
        try{
            jedis.set(key,value);
            if(expire >= 0){
                jedis.expire(key, expire);
            }
        }finally{
            jedis.close();
        }
        return value;
    }

    /**
     * del
     * @param key
     */
    @Override
    public void del(byte[] key){
        if (key == null) {
            return;
        }
        Jedis jedis = getJedis();
        try{
            jedis.del(key);
        }finally{
            jedis.close();
        }
    }

    /**
     * size
     */
    @Override
    public Long dbSize(){
        Long dbSize = 0L;
        Jedis jedis = getJedis();
        try{
            dbSize = jedis.dbSize();
        }finally{
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

    @Deprecated
    public int getExpire() {
        return expire;
    }

    public void setExpire(int expire) {
        this.expire = expire;
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
