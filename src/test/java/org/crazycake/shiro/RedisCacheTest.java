package org.crazycake.shiro;

import static fixture.TestFixture.*;
import static org.junit.Assert.fail;

import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.crazycake.shiro.exception.CacheManagerPrincipalIdNotAssignedException;
import org.crazycake.shiro.exception.PrincipalInstanceException;
import org.crazycake.shiro.model.FakeAuth;
import org.crazycake.shiro.model.UserInfo;
import org.crazycake.shiro.serializer.ObjectSerializer;
import org.crazycake.shiro.serializer.StringSerializer;
import org.junit.Before;
import org.junit.Test;

import com.github.javafaker.Faker;

/**
 * input key, value (java)
 * output value (java)
 */
public class RedisCacheTest {

    private RedisCache<PrincipalCollection, FakeAuth> redisCache;
    private RedisCache<PrincipalCollection, FakeAuth> redisCacheWithPrincipalIdFieldName;
    private RedisCache<PrincipalCollection, FakeAuth> redisCacheWithEmptyPrincipalIdFieldName;
    private RedisCache<PrincipalCollection, String> redisCacheWithStrings;

    private Properties properties = loadProperties("shiro-standalone.ini");
    private PrincipalCollection user1;
    private PrincipalCollection user2;
    private PrincipalCollection user3;
    private PrincipalCollection user4;

    private Set users1_2_3;
    private String prefix;

    private void blast() {
        blastRedis();
    }

    private void scaffold() {
        RedisManager redisManager = scaffoldStandaloneRedisManager();
        prefix = scaffoldPrefix();
        redisCache = scaffoldRedisCache(redisManager, new StringSerializer(), new ObjectSerializer(), prefix, NumberUtils.toInt(properties.getProperty("cacheManager.expire")), RedisCacheManager.DEFAULT_PRINCIPAL_ID_FIELD_NAME);
        redisCacheWithPrincipalIdFieldName = scaffoldRedisCache(redisManager, new StringSerializer(), new ObjectSerializer(), prefix, NumberUtils.toInt(properties.getProperty("cacheManager.expire")), properties.getProperty("cacheManager.principalIdFieldName"));
        redisCacheWithEmptyPrincipalIdFieldName = scaffoldRedisCache(redisManager, new StringSerializer(), new ObjectSerializer(), prefix, NumberUtils.toInt(properties.getProperty("cacheManager.expire")), "");
        redisCacheWithStrings = scaffoldRedisCache(redisManager, new StringSerializer(), new ObjectSerializer(), prefix, NumberUtils.toInt(properties.getProperty("cacheManager.expire")), properties.getProperty("cacheManager.principalIdFieldName"));
        user1 = scaffoldAuthKey(scaffoldUser());
        user2 = scaffoldAuthKey(scaffoldUser());
        user3 = scaffoldAuthKey(scaffoldUser());
        user4 = new SimplePrincipalCollection(Faker.instance().gameOfThrones().character(), Faker.instance().gameOfThrones().city());
        users1_2_3 = scaffoldKeys(user1, user2, user3);
    }


    @Before
    public void setUp() {
        blast();
        scaffold();
    }

    @Test
    public void testInitialize() {
        try {
            new RedisCache<String, String>(null, null, null, "abc:", 1, RedisCacheManager.DEFAULT_PRINCIPAL_ID_FIELD_NAME);
            fail("Excepted exception to be thrown");
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), "redisManager cannot be null.");
        }

        try {
            new RedisCache<String, String>(new RedisManager(), null, null, "abc:", 1, RedisCacheManager.DEFAULT_PRINCIPAL_ID_FIELD_NAME);
            fail("Excepted exception to be thrown");
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), "keySerializer cannot be null.");
        }

        try {
            new RedisCache<String, String>(new RedisManager(), new StringSerializer(), null, "abc:", 1, RedisCacheManager.DEFAULT_PRINCIPAL_ID_FIELD_NAME);
            fail("Excepted exception to be thrown");
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), "valueSerializer cannot be null.");
        }

        RedisCache rc = new RedisCache(new RedisManager(), new StringSerializer(), new ObjectSerializer(), "abc", 1, RedisCacheManager.DEFAULT_PRINCIPAL_ID_FIELD_NAME);
        assertEquals(rc.getKeyPrefix(), "abc");
    }

    @Test
    public void testPutNull() {
        doPutAuth(redisCache, null);
        assertRedisEmpty();
    }

    @Test
    public void testPut() {
        doPutAuth(redisCache, user1);
        FakeAuth fakeAuth = redisCache.get(user1);
        assertAuthEquals(fakeAuth, turnUserToFakeAuth((UserInfo)user1.getPrimaryPrincipal()));
    }
    
    @Test
    public void testPutString() {
        redisCacheWithStrings.put(user4, user4.getPrimaryPrincipal().toString());
        String auth = redisCacheWithStrings.get(user4);
        assertEquals(auth, user4.getPrimaryPrincipal());
    }

    @Test
    public void testSize() throws InterruptedException {
        doPutAuth(redisCache, user1);
        doPutAuth(redisCache, user2);
        Thread.sleep(800);
        assertEquals(redisCache.size(), 2);
    }

    @Test
    public void testPutInvalidPrincipal() {
        try {
            doPutAuth(redisCacheWithPrincipalIdFieldName, user3);
            fail();
        } catch (PrincipalInstanceException e) {
            assertPrincipalInstanceException(e);
        }
    }

    @Test
    public void testPutPrincipalWithEmptyIdFieldName() {
        try {
            doPutAuth(redisCacheWithEmptyPrincipalIdFieldName, user3);
            fail();
        } catch (CacheManagerPrincipalIdNotAssignedException e) {
            assertEquals(e.getMessage(), "CacheManager didn't assign Principal Id field name!");
        }
    }

    @Test
    public void testRemove() {
        doPutAuth(redisCache, user1);
        doRemoveAuth(redisCache, user1);
        assertRedisEmpty();
    }

    @Test
    public void testClear() {
        doClearAuth(redisCache);
        assertRedisEmpty();
    }

    @Test
    public void testKeys() {
        doPutAuth(redisCache, user1);
        doPutAuth(redisCache, user2);
        doPutAuth(redisCache, user3);
        Set actualKeys = doKeysAuth(redisCache);
        assertKeysEquals(actualKeys, turnPrincipalCollectionToString(users1_2_3, prefix));
    }


}
