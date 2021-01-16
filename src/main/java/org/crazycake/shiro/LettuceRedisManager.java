package org.crazycake.shiro;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.support.ConnectionPoolSupport;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.crazycake.shiro.common.WorkAloneLettuceRedisManager;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class LettuceRedisManager extends WorkAloneLettuceRedisManager implements IRedisManager {

    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 6379;
    private static final int DEFAULT_DATABASE = 0;
    private static final int DEFAULT_TIMEOUT = 10;

    private String host = DEFAULT_HOST;

    private int port = DEFAULT_PORT;

    private int timeout = DEFAULT_TIMEOUT;

    private String password;

    private int database = DEFAULT_DATABASE;

    private GenericObjectPool<StatefulRedisConnection<byte[], byte[]>> pool;

    private void initPool() {
        RedisURI redisUri = RedisURI.builder()
                .withHost(host)
                .withPort(port)
                .withDatabase(database)
                .withTimeout(Duration.of(timeout, ChronoUnit.SECONDS))
                .build();
        if (password != null) {
            redisUri.setPassword(password);
        }
        RedisClient redisClient = RedisClient.create(redisUri);
        synchronized (this) {
            if (pool == null) {
                pool = ConnectionPoolSupport.createGenericObjectPool(() -> redisClient.connect(ByteArrayCodec.INSTANCE), getPoolConfig());
            }
        }
    }

    @Override
    protected StatefulRedisConnection<byte[], byte[]> getConnection() {
        if (pool == null) {
            initPool();
        }
        try {
            return pool.borrowObject();
        } catch (Exception e) {
            throw new RedisConnectionException("Get StatefulRedisConnection failed");
        }
    }

    public LettuceRedisManager() {
    }

    public LettuceRedisManager(String host) {
        this.host = host;
    }

    public LettuceRedisManager(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public LettuceRedisManager(String host, int port, int database) {
        this.host = host;
        this.port = port;
        this.database = database;
    }

    public LettuceRedisManager(String host, int port, String password, int database) {
        this.host = host;
        this.port = port;
        this.password = password;
        this.database = database;
    }

    public LettuceRedisManager(String host, int port, String password, int database, int timeout) {
        this.host = host;
        this.port = port;
        this.timeout = timeout;
        this.password = password;
        this.database = database;
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

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public static LettuceRedisManagerBuilder builder() {
        return new LettuceRedisManagerBuilder();
    }

    public static final class LettuceRedisManagerBuilder {
        private String host = DEFAULT_HOST;
        private int port = DEFAULT_PORT;
        private int timeout = DEFAULT_TIMEOUT;
        private String password;
        private int database = DEFAULT_DATABASE;
        private GenericObjectPoolConfig<StatefulRedisConnection<byte[], byte[]>> poolConfig = new GenericObjectPoolConfig<>();

        private LettuceRedisManagerBuilder() {
        }

        public LettuceRedisManagerBuilder host(String host) {
            this.host = host;
            return this;
        }

        public LettuceRedisManagerBuilder port(int port) {
            this.port = port;
            return this;
        }

        public LettuceRedisManagerBuilder timeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

        public LettuceRedisManagerBuilder password(String password) {
            this.password = password;
            return this;
        }

        public LettuceRedisManagerBuilder database(int database) {
            this.database = database;
            return this;
        }

        public LettuceRedisManagerBuilder poolConfig(GenericObjectPoolConfig<StatefulRedisConnection<byte[], byte[]>> poolConfig) {
            this.poolConfig = poolConfig;
            return this;
        }

        public LettuceRedisManager build() {
            LettuceRedisManager lettuceRedisManager = new LettuceRedisManager();
            lettuceRedisManager.setHost(host);
            lettuceRedisManager.setPort(port);
            lettuceRedisManager.setTimeout(timeout);
            lettuceRedisManager.setPassword(password);
            lettuceRedisManager.setDatabase(database);
            lettuceRedisManager.setPoolConfig(poolConfig);
            return lettuceRedisManager;
        }
    }
}
