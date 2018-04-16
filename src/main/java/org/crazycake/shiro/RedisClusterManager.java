package org.crazycake.shiro;

import redis.clients.jedis.*;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class RedisClusterManager implements IRedisManager {

    private static final int DEFAULT_COUNT = 100;
    private static final int DEFAULT_MAX_ATTEMPTS = 3;
    private static final String DEFAULT_HOST = "127.0.0.1:7000,127.0.0.1:7001,127.0.0.1:7002";
    private String host = DEFAULT_HOST;

    // timeout for jedis try to connect to redis server, not expire time! In milliseconds
    private int timeout = Protocol.DEFAULT_TIMEOUT;

    // timeout for jedis try to read data from redis server
    private int soTimeout = Protocol.DEFAULT_TIMEOUT;

    private String password;

    private int database = Protocol.DEFAULT_DATABASE;

    private int count = DEFAULT_COUNT;

    private int maxAttempts = DEFAULT_MAX_ATTEMPTS;

    /**
     * JedisPoolConfig used to initialize JedisPool.
     */
    private JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();

    private volatile JedisCluster jedisCluster = null;

    private void init() {
        synchronized (this) {
            if (jedisCluster == null) {
                jedisCluster = new JedisCluster(getHostAndPortSet(), timeout, soTimeout, maxAttempts, password, getJedisPoolConfig());
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

    public byte[] set(byte[] key, byte[] value, int expireTime) {
        if (key == null) {
            return null;
        }
        getJedisCluster().set(key, value);
        if (expireTime >= 0) {
            getJedisCluster().expire(key, expireTime);
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
        Map<String, JedisPool> clusterNodes = getJedisCluster().getClusterNodes();
        Iterator<Map.Entry<String, JedisPool>> nodeIt = clusterNodes.entrySet().iterator();
        while (nodeIt.hasNext()) {
            Map.Entry<String, JedisPool> node = nodeIt.next();
            Set<byte[]> nodeKeys = getKeysFromClusterNode(node.getValue(), pattern);
            if (nodeKeys == null || nodeKeys.size() == 0) {
                continue;
            }
            keys.addAll(nodeKeys);
        }

        return keys;
    }

    private Set<byte[]> getKeysFromClusterNode(JedisPool jedisPool, byte[] pattern) {
        Set<byte[]> keys = new HashSet<byte[]>();
        Jedis jedis = jedisPool.getResource();

        try {
            ScanParams params = new ScanParams();
            params.count(count);
            params.match(pattern);
            byte[] cursor = ScanParams.SCAN_POINTER_START_BINARY;
            ScanResult<byte[]> scanResult;
            do {
                scanResult = jedis.scan(cursor, params);
                keys.addAll(scanResult.getResult());
                cursor = scanResult.getCursorAsBytes();
            } while (scanResult.getStringCursor().compareTo(ScanParams.SCAN_POINTER_START) > 0);
        } finally {
            jedis.close();
        }
        return keys;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
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

    public JedisPoolConfig getJedisPoolConfig() {
        return jedisPoolConfig;
    }

    public void setJedisPoolConfig(JedisPoolConfig jedisPoolConfig) {
        this.jedisPoolConfig = jedisPoolConfig;
    }
}
