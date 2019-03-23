package org.crazycake.shiro;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.JedisCluster;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static fixture.TestFixture.*;

public class RedisClusterManagerTest {

    private RedisClusterManager redisClusterManager;
    private JedisCluster jedisCluster;

    @Before
    public void setUp() throws Exception {
        jedisCluster = mock(JedisCluster.class);
        redisClusterManager = new RedisClusterManager();
        redisClusterManager.setJedisCluster(jedisCluster);
    }

    @After
    public void tearDown() {
        blastRedis();
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
        redisClusterManager.set(new byte[0], null, -1);
        verify(jedisCluster, times(1)).set(any(byte[].class), any(byte[].class));
        verify(jedisCluster, times(0)).expire(any(byte[].class), any(int.class));
        redisClusterManager.set(new byte[0], null, 0);
        verify(jedisCluster, times(1)).expire(any(byte[].class), any(int.class));
        redisClusterManager.set(new byte[0], null, 1);
        verify(jedisCluster, times(2)).expire(any(byte[].class), any(int.class));
    }
}