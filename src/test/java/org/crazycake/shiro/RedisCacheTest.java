package org.crazycake.shiro;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class RedisCacheTest {

    private RedisManager redisManager;
    private RedisCache<String, FakeSession> redisCache;
    private String testKey;
    private StringSerializer keySerializer;
    private ObjectSerializer valueSerializer;
    private FakeSession testValue;
    private String testPrefix;
    private Set<byte[]> testSet;
    private Collection<FakeSession> testValues;
    private FakeSession tomSession;
    private FakeSession paulSession;
    private FakeSession billySession;
    private byte[] nullValueByte;

    @Before
    public void setUp() throws SerializationException, NoSuchFieldException, IllegalAccessException {
        testPrefix = "testPrefix:";
        testKey = "testKey";
        testValue = new FakeSession();
        testValue.setId(3);
        testValue.setName("jack");
        keySerializer = new StringSerializer();
        valueSerializer = new ObjectSerializer();
        testSet = new HashSet<byte[]>();
        testSet.add(keySerializer.serialize(testPrefix + "tom"));
        testSet.add(keySerializer.serialize(testPrefix + "paul"));
        testSet.add(keySerializer.serialize(testPrefix + "billy"));
        testValues = new ArrayList<FakeSession>();
        tomSession = new FakeSession(1, "tom");
        testValues.add(tomSession);
        paulSession = new FakeSession(2, "paul");
        testValues.add(paulSession);
        billySession = new FakeSession(3, "billy");
        testValues.add(billySession);
        redisManager = mock(RedisManager.class);
        when(redisManager.dbSize()).thenReturn(2L);
        when(redisManager.get(keySerializer.serialize(testPrefix + testKey))).thenReturn(valueSerializer.serialize(testValue));
        when(redisManager.keys(keySerializer.serialize(testPrefix + "*"))).thenReturn(testSet);
        when(redisManager.get(keySerializer.serialize(testPrefix + "tom"))).thenReturn(valueSerializer.serialize(tomSession));
        when(redisManager.get(keySerializer.serialize(testPrefix + "paul"))).thenReturn(valueSerializer.serialize(paulSession));
        when(redisManager.get(keySerializer.serialize(testPrefix + "billy"))).thenReturn(valueSerializer.serialize(billySession));
        redisCache = new RedisCache<String, FakeSession>(redisManager, keySerializer, valueSerializer, testPrefix, 1);

        nullValueByte = new byte[0];
    }

    @Test
    public void testRedisCache() {
        try {
            new RedisCache<String, String>(null, keySerializer, valueSerializer, "abc:", 1);
            fail("Excepted exception to be thrown");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(),is("Cache argument cannot be null."));
        }

        RedisCache rc = new RedisCache(new RedisManager(), keySerializer, valueSerializer, "abc", 1);
        assertThat(rc.getKeyPrefix(), is("abc"));
    }

    @Test
    public void testSize() {
        assertThat(redisCache.size(), is(2));
    }

    @Test
    public void testGet() {
        FakeSession actualValue = redisCache.get(testKey);
        assertThat(actualValue.getId(), is(3));
        assertThat(actualValue.getName(), is("jack"));

        FakeSession nullValue = redisCache.get(null);
        assertThat(nullValue, is(nullValue()));

        FakeSession nonExistValue = redisCache.get("nonExistKey");
        assertThat(nonExistValue, is(nullValue));
    }

    @Test
    public void testPut() {
        redisCache.put(null, null);
        verify(redisManager, times(1)).set(null, nullValueByte, 1);
        redisCache.put(null, new FakeSession());
        redisCache.put(testKey, testValue);
    }

    @Test
    public void testRemove() {
        redisCache.remove(null);
        FakeSession actualValue = redisCache.remove(testKey);
        assertThat(actualValue.getId(), is(3));
        assertThat(actualValue.getName(), is("jack"));
    }

    @Test
    public void testClear() {
        redisCache.clear();
    }

    @Test
    public void testKeys() {
        Set<String> keys = redisCache.keys();
        assertThat(keys.size(), is(3));
        assertThat(keys, hasItem(testPrefix + "tom"));
        assertThat(keys, hasItem(testPrefix + "paul"));
        assertThat(keys, hasItem(testPrefix + "billy"));
    }

    @Test
    public void testValues() {
        Collection<FakeSession> values = redisCache.values();
        assertThat(values.size(), is(3));
        for (Iterator<FakeSession> iterator = values.iterator(); iterator.hasNext(); ) {
            FakeSession next = iterator.next();
            if (next.getId() == 2) {
                assertThat(next.getName(), is("paul"));
            }
        }
    }
}
