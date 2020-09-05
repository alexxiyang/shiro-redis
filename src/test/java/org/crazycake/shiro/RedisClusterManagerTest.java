package org.crazycake.shiro;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.JedisCluster;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class RedisClusterManagerTest {

    private RedisClusterManager redisClusterManager;
    private JedisCluster jedisCluster;

    @BeforeEach
    public void setUp() {
        jedisCluster = mock(JedisCluster.class);
        redisClusterManager = new RedisClusterManager();
        redisClusterManager.setJedisCluster(jedisCluster);
    }

    @Test
    public void get() {
        byte[] value = redisClusterManager.get(null);
        assertThat(value, is(nullValue()));
        byte[] testKey = "123".getBytes();
        byte[] expectValue = "abc".getBytes();
        when(jedisCluster.get(testKey)).thenReturn(expectValue);
        byte[] testValue = redisClusterManager.get(testKey);
        assertThat(testValue, is(expectValue));
    }

    @Test
    public void set() {
        redisClusterManager.set(null, null, -1);
        verify(jedisCluster, times(0)).set(any(byte[].class), any(byte[].class));
        redisClusterManager.set(new byte[0], new byte[0], -1);
        verify(jedisCluster, times(1)).set(any(byte[].class), any(byte[].class));
        verify(jedisCluster, times(0)).expire(any(byte[].class), any(int.class));
        redisClusterManager.set(new byte[0], new byte[0], 0);
        verify(jedisCluster, times(1)).expire(any(byte[].class), any(int.class));
        redisClusterManager.set(new byte[0], new byte[0], 1);
        verify(jedisCluster, times(2)).expire(any(byte[].class), any(int.class));
    }
}