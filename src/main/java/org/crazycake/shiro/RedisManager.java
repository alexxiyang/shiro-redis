package org.crazycake.shiro;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

import java.util.Set;

public class RedisManager extends JedisManager {

    private volatile JedisPool jedisPool = null;

    private void init() {
        synchronized (this) {
            if (jedisPool == null) {
                String[] hostAndPort = host.split(":");
                jedisPool = new JedisPool(new JedisPoolConfig(), hostAndPort[0], Integer.parseInt(hostAndPort[1]), timeout, password, database);
            }
        }
    }

    @Override
    protected Jedis getJedis() {
        if (jedisPool == null) {
            init();
        }
        return jedisPool.getResource();
    }

}
