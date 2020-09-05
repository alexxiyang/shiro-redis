package org.crazycake.shiro.integration;

import com.github.javafaker.Faker;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.crazycake.shiro.RedisCache;
import org.crazycake.shiro.RedisCacheManager;
import org.crazycake.shiro.RedisManager;
import org.crazycake.shiro.exception.CacheManagerPrincipalIdNotAssignedException;
import org.crazycake.shiro.exception.PrincipalInstanceException;
import org.crazycake.shiro.integration.fixture.model.FakeAuth;
import org.crazycake.shiro.integration.fixture.model.UserInfo;
import org.crazycake.shiro.serializer.ObjectSerializer;
import org.crazycake.shiro.serializer.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;
import java.util.Set;

import static org.crazycake.shiro.integration.fixture.TestFixture.*;

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


    @BeforeEach
    public void setUp() {
        blast();
        scaffold();
    }

    @AfterEach
    public void tearDown() {
        blast();
    }


    @Test
    public void testInitialize() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new RedisCache<String, String>(null, null, null, "abc:", 1, RedisCacheManager.DEFAULT_PRINCIPAL_ID_FIELD_NAME);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new RedisCache<String, String>(new RedisManager(), null, null, "abc:", 1, RedisCacheManager.DEFAULT_PRINCIPAL_ID_FIELD_NAME);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new RedisCache<String, String>(new RedisManager(), new StringSerializer(), null, "abc:", 1, RedisCacheManager.DEFAULT_PRINCIPAL_ID_FIELD_NAME);
        });

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
        Assertions.assertThrows(PrincipalInstanceException.class, () -> {
            doPutAuth(redisCacheWithPrincipalIdFieldName, user3);
        });
    }

    @Test
    public void testPutPrincipalWithEmptyIdFieldName() {
        Assertions.assertThrows(CacheManagerPrincipalIdNotAssignedException.class, () -> {
            doPutAuth(redisCacheWithEmptyPrincipalIdFieldName, user3);
        });
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
