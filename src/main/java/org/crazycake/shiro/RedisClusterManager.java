package org.crazycake.shiro;

import redis.clients.jedis.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RedisClusterManager implements IRedisManager {

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


    // max attempts to connect to server
    private int maxAttempts = 3;

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


    protected JedisCluster getJedisCluster() {
        if (jedisCluster == null) {
            init();
        }
        return jedisCluster;
    }

    public byte[] get(byte[] key) {
        if (key == null) {
            return null;
        }
        return getJedisCluster().get(key);
    }

    public byte[] set(byte[] key, byte[] value) {
        if (key == null) {
            return null;
        }
        getJedisCluster().set(key, value);
        if (this.expire != 0) {
            getJedisCluster().expire(key, this.expire);
        }
        return value;
    }

    public byte[] set(byte[] key, byte[] value, int expire) {
        if (key == null) {
            return null;
        }
        getJedisCluster().set(key, value);
        if (this.expire != 0) {
            getJedisCluster().expire(key, expire);
        }
        return value;
    }

    public void del(byte[] key) {
        if (key == null) {
            return;
        }
        getJedisCluster().del(key);
    }

    public Long dbSize() {
        Long dbSize = 0L;
        Map<String, JedisPool> clusterNodes = getJedisCluster().getClusterNodes();
        for (String k : clusterNodes.keySet()) {
            JedisPool jp = clusterNodes.get(k);
            Jedis connection = jp.getResource();
            try {
                dbSize += connection.dbSize();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                connection.close();
            }
        }
        return dbSize;
    }

    public Set<byte[]> keys(byte[] pattern) {
        Set<byte[]> keys = new HashSet<byte[]>();
        ScanParams params = new ScanParams();
        params.count(count);
        params.match(pattern);
        byte[] cursor = ScanParams.SCAN_POINTER_START_BINARY;
        ScanResult<byte[]> scanResult;
        do {
            scanResult = getJedisCluster().scan(cursor, params);
            keys.addAll(scanResult.getResult());
            cursor = scanResult.getCursorAsBytes();
        } while (scanResult.getStringCursor().compareTo(ScanParams.SCAN_POINTER_START) > 0);

        return keys;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
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

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void setJedisCluster(JedisCluster jedisCluster) {
        this.jedisCluster = jedisCluster;
    }
}
