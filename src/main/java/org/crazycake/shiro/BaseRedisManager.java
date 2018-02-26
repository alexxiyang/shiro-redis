package org.crazycake.shiro;

import redis.clients.jedis.Jedis;
import redis.clients.util.Pool;

import java.util.Set;

public abstract class BaseRedisManager implements IRedisManager {

    protected volatile Pool<Jedis> jedisPool = null;

    protected abstract void checkAndInit();

    // expire time in seconds
    protected static final int DEFAULT_EXPIRE = 3600;
    protected int expire = DEFAULT_EXPIRE;

    /**
     * get value from redis
     * @param key
     * @return
     */
    @Override
    public byte[] get(byte[] key){
        checkAndInit();
        if (key == null) {
            return null;
        }
        byte[] value = null;
        Jedis jedis = jedisPool.getResource();
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
    public byte[] set(byte[] key,byte[] value){
        checkAndInit();
        if (key == null) {
            return null;
        }
        Jedis jedis = jedisPool.getResource();
        try{
            jedis.set(key,value);
            if(this.getExpire() != 0){
                jedis.expire(key, this.getExpire());
            }
        }finally{
            jedis.close();
        }
        return value;
    }

    /**
     * set
     * @param key
     * @param value
     * @param expire
     * @return
     */
    @Override
    public byte[] set(byte[] key,byte[] value,int expire){
        checkAndInit();
        if (key == null) {
            return null;
        }
        Jedis jedis = jedisPool.getResource();
        try{
            jedis.set(key,value);
            if(expire != 0){
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
        checkAndInit();
        if (key == null) {
            return;
        }
        Jedis jedis = jedisPool.getResource();
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
        checkAndInit();
        Long dbSize = 0L;
        Jedis jedis = jedisPool.getResource();
        try{
            dbSize = jedis.dbSize();
        }finally{
            jedis.close();
        }
        return dbSize;
    }

    /**
     * keys
     * @param pattern
     * @return
     */
    @Override
    public Set<byte[]> keys(byte[] pattern){
        checkAndInit();
        Set<byte[]> keys = null;
        Jedis jedis = jedisPool.getResource();
        try{
            keys = jedis.keys(pattern);
        }finally{
            jedis.close();
        }
        return keys;
    }

    @Override
    public int getExpire() {
        return expire;
    }

    public void setExpire(int expire) {
        this.expire = expire;
    }
}
