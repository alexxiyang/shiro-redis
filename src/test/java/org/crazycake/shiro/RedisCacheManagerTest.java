package org.crazycake.shiro;

import org.apache.shiro.cache.Cache;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static fixture.TestFixture.*;

public class RedisCacheManagerTest {

    private RedisManager redisManager;
    private RedisCacheManager redisCacheManager;

    @Before
    public void setUp() {
        redisManager = Mockito.mock(RedisManager.class);
        redisCacheManager = new RedisCacheManager();
        redisCacheManager.setRedisManager(redisManager);
    }

    @After
    public void tearDown() {
        blastRedis();
    }

    @Test
    public void testGetCache() {
        Cache cache = redisCacheManager.getCache("testCache1");
        Cache cache1 = redisCacheManager.getCache("testCache1");
        assertThat(cache,is(cache1));

        redisCacheManager.setKeyPrefix("testRedisManager1:");
        Cache cache2 = redisCacheManager.getCache("testCache2");
        assertThat(cache2.getClass().getName(), is("org.crazycake.shiro.RedisCache"));

        RedisCache redisCache2 = (RedisCache) cache2;
        assertThat(redisCache2.getKeyPrefix(), is("testRedisManager1:testCache2:"));
    }

}
