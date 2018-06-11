package org.crazycake.shiro;

import org.crazycake.shiro.exception.SerializationException;
import org.crazycake.shiro.serializer.ObjectSerializer;
import org.crazycake.shiro.serializer.StringSerializer;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class RedisManagerTest {

    private RedisManager redisManager;
    private JedisPool jedisPool;
    private Jedis jedis;
    private String testPrefix;
    private StringSerializer keySerializer;
    private ObjectSerializer valueSerializer;

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException, SerializationException {
        testPrefix = "testPrefix";
        keySerializer = new StringSerializer();
        valueSerializer = new ObjectSerializer();
        jedis = mock(Jedis.class);
        jedisPool = mock(JedisPool.class);
        when(jedisPool.getResource()).thenReturn(jedis);
        redisManager = new RedisManager();
        TestUtils.setPrivateField(redisManager, "jedisPool", jedisPool);

    }

    @Test
    public void testGet() throws SerializationException {
        byte[] nullValue = redisManager.get(null);
        assertThat(nullValue, is(nullValue));
        verify(jedis, times(0)).get(any((new byte[0]).getClass()));

        byte[] testKey = keySerializer.serialize("1");
        byte[] testValue = valueSerializer.serialize("abc");
        when(jedis.get(testKey)).thenReturn(testValue);
        byte[] actualValue = redisManager.get(testKey);
        assertThat(actualValue, is(testValue));
        verify(jedis, times(1)).get(testKey);
        verify(jedis, times(1)).close();
    }

    @Test
    public void testSet() throws SerializationException {
        redisManager.set(null, null, 0);
        verify(jedis, times(0)).set(any((new byte[0]).getClass()), any((new byte[0]).getClass()));
        verify(jedis, times(0)).expire(any((new byte[0]).getClass()), any(int.class));

        byte[] testKey = keySerializer.serialize("1");
        redisManager.set(testKey, null, 0);
        verify(jedis, times(1)).set(any((new byte[0]).getClass()), any((new byte[0]).getClass()));
        verify(jedis, times(1)).expire(any((new byte[0]).getClass()), any(int.class));

        byte[] testValue = valueSerializer.serialize("abc");
        redisManager.set(testKey, testValue, 700);
        verify(jedis, times(2)).set(any((new byte[0]).getClass()), any((new byte[0]).getClass()));
        verify(jedis, times(2)).expire(any((new byte[0]).getClass()), any(int.class));

        redisManager.set(testKey, testValue, -1);
        verify(jedis, times(3)).set(any((new byte[0]).getClass()), any((new byte[0]).getClass()));
        verify(jedis, times(2)).expire(any((new byte[0]).getClass()), any(int.class));
    }

    @Test
    public void testDel() throws SerializationException {
        redisManager.del(null);
        verify(jedis, times(0)).del(any((new byte[0]).getClass()));
        redisManager.del(keySerializer.serialize("1"));
        verify(jedis, times(1)).del(any((new byte[0]).getClass()));
    }

    @Test
    public void testKeys() throws SerializationException {
        ScanResult<byte[]> scanResult = mock(ScanResult.class);
        when(jedis.scan(any(byte[].class), any(ScanParams.class))).thenReturn(scanResult);
        when(scanResult.getStringCursor()).thenReturn("0");
        List<byte[]> testResults = new ArrayList<byte[]>();
        testResults.add(keySerializer.serialize("1"));
        testResults.add(keySerializer.serialize("2"));
        testResults.add(keySerializer.serialize("3"));
        when(scanResult.getResult()).thenReturn(testResults);
        Set<byte[]> keys = redisManager.keys(keySerializer.serialize(testPrefix + "*"));
        assertThat(keys.size(), is(3));
    }
}
