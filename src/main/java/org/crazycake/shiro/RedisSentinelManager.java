package org.crazycake.shiro;

import redis.clients.jedis.*;

import java.util.HashSet;
import java.util.Set;

/**
 * support jedis sentinel
 * @create 2018-02-26 11:16
 **/

public class RedisSentinelManager implements IRedisManager{

    private String host = "127.0.0.1:26379";
    private String masterName = "mymaster";

    private static final int DEFAULT_EXPIRE = 3600;
    // expire time in seconds
    private int expire = DEFAULT_EXPIRE;

    // timeout for jedis try to connect to redis server, not expire time! In milliseconds
    private int timeout = Protocol.DEFAULT_TIMEOUT;

    // timeout for jedis try to read data from redis server
    private int soTimeout = Protocol.DEFAULT_TIMEOUT;

    private String password;

    private int database = Protocol.DEFAULT_DATABASE;

    private volatile JedisSentinelPool jedisSentinelPool = null;

    private void init() {
        synchronized (this) {
            if (jedisSentinelPool == null) {
                jedisSentinelPool = new JedisSentinelPool(masterName,getJedisSentinelSet(),new JedisPoolConfig(),timeout,soTimeout,password,database);
            }
        }
    }

    private Set<String> getJedisSentinelSet(){
        String[] hostAndPortArr = host.split(",");
        Set<String> hostAndPorts = new HashSet<String>();
        for(String host : hostAndPortArr){
            hostAndPorts.add(host);
        }
        return hostAndPorts;
    }

    private void checkAndInit() {
        if (jedisSentinelPool == null) {
            init();
        }
    }

    /**
     * get value from redis
     * @param key
     * @return
     */
    public byte[] get(byte[] key){
        checkAndInit();
        if (key == null) {
            return null;
        }
        byte[] value = null;
        Jedis jedis = jedisSentinelPool.getResource();
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
    public byte[] set(byte[] key,byte[] value){
        checkAndInit();
        if (key == null) {
            return null;
        }
        Jedis jedis = jedisSentinelPool.getResource();
        try{
            jedis.set(key,value);
            if(this.expire != 0){
                jedis.expire(key, this.expire);
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
    public byte[] set(byte[] key,byte[] value,int expire){
        checkAndInit();
        if (key == null) {
            return null;
        }
        Jedis jedis = jedisSentinelPool.getResource();
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
    public void del(byte[] key){
        checkAndInit();
        if (key == null) {
            return;
        }
        Jedis jedis = jedisSentinelPool.getResource();
        try{
            jedis.del(key);
        }finally{
            jedis.close();
        }
    }

    /**
     * size
     */
    public Long dbSize(){
        checkAndInit();
        Long dbSize = 0L;
        Jedis jedis = jedisSentinelPool.getResource();
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
    public Set<byte[]> keys(byte[] pattern){
        checkAndInit();
        Set<byte[]> keys = null;
        Jedis jedis = jedisSentinelPool.getResource();
        try{
            keys = jedis.keys(pattern);
        }finally{
            jedis.close();
        }
        return keys;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getMasterName() {
        return masterName;
    }

    public void setMasterName(String masterName) {
        this.masterName = masterName;
    }

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

    public int getSoTimeout() {
        return soTimeout;
    }

    public void setSoTimeout(int soTimeout) {
        this.soTimeout = soTimeout;
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

}
