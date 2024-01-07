package org.crazycake.shiro;

import io.lettuce.core.*;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import io.lettuce.core.cluster.models.partitions.ClusterPartitionParser;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.support.ConnectionPoolSupport;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.crazycake.shiro.exception.PoolException;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * @author Teamo
 * @since 2022/05/19
 */
public class LettuceRedisClusterManager implements IRedisManager {

    /**
     * Comma-separated list of "host:port" pairs to bootstrap from. This represents an
     * "initial" list of cluster nodes and is required to have at least one entry.
     */
    private List<String> nodes;

    /**
     * Default value of count.
     */
    private static final int DEFAULT_COUNT = 100;

    /**
     * timeout for RedisClient try to connect to redis server, not expire time! unit seconds.
     */
    private Duration timeout = RedisURI.DEFAULT_TIMEOUT_DURATION;

    /**
     * Redis database.
     */
    private int database = 0;

    /**
     * Redis password.
     */
    private String password;

    /**
     * Whether to enable async.
     */
    private boolean isAsync = true;

    /**
     * The number of elements returned at every iteration.
     */
    private int count = DEFAULT_COUNT;

    /**
     * genericObjectPoolConfig used to initialize GenericObjectPoolConfig object.
     */
    private GenericObjectPoolConfig<StatefulRedisClusterConnection<byte[], byte[]>> genericObjectPoolConfig = new GenericObjectPoolConfig<>();

    /**
     * GenericObjectPool.
     */
    private volatile GenericObjectPool<StatefulRedisClusterConnection<byte[], byte[]>> genericObjectPool;

    /**
     * ClusterClientOptions used to initialize RedisClient.
     */
    private ClusterClientOptions clusterClientOptions = ClusterClientOptions.create();

    private void initialize() {
        if (genericObjectPool == null) {
            synchronized (LettuceRedisClusterManager.class) {
                if (genericObjectPool == null) {
                    RedisClusterClient redisClusterClient = RedisClusterClient.create(getClusterRedisURI());
                    redisClusterClient.setOptions(clusterClientOptions);
                    StatefulRedisClusterConnection<byte[], byte[]> connect = redisClusterClient.connect(new ByteArrayCodec());
                    genericObjectPool = ConnectionPoolSupport.createGenericObjectPool(() -> connect, genericObjectPoolConfig);
                }
            }
        }
    }

    private StatefulRedisClusterConnection<byte[], byte[]> getStatefulConnection() {
        if (genericObjectPool == null) {
            initialize();
        }
        try {
            return genericObjectPool.borrowObject();
        } catch (Exception e) {
            throw new PoolException("Could not get a resource from the pool", e);
        }
    }

    private List<RedisURI> getClusterRedisURI() {
        Objects.requireNonNull(nodes, "nodes must not be null!");
        return nodes.stream().map(node -> {
            String[] hostAndPort = node.split(":");
            RedisURI.Builder builder = RedisURI.builder()
                    .withHost(hostAndPort[0])
                    .withPort(Integer.parseInt(hostAndPort[1]))
                    .withDatabase(database)
                    .withTimeout(timeout);
            if (password != null) {
                builder.withPassword(password.toCharArray());
            }
            return builder.build();
        }).collect(Collectors.toList());
    }

    @Override
    public byte[] get(byte[] key) {
        if (key == null) {
            return null;
        }
        byte[] value = null;
        try (StatefulRedisClusterConnection<byte[], byte[]> connection = getStatefulConnection()) {
            if (isAsync) {
                RedisAdvancedClusterAsyncCommands<byte[], byte[]> async = connection.async();
                value = LettuceFutures.awaitOrCancel(async.get(key), timeout.getSeconds(), TimeUnit.SECONDS);
            } else {
                RedisAdvancedClusterCommands<byte[], byte[]> sync = connection.sync();
                value = sync.get(key);
            }
        }
        return value;
    }

    @Override
    public byte[] set(byte[] key, byte[] value, int expire) {
        if (key == null) {
            return null;
        }
        try (StatefulRedisClusterConnection<byte[], byte[]> connection = getStatefulConnection()) {
            if (isAsync) {
                RedisAdvancedClusterAsyncCommands<byte[], byte[]> async = connection.async();
                if (expire > 0) {
                    async.set(key, value, SetArgs.Builder.ex(expire));
                } else {
                    async.set(key, value);
                }
            } else {
                RedisAdvancedClusterCommands<byte[], byte[]> sync = connection.sync();
                if (expire > 0) {
                    sync.set(key, value, SetArgs.Builder.ex(expire));
                } else {
                    sync.set(key, value);
                }
            }
        }
        return value;
    }

    @Override
    public void del(byte[] key) {
        try (StatefulRedisClusterConnection<byte[], byte[]> connection = getStatefulConnection()) {
            if (isAsync) {
                RedisAdvancedClusterAsyncCommands<byte[], byte[]> async = connection.async();
                async.del(key);
            } else {
                RedisAdvancedClusterCommands<byte[], byte[]> sync = connection.sync();
                sync.del(key);
            }
        }
    }

