package org.crazycake.shiro.common;

import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.crazycake.shiro.IRedisManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Abstract class of RedisManager.
 */
public abstract class WorkAloneLettuceRedisManager implements IRedisManager {

    /**
     * Get StatefulRedisConnection
     *
     * @return StatefulRedisConnection
     */
    protected abstract StatefulRedisConnection<byte[], byte[]> getConnection();

    /**
     * Default value of count.
     */
    protected static final int DEFAULT_COUNT = 100;

    /**
     * The number of elements returned at every iteration.
     */
    private int count = DEFAULT_COUNT;

    /**
     * poolConfig
     */
    private GenericObjectPoolConfig<StatefulRedisConnection<byte[], byte[]>> poolConfig = new GenericObjectPoolConfig<>();

    /**
     * get value from redis
     *
     * @param key key
     * @return value
     */
    @Override
    public byte[] get(byte[] key) {
        if (key == null) {
            return null;
        }
        byte[] value;
        StatefulRedisConnection<byte[], byte[]> connection = getConnection();
        try {
            RedisCommands<byte[], byte[]> commands = connection.sync();
            value = commands.get(key);
        } finally {
            connection.closeAsync();
        }
        return value;
    }

    /**
     * set
     *
     * @param key        key
     * @param value      value
     * @param expireTime expire time
     * @return value
     */
    @Override
    public byte[] set(byte[] key, byte[] value, int expireTime) {
        if (key == null) {
            return null;
        }
        StatefulRedisConnection<byte[], byte[]> connection = getConnection();
        try {
            RedisCommands<byte[], byte[]> commands = connection.sync();
            commands.set(key, value);
            if (expireTime > 0) {
                commands.expire(key, expireTime);
            }
        } finally {
            connection.closeAsync();
        }
        return value;
    }

    /**
     * Delete a key-value pair.
     *
     * @param key key
     */
    @Override
    public void del(byte[] key) {
        if (key == null) {
            return;
        }
        StatefulRedisConnection<byte[], byte[]> connection = getConnection();
        try {
            RedisCommands<byte[], byte[]> commands = connection.sync();
            commands.del(key);
        } finally {
            connection.closeAsync();
        }
    }

    /**
     * Return the size of redis db.
     *
     * @param pattern key pattern
     * @return key-value size
     */
    @Override
    public Long dbSize(byte[] pattern) {
        long dbSize = 0L;
        StatefulRedisConnection<byte[], byte[]> connection = getConnection();
        try {
            RedisCommands<byte[], byte[]> commands = connection.sync();
            ScanArgs scanArgs = ScanArgs.Builder.limit(count).match(new String(pattern));
            KeyScanCursor<byte[]> cursor = null;
            do {
                if (cursor == null) {
                    cursor = commands.scan(scanArgs);
                } else {
                    cursor = commands.scan(cursor, scanArgs);
                }
                List<byte[]> list = cursor.getKeys();
                dbSize += list.size();
            } while (!cursor.isFinished());
        } finally {
            connection.closeAsync();
        }
        return dbSize;
    }

    /**
     * Return all the keys of Redis db. Filtered by pattern.
     *
     * @param pattern key pattern
     * @return key set
     */
    @Override
    public Set<byte[]> keys(byte[] pattern) {
        Set<byte[]> keys = new HashSet<>();
        StatefulRedisConnection<byte[], byte[]> connection = getConnection();
        try {
            RedisCommands<byte[], byte[]> commands = connection.sync();
            ScanArgs scanArgs = ScanArgs.Builder.limit(count).match(new String(pattern));
            KeyScanCursor<byte[]> cursor = null;
            do {
                if (cursor == null) {
                    cursor = commands.scan(scanArgs);
                } else {
                    cursor = commands.scan(cursor, scanArgs);
                }
                List<byte[]> list = cursor.getKeys();
                keys.addAll(list);
            } while (!cursor.isFinished());
        } finally {
            connection.closeAsync();
        }
        return keys;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public GenericObjectPoolConfig<StatefulRedisConnection<byte[], byte[]>> getPoolConfig() {
        return poolConfig;
    }

    public void setPoolConfig(GenericObjectPoolConfig<StatefulRedisConnection<byte[], byte[]>> poolConfig) {
        this.poolConfig = poolConfig;
    }
}
