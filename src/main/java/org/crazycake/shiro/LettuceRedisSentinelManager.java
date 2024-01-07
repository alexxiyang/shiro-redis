package org.crazycake.shiro;

import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.masterreplica.MasterReplica;
import io.lettuce.core.masterreplica.StatefulRedisMasterReplicaConnection;
import io.lettuce.core.support.ConnectionPoolSupport;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.crazycake.shiro.common.AbstractLettuceRedisManager;
import org.crazycake.shiro.exception.PoolException;

import java.util.List;
import java.util.Objects;

/**
 * @author Teamo
 * @since 2022/05/19
 */
public class LettuceRedisSentinelManager extends AbstractLettuceRedisManager {
    private static final String DEFAULT_MASTER_NAME = "mymaster";

    private String masterName = DEFAULT_MASTER_NAME;

    private List<String> nodes;

    private String sentinelPassword;

    private ReadFrom readFrom = ReadFrom.REPLICA_PREFERRED;

    /**
     * GenericObjectPool.
     */
    private volatile GenericObjectPool<StatefulRedisMasterReplicaConnection<byte[], byte[]>> genericObjectPool;

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void initialize() {
        if (genericObjectPool == null) {
            synchronized (LettuceRedisSentinelManager.class) {
                if (genericObjectPool == null) {
                    RedisURI redisURI = this.createSentinelRedisURI();
                    RedisClient redisClient = RedisClient.create(redisURI);
                    redisClient.setOptions(getClientOptions());
                    StatefulRedisMasterReplicaConnection<byte[], byte[]> connect = MasterReplica.connect(redisClient, new ByteArrayCodec(), redisURI);
                    connect.setReadFrom(readFrom);
                    GenericObjectPoolConfig genericObjectPoolConfig = getGenericObjectPoolConfig();
                    genericObjectPool = ConnectionPoolSupport.createGenericObjectPool(() -> connect, genericObjectPoolConfig);
                }
            }
        }
    }

    @Override
    protected StatefulRedisMasterReplicaConnection<byte[], byte[]> getStatefulConnection() {
        if (genericObjectPool == null) {
            initialize();
        }
        try {
            return genericObjectPool.borrowObject();
        } catch (Exception e) {
            throw new PoolException("Could not get a resource from the pool", e);
        }
    }

    private RedisURI createSentinelRedisURI() {
        Objects.requireNonNull(nodes, "nodes must not be null!");

        RedisURI.Builder builder = RedisURI.builder();
        for (String node : nodes) {
            String[] hostAndPort = node.split(":");

            RedisURI.Builder sentinelBuilder = RedisURI.Builder.redis(hostAndPort[0], Integer.parseInt(hostAndPort[1]));

            if (sentinelPassword != null) {
                sentinelBuilder.withPassword(sentinelPassword.toCharArray());
            }

            builder.withSentinel(sentinelBuilder.build());
        }

        String password = getPassword();
        if (password != null) {
            builder.withPassword(password.toCharArray());
        }
        return builder.withSentinelMasterId(masterName).withDatabase(getDatabase()).build();
    }

    public String getMasterName() {
        return masterName;
    }

    public void setMasterName(String masterName) {
        this.masterName = masterName;
    }

    public List<String> getNodes() {
        return nodes;
    }

    public void setNodes(List<String> nodes) {
        this.nodes = nodes;
    }

    public String getSentinelPassword() {
        return sentinelPassword;
    }

    public void setSentinelPassword(String sentinelPassword) {
        this.sentinelPassword = sentinelPassword;
    }

    public ReadFrom getReadFrom() {
        return readFrom;
    }

    public void setReadFrom(ReadFrom readFrom) {
        this.readFrom = readFrom;
    }
}
