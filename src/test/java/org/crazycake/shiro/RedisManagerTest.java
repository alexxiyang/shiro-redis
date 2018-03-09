package org.crazycake.shiro;

import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class RedisManagerTest {

    private RedisManager redisManager;
    private JedisPool jedisPool;
    private Jedis jedis;
    private byte[] testKey;
    private byte[] testValue;
    private String testPrefix;
    private Set<byte[]> testSet;
    private StringSerializer keySerializer;
    private ObjectSerializer valueSerializer;

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException, SerializationException {
        testPrefix = "testPrefix";
        keySerializer = new StringSerializer();
        valueSerializer = new ObjectSerializer();
        testKey = keySerializer.serialize("testKey");
        testValue = valueSerializer.serialize(new FakeSession(1, "jack"));
        jedis = mock(Jedis.class);
        when(jedis.get(testKey)).thenReturn(testValue);
        when(jedis.dbSize()).thenReturn(3L);
        testSet = new HashSet<byte[]>();
        testSet.add(keySerializer.serialize(testPrefix + "tom"));
        testSet.add(keySerializer.serialize(testPrefix + "paul"));
        testSet.add(keySerializer.serialize(testPrefix + "billy"));
        when(jedis.keys(keySerializer.serialize(testPrefix + "*"))).thenReturn(testSet);
        jedisPool = mock(JedisPool.class);
        when(jedisPool.getResource()).thenReturn(jedis);
        redisManager = new RedisManager();
        TestUtils.setPrivateField(redisManager, "jedisPool", jedisPool);

    }

    @Test
    public void testGet() {
        byte[] actualValue = redisManager.get(testKey);
        assertThat(actualValue, is(testValue));
        byte[] nullValue = redisManager.get(null);
        assertThat(nullValue, is(nullValue));
    }

    @Test
    public void testSet() {
        redisManager.set(testKey, testValue, 0);
        redisManager.set(null, null, 0);
        redisManager.set(testKey, testValue, 0);
        verify(jedis, times(0)).expire(testKey, 0);
        redisManager.set(testKey, testValue, 700);
        verify(jedis, times(1)).expire(testKey, 700);
    }

    @Test
    public void testDel() throws SerializationException {
        redisManager.del(null);
        verify(jedis, times(0)).del("");
        redisManager.del(keySerializer.serialize("1"));
        verify(jedis, times(1)).del(keySerializer.serialize("1"));
    }

    @Test
    public void testDbSize() {
        Long actualDbSize = redisManager.dbSize();
        assertThat(actualDbSize, is(3L));
    }

    @Test
    public void testKeys() throws SerializationException {
        ScanResult<byte[]> scanResult = mock(ScanResult.class);
        when(jedis.scan(any(byte[].class), any(ScanParams.class))).thenReturn(scanResult);
        when(scanResult.getStringCursor()).thenReturn("0");
        Set<byte[]> keys = redisManager.keys(keySerializer.serialize(testPrefix + "*"));
        assertThat(keys.size(), is(0));
    }
}
