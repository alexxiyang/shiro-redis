package org.crazycake.shiro;

import org.apache.shiro.cache.Cache;
import org.crazycake.shiro.common.IRedisManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.*;

public class RedisCacheManagerTest {

    private IRedisManager redisManager;
    private RedisCacheManager redisCacheManager;

    @BeforeEach
    public void setUp() {
        redisManager = mock(IRedisManager.class);
    }

    @Test
    public void testInitWithoutSettingRedisManager() {
        redisCacheManager = new RedisCacheManager();
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            redisCacheManager.getCache("testCache");
        });
    }

    @Test
    public void testInit() {
        redisCacheManager = new RedisCacheManager();
        redisCacheManager.setRedisManager(redisManager);
        redisCacheManager.setKeyPrefix("testRedisManager1:");
        redisCacheManager.setPrincipalIdFieldName("id");
        Cache testCache = redisCacheManager.getCache("testCache");
        assertThat(testCache.getClass().getName(), is("org.crazycake.shiro.RedisCache"));
        RedisCache redisTestCache = (RedisCache) testCache;
        assertThat(redisTestCache.getKeyPrefix(), is("testRedisManager1:testCache:"));
        assertThat(redisTestCache.getPrincipalIdFieldName(), is("id"));
    }

}
