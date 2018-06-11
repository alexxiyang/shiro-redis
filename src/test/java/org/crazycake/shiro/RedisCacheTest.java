package org.crazycake.shiro;

import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.crazycake.shiro.exception.PrincipalInstanceException;
import org.crazycake.shiro.exception.SerializationException;
import org.crazycake.shiro.model.*;
import org.crazycake.shiro.serializer.ObjectSerializer;
import org.crazycake.shiro.serializer.StringSerializer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class RedisCacheTest {

    private RedisManager redisManager;
    private RedisCache<String, FakeAuth> redisCache;
    private StringSerializer keySerializer;
    private ObjectSerializer valueSerializer;
    private String testPrefix;

    private String tomKey;
    private byte[] tomKeyBytes;
    private String paulKey;
    private String billyKey;

    @Before
    public void setUp() throws SerializationException, NoSuchFieldException, IllegalAccessException {
        keySerializer = new StringSerializer();
        valueSerializer = new ObjectSerializer();
        redisManager = mock(RedisManager.class);
        testPrefix = "testPrefix:";
        redisCache = new RedisCache<String, FakeAuth>(redisManager, keySerializer, valueSerializer, testPrefix, 1, RedisCacheManager.DEFAULT_PRINCIPAL_ID_FIELD_NAME);

        Set<byte[]> testSet;
        testSet = new HashSet<byte[]>();
        tomKey = testPrefix + "tom";
        tomKeyBytes = keySerializer.serialize(tomKey);
        testSet.add(tomKeyBytes);
        paulKey = testPrefix + "paul";
        testSet.add(keySerializer.serialize(paulKey));
        billyKey = testPrefix + "billy";
        testSet.add(keySerializer.serialize(billyKey));
        byte[] testKeysBytes = keySerializer.serialize(testPrefix + "*");
        when(redisManager.keys(testKeysBytes)).thenReturn(testSet);
    }

    @Test
    public void testRedisCache() {
        try {
            new RedisCache<String, String>(null, keySerializer, valueSerializer, "abc:", 1, RedisCacheManager.DEFAULT_PRINCIPAL_ID_FIELD_NAME);
            fail("Excepted exception to be thrown");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(),is("redisManager cannot be null."));
        }

        try {
            new RedisCache<String, String>(new RedisManager(), null, valueSerializer, "abc:", 1, RedisCacheManager.DEFAULT_PRINCIPAL_ID_FIELD_NAME);
            fail("Excepted exception to be thrown");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(),is("keySerializer cannot be null."));
        }

        try {
            new RedisCache<String, String>(new RedisManager(), keySerializer, null, "abc:", 1, RedisCacheManager.DEFAULT_PRINCIPAL_ID_FIELD_NAME);
            fail("Excepted exception to be thrown");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(),is("valueSerializer cannot be null."));
        }

        RedisCache rc = new RedisCache(new RedisManager(), keySerializer, valueSerializer, "abc", 1, RedisCacheManager.DEFAULT_PRINCIPAL_ID_FIELD_NAME);
        assertThat(rc.getKeyPrefix(), is("abc"));
    }

    @Test
    public void testSize() throws SerializationException {
        when(redisManager.dbSize(keySerializer.serialize(testPrefix + "*"))).thenReturn(2L);
        assertThat(redisCache.size(), is(2));
    }

    @Test
    public void testGet() throws SerializationException {
        FakeAuth nullValue = redisCache.get(null);
        assertThat(nullValue, nullValue());

        byte[] adminKeyBytes = keySerializer.serialize(testPrefix + "admin");
        FakeAuth adminFakeAuth = new FakeAuth(1, "admin");
        byte[] adminValueBytes = valueSerializer.serialize(adminFakeAuth);
        when(redisManager.get(adminKeyBytes)).thenReturn(adminValueBytes);

        FakeAuth actualValue = redisCache.get("admin");
        assertThat(actualValue.getId(), is(1));
        assertThat(actualValue.getRole(), is("admin"));

        FakeAuth nonExistValue = redisCache.get("nonExistKey");
        assertThat(nonExistValue, is(nullValue));
    }

    @Test
    public void testPut() throws SerializationException {
        redisCache.put(null, null);
        verify(redisManager, times(0)).set(null, null, 1);

        FakeAuth emptyFakeAuth = new FakeAuth();
        byte[] emptyFakeAuthBytes = valueSerializer.serialize(emptyFakeAuth);
        redisCache.put(null, emptyFakeAuth);
        verify(redisManager, times(0)).set(null, emptyFakeAuthBytes, 1);

        String testKey = "jack";
        byte[] testKeyBytes = keySerializer.serialize(testPrefix + testKey);
        redisCache.put(testKey, null);
        verify(redisManager, times(1)).set(testKeyBytes, null, 1);

        FakeAuth testValue = new FakeAuth(2, "user");
        byte[] testValueBytes = valueSerializer.serialize(testValue);
        redisCache.put(testKey, testValue);
        verify(redisManager, times(1)).set(testKeyBytes, testValueBytes, 1);
    }

    @Test
    public void testPutInvalidPrincipal() throws SerializationException {
        FakeAuth testValue = new FakeAuth(3, "ted");

        try {
            RedisCache<PrincipalCollection, FakeAuth> redisCache = new RedisCache<PrincipalCollection, FakeAuth>(redisManager, keySerializer, valueSerializer, testPrefix, 1, RedisCacheManager.DEFAULT_PRINCIPAL_ID_FIELD_NAME);
            SimplePrincipalCollection testPrincipalCollection = new SimplePrincipalCollection();
            FakeInvalidPrincipal invalidPrincipal = new FakeInvalidPrincipal();
            testPrincipalCollection.add(invalidPrincipal, "realm1");
            redisCache.put(testPrincipalCollection, testValue);
            fail();
        } catch (PrincipalInstanceException e) {
            assertThat(e, is(notNullValue()));
        }
        verify(redisManager, times(0)).set(Matchers.any(byte[].class), Matchers.any(byte[].class), anyInt());
    }

    @Test
    public void testPutDefaultPrincipal1() throws SerializationException {
        FakeAuth testValue = new FakeAuth(3, "ted");

        RedisCache<PrincipalCollection, FakeAuth> redisCache = new RedisCache<PrincipalCollection, FakeAuth>(redisManager, keySerializer, valueSerializer, testPrefix, 1, RedisCacheManager.DEFAULT_PRINCIPAL_ID_FIELD_NAME);
        SimplePrincipalCollection testPrincipalCollection = new SimplePrincipalCollection();
        FakePrincipalWithDefaultId1 p = new FakePrincipalWithDefaultId1();
        p.setAuthCacheKey("abc");
        testPrincipalCollection.add(p, "realm1");
        redisCache.put(testPrincipalCollection, testValue);

        byte[] testKeyBytes = keySerializer.serialize(testPrefix + "abc");
        verify(redisManager, times(1)).set(eq(testKeyBytes), Matchers.any(byte[].class), anyInt());
    }

    @Test
    public void testPutDefaultPrincipal2() throws SerializationException {
        FakeAuth testValue = new FakeAuth(3, "ted");

        RedisCache<PrincipalCollection, FakeAuth> redisCache = new RedisCache<PrincipalCollection, FakeAuth>(redisManager, keySerializer, valueSerializer, testPrefix, 1, RedisCacheManager.DEFAULT_PRINCIPAL_ID_FIELD_NAME);
        SimplePrincipalCollection testPrincipalCollection = new SimplePrincipalCollection();
        FakePrincipalWithDefaultId2 p = new FakePrincipalWithDefaultId2();
        p.setId(1);
        testPrincipalCollection.add(p, "realm1");
        redisCache.put(testPrincipalCollection, testValue);

        byte[] testKeyBytes = keySerializer.serialize(testPrefix + "1");
        verify(redisManager, times(1)).set(eq(testKeyBytes), Matchers.any(byte[].class), anyInt());
    }

    @Test
    public void testPutPrincipalWithEmptyIdFieldName() throws SerializationException {
        FakeAuth testValue = new FakeAuth(3, "ted");

        RedisCache<PrincipalCollection, FakeAuth> redisCache = new RedisCache<PrincipalCollection, FakeAuth>(redisManager, keySerializer, valueSerializer, testPrefix, 1, "");
        SimplePrincipalCollection testPrincipalCollection = new SimplePrincipalCollection();
        FakePrincipalWithDefaultId2 p = new FakePrincipalWithDefaultId2();
        p.setId(1);
        testPrincipalCollection.add(p, "realm1");
        redisCache.put(testPrincipalCollection, testValue);

        byte[] testKeyBytes = keySerializer.serialize(testPrefix + "1");
        verify(redisManager, times(1)).set(eq(testKeyBytes), Matchers.any(byte[].class), anyInt());
    }

    @Test
    public void testPutPrincipalWith1CharIdFieldName() throws SerializationException {
        FakeAuth testValue = new FakeAuth(3, "ted");

        RedisCache<PrincipalCollection, FakeAuth> redisCache = new RedisCache<PrincipalCollection, FakeAuth>(redisManager, keySerializer, valueSerializer, testPrefix, 1, "i");
        SimplePrincipalCollection testPrincipalCollection = new SimplePrincipalCollection();
        FakePrincipalWith1CharId p = new FakePrincipalWith1CharId();
        p.setI(1);
        testPrincipalCollection.add(p, "realm1");
        redisCache.put(testPrincipalCollection, testValue);

        byte[] testKeyBytes = keySerializer.serialize(testPrefix + "1");
        verify(redisManager, times(1)).set(eq(testKeyBytes), Matchers.any(byte[].class), anyInt());
    }

    @Test
    public void testPutPrincipalWithWrongIdFieldName() throws SerializationException {
        FakeAuth testValue = new FakeAuth(3, "ted");

        try {
            RedisCache<PrincipalCollection, FakeAuth> redisCache = new RedisCache<PrincipalCollection, FakeAuth>(redisManager, keySerializer, valueSerializer, testPrefix, 1, "ddd");
            SimplePrincipalCollection testPrincipalCollection = new SimplePrincipalCollection();
            FakePrincipalWithDefaultId2 p = new FakePrincipalWithDefaultId2();
            p.setId(1);
            testPrincipalCollection.add(p, "realm1");
            redisCache.put(testPrincipalCollection, testValue);
            fail();
        } catch (PrincipalInstanceException e) {
            assertThat(e, is(notNullValue()));
        }

        verify(redisManager, times(0)).set(Matchers.any(byte[].class), Matchers.any(byte[].class), anyInt());
    }

    @Test
    public void testPutPrincipalCollection() throws SerializationException {
        RedisCache<PrincipalCollection, FakeAuth> principalRedisCache = new RedisCache<PrincipalCollection, FakeAuth>(redisManager, keySerializer, valueSerializer, testPrefix, 1, "userId");
        FakeAuth testValue = new FakeAuth(3, "user");
        byte[] testValueBytes = valueSerializer.serialize(testValue);
        try {
            SimplePrincipalCollection testKey = new SimplePrincipalCollection();
            testKey.add(new Object(), "realm1");
            principalRedisCache.put(testKey, testValue);
            fail();
        } catch (PrincipalInstanceException e) {
            System.out.println(e.getMessage());
            assertThat(e, is(notNullValue()));
        }

        SimplePrincipalCollection testKey = new SimplePrincipalCollection();
        FakePrincipal fakePrincipal = new FakePrincipal();
        fakePrincipal.setUsername("admin");
        fakePrincipal.setUserId(1);
        testKey.add(fakePrincipal, "realm1");
        byte[] testKeyBytes = keySerializer.serialize(testPrefix + "1");
        principalRedisCache.put(testKey, testValue);
        verify(redisManager, times(1)).set(testKeyBytes, testValueBytes, 1);
    }

    @Test
    public void testRemove() throws SerializationException {
        FakeAuth nullValue = redisCache.remove(null);
        assertThat(nullValue, is(nullValue()));

        String testKey = "billy";
        byte[] testKeyBytes = keySerializer.serialize(testPrefix + testKey);
        FakeAuth testValue = new FakeAuth(3, "client");
        byte[] testValueBytes = valueSerializer.serialize(testValue);
        when(redisManager.get(testKeyBytes)).thenReturn(testValueBytes);
        FakeAuth actualValue = redisCache.remove(testKey);
        assertThat(actualValue.getId(), is(3));
        assertThat(actualValue.getRole(), is("client"));
    }

    @Test
    public void testClear() throws SerializationException {
        redisCache.clear();
        verify(redisManager, times(1)).del(tomKeyBytes);
    }

    @Test
    public void testKeys() throws SerializationException {
        Set<String> keys = redisCache.keys();
        assertThat(keys.size(), is(3));
        assertThat(keys, hasItem(testPrefix + "tom"));
        assertThat(keys, hasItem(testPrefix + "paul"));
        assertThat(keys, hasItem(testPrefix + "billy"));
    }

    @Test
    public void testValues() throws SerializationException {
        FakeAuth tomFakeAuth = new FakeAuth(1, "admin");
        mockRedisManagerGet(tomKey, tomFakeAuth);
        FakeAuth paulFakeAuth = new FakeAuth(2, "client");
        mockRedisManagerGet(paulKey, paulFakeAuth);
        FakeAuth billyFakeAuth = new FakeAuth(3, "user");
        mockRedisManagerGet(billyKey, billyFakeAuth);
        Collection<FakeAuth> values = redisCache.values();
        assertThat(values.size(), is(3));
    }

    private void mockRedisManagerGet(String key, FakeAuth value) throws SerializationException {
        byte[] keyByte = keySerializer.serialize(key);
        byte[] valueByte = valueSerializer.serialize(value);
        when(redisManager.get(keyByte)).thenReturn(valueByte);
    }
}
