package org.crazycake.shiro;

import redis.clients.jedis.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * redis cluster manager
 *
 * @create 2018-02-14 0:00
 **/

public class RedisClusterManager implements IRedisManager {


    private String host = "127.0.0.1";

    private static final int DEFAULT_EXPIRE = 3600;
    // expire time in seconds
    private int expire = DEFAULT_EXPIRE;

    // timeout for jedis try to connect to redis server, not expire time! In milliseconds
    private int timeout = Protocol.DEFAULT_TIMEOUT;

    // timeout for jedis try to read data from redis server
    private int soTimeout = Protocol.DEFAULT_TIMEOUT;

    // max attempts to connect to server
    private int maxAttempts = 3;

    private String password;

    private volatile JedisCluster jedisCluster = null;

    private void init() {
        synchronized (this) {
            if (jedisCluster == null) {
                jedisCluster = new JedisCluster(getHostAndPortSet(), timeout, soTimeout, maxAttempts, password, new JedisPoolConfig());
            }
        }
    }

    private Set<HostAndPort> getHostAndPortSet() {
        String[] hostAndPortArr = host.split(",");
        Set<HostAndPort> hostAndPorts = new HashSet<HostAndPort>();
        for (String hostAndPortStr : hostAndPortArr) {
            String[] hostAndPort = hostAndPortStr.split(":");
            hostAndPorts.add(new HostAndPort(hostAndPort[0], Integer.parseInt(hostAndPort[1])));
        }
        return hostAndPorts;
    }

    private void checkAndInit() {
        if (jedisCluster == null) {
            init();
        }
    }

    /**
     * get value from redis
     *
     * @param key
     * @return
     */
    public byte[] get(byte[] key) {
        checkAndInit();
        if (key == null) {
            return null;
        }
        return jedisCluster.get(key);
    }

    /**
     * set
     *
     * @param key
     * @param value
     * @return
     */
    public byte[] set(byte[] key, byte[] value) {
        checkAndInit();
        if (key == null) {
            return null;
        }
        jedisCluster.set(key, value);
        if (this.expire != 0) {
            jedisCluster.expire(key, this.expire);
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
        checkAndInit();
        if (key == null) {
            return null;
        }
        jedisCluster.set(key, value);
        if (this.expire != 0) {
            jedisCluster.expire(key, expire);
        }
        return value;
    }

    /**
     * del
     *
     * @param key
     */
    public void del(byte[] key) {
        checkAndInit();
        if (key == null) {
            return;
        }
        jedisCluster.del(key);
    }

    /**
     * cluster size
     */
    public Long dbSize() {
        checkAndInit();
        Long dbSize = 0L;
        Map<String, JedisPool> clusterNodes = jedisCluster.getClusterNodes();
        for(String k : clusterNodes.keySet()){
            JedisPool jp = clusterNodes.get(k);
            Jedis connection = jp.getResource();
            try {
                dbSize += connection.dbSize();
            } catch(Exception e){
                e.printStackTrace();
            } finally{
                connection.close();
            }
        }
        return dbSize;
    }

    /**
     * cluster keys
     * @param pattern
     * @return
     */
    public Set<byte[]> keys(byte[] pattern){
        checkAndInit();
        Set<byte[]> keys = new HashSet<byte[]>();
        Map<String, JedisPool> clusterNodes = jedisCluster.getClusterNodes();
        for(String k : clusterNodes.keySet()){
            JedisPool jp = clusterNodes.get(k);
            Jedis connection = jp.getResource();
            try {
                keys.addAll(connection.keys(pattern));
            } catch(Exception e){
                e.printStackTrace();
            } finally{
                connection.close();
            }
        }
        return keys;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getSoTimeout() {
        return soTimeout;
    }

    public void setSoTimeout(int soTimeout) {
        this.soTimeout = soTimeout;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

}
