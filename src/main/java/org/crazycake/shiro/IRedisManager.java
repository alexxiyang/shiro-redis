package org.crazycake.shiro;

import java.util.Set;

/**
 * redisManager interface
 *
 * @create 2018-02-13 23:58
 **/

public interface IRedisManager {

    /**
     * get value from redis
     * @param key
     * @return
     */
    public byte[] get(byte[] key);

    /**
     * set
     * @param key
     * @param value
     * @return
     */
    public byte[] set(byte[] key, byte[] value);

    /**
     * del
     * @param key
     */
    public void del(byte[] key);

    /**
     * size
     */
    public Long dbSize();

    /**
     * keys
     * @param pattern
     * @return
     */
    public Set<byte[]> keys(byte[] pattern);

    /**
     * expire time
     * @return
     */
    public int getExpire();

}
