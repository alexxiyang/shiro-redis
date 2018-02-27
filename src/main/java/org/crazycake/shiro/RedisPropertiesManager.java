package org.crazycake.shiro;

import redis.clients.jedis.*;

/**
 * abstract redis manager
 *
 * @author chenxing
 * @create 2018-02-26 14:43
 **/

public abstract class RedisPropertiesManager implements IRedisManager{

    protected String ip = "127.0.0.1";

    protected String host = ip + ":" + Protocol.DEFAULT_PORT ;

    protected static final int DEFAULT_EXPIRE = 3600;

    // expire time in seconds
    protected int expire = DEFAULT_EXPIRE;

    // timeout for jedis try to connect to redis server, not expire time! In milliseconds
    protected int timeout = Protocol.DEFAULT_TIMEOUT;

    // timeout for jedis try to read data from redis server
    protected int soTimeout = Protocol.DEFAULT_TIMEOUT;

    // requirepass
    protected String password;

    // default select database
    protected int database = Protocol.DEFAULT_DATABASE;

    //scan numbers each time
    protected int count = 100;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public static int getDefaultExpire() {
        return DEFAULT_EXPIRE;
    }

    @Override
    public int getExpire() {
        return expire;
    }

    public void setExpire(int expire) {
        this.expire = expire;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getDatabase() {
        return database;
    }

    public void setDatabase(int database) {
        this.database = database;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getSoTimeout() {
        return soTimeout;
    }

    public void setSoTimeout(int soTimeout) {
        this.soTimeout = soTimeout;
    }

}
