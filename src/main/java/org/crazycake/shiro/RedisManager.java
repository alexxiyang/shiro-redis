package org.crazycake.shiro;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisManager extends RedisSingletonManager {

    private volatile JedisPool jedisPool = null;
    private int port;

    private void init() {
        synchronized (this) {
            if (jedisPool == null) {
                if(port == 0){
                    // support host:port config style
                    String[] hostAndPort = host.split(":");
                    jedisPool = new JedisPool(new JedisPoolConfig(), hostAndPort[0], Integer.parseInt(hostAndPort[1]), timeout, password, database);
                }else{
                    jedisPool = new JedisPool(new JedisPoolConfig(), ip, port, timeout, password, database);
                }
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

    public JedisPool getJedisPool() {
        return jedisPool;
    }

    public void setJedisPool(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