    @Override
    public Long dbSize(byte[] pattern) {
        AtomicLong dbSize = new AtomicLong(0L);

        try (StatefulRedisClusterConnection<byte[], byte[]> connection = getStatefulConnection()) {
            if (isAsync) {
                RedisAdvancedClusterAsyncCommands<byte[], byte[]> async = connection.async();
                Partitions parse = ClusterPartitionParser.parse(LettuceFutures.awaitOrCancel(async.clusterNodes(), timeout.getSeconds(), TimeUnit.SECONDS));

                parse.forEach(redisClusterNode -> {
                    RedisClusterAsyncCommands<byte[], byte[]> clusterAsyncCommands = async.getConnection(redisClusterNode.getNodeId());

                    KeyScanCursor<byte[]> scanCursor = new KeyScanCursor<>();
                    scanCursor.setCursor(ScanCursor.INITIAL.getCursor());
                    ScanArgs scanArgs = ScanArgs.Builder.matches(pattern).limit(count);
                    while (!scanCursor.isFinished()) {
                        scanCursor = LettuceFutures.awaitOrCancel(clusterAsyncCommands.scan(scanCursor, scanArgs), timeout.getSeconds(), TimeUnit.SECONDS);
                        dbSize.addAndGet(scanCursor.getKeys().size());
                    }
                });
            } else {
                RedisAdvancedClusterCommands<byte[], byte[]> sync = connection.sync();
                Partitions parse = ClusterPartitionParser.parse(sync.clusterNodes());

                parse.forEach(redisClusterNode -> {
                    RedisClusterCommands<byte[], byte[]> clusterCommands = sync.getConnection(redisClusterNode.getNodeId());

                    KeyScanCursor<byte[]> scanCursor = new KeyScanCursor<>();
                    scanCursor.setCursor(ScanCursor.INITIAL.getCursor());
                    ScanArgs scanArgs = ScanArgs.Builder.matches(pattern).limit(count);
                    while (!scanCursor.isFinished()) {
                        scanCursor = clusterCommands.scan(scanCursor, scanArgs);
                        dbSize.addAndGet(scanCursor.getKeys().size());
                    }
                });
            }
        }
        return dbSize.get();
    }

    @Override
    public Set<byte[]> keys(byte[] pattern) {
        Set<byte[]> keys = new HashSet<>();

        try (StatefulRedisClusterConnection<byte[], byte[]> connection = getStatefulConnection()) {
            if (isAsync) {
                RedisAdvancedClusterAsyncCommands<byte[], byte[]> async = connection.async();
                Partitions parse = ClusterPartitionParser.parse(LettuceFutures.awaitOrCancel(async.clusterNodes(), timeout.getSeconds(), TimeUnit.SECONDS));

                parse.forEach(redisClusterNode -> {
                    RedisClusterAsyncCommands<byte[], byte[]> clusterAsyncCommands = async.getConnection(redisClusterNode.getNodeId());

                    KeyScanCursor<byte[]> scanCursor = new KeyScanCursor<>();
                    scanCursor.setCursor(ScanCursor.INITIAL.getCursor());
                    ScanArgs scanArgs = ScanArgs.Builder.matches(pattern).limit(count);
                    while (!scanCursor.isFinished()) {
                        scanCursor = LettuceFutures.awaitOrCancel(clusterAsyncCommands.scan(scanCursor, scanArgs), timeout.getSeconds(), TimeUnit.SECONDS);
                        keys.addAll(scanCursor.getKeys());
                    }
                });
            } else {
                RedisAdvancedClusterCommands<byte[], byte[]> sync = connection.sync();
                Partitions parse = ClusterPartitionParser.parse(sync.clusterNodes());

                parse.forEach(redisClusterNode -> {
                    RedisClusterCommands<byte[], byte[]> clusterCommands = sync.getConnection(redisClusterNode.getNodeId());

                    KeyScanCursor<byte[]> scanCursor = new KeyScanCursor<>();
                    scanCursor.setCursor(ScanCursor.INITIAL.getCursor());
                    ScanArgs scanArgs = ScanArgs.Builder.matches(pattern).limit(count);
                    while (!scanCursor.isFinished()) {
                        scanCursor = clusterCommands.scan(scanCursor, scanArgs);
                        keys.addAll(scanCursor.getKeys());
                    }
                });
            }
        }
        return keys;
    }

    public List<String> getNodes() {
        return nodes;
    }

    public void setNodes(List<String> nodes) {
        this.nodes = nodes;
    }

    public ClusterClientOptions getClusterClientOptions() {
        return clusterClientOptions;
    }

    public void setClusterClientOptions(ClusterClientOptions clusterClientOptions) {
        this.clusterClientOptions = clusterClientOptions;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public int getDatabase() {
        return database;
    }

    public void setDatabase(int database) {
        this.database = database;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isAsync() {
        return isAsync;
    }

    public void setIsAsync(boolean isAsync) {
        this.isAsync = isAsync;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public GenericObjectPoolConfig<StatefulRedisClusterConnection<byte[], byte[]>> getGenericObjectPoolConfig() {
        return genericObjectPoolConfig;
    }

    public void setGenericObjectPoolConfig(GenericObjectPoolConfig<StatefulRedisClusterConnection<byte[], byte[]>> genericObjectPoolConfig) {
        this.genericObjectPoolConfig = genericObjectPoolConfig;
    }

    public GenericObjectPool<StatefulRedisClusterConnection<byte[], byte[]>> getGenericObjectPool() {
        return genericObjectPool;
    }

    public void setGenericObjectPool(GenericObjectPool<StatefulRedisClusterConnection<byte[], byte[]>> genericObjectPool) {
        this.genericObjectPool = genericObjectPool;
    }
}
