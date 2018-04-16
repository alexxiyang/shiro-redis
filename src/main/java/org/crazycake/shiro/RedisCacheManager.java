package org.crazycake.shiro;

import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheException;
import org.apache.shiro.cache.CacheManager;
import org.crazycake.shiro.serializer.ObjectSerializer;
import org.crazycake.shiro.serializer.RedisSerializer;
import org.crazycake.shiro.serializer.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RedisCacheManager implements CacheManager {

	private final Logger logger = LoggerFactory.getLogger(RedisCacheManager.class);

	// fast lookup by name map
	private final ConcurrentMap<String, Cache> caches = new ConcurrentHashMap<String, Cache>();
	private RedisSerializer keySerializer = new StringSerializer();
	private RedisSerializer valueSerializer = new ObjectSerializer();

	private IRedisManager redisManager;

	// expire time in seconds
	private static final int DEFAULT_EXPIRE = 1800;
	private int expire = DEFAULT_EXPIRE;

	/**
	 * The Redis key prefix for caches
	 */
	public static final String DEFAULT_CACHE_KEY_PREFIX = "shiro:cache:";
	private String keyPrefix = DEFAULT_CACHE_KEY_PREFIX;

	@Override
	public <K, V> Cache<K, V> getCache(String name) throws CacheException {
		logger.debug("get cache, name=" + name);

		Cache cache = caches.get(name);

		if (cache == null) {
			cache = new RedisCache<K, V>(redisManager, keySerializer, valueSerializer, keyPrefix + name + ":", expire);
			caches.put(name, cache);
		}
		return cache;
	}

	public IRedisManager getRedisManager() {
		return redisManager;
	}

	public void setRedisManager(IRedisManager redisManager) {
		this.redisManager = redisManager;
	}

	public String getKeyPrefix() {
		return keyPrefix;
	}

	public void setKeyPrefix(String keyPrefix) {
		this.keyPrefix = keyPrefix;
	}

	public RedisSerializer getKeySerializer() {
		return keySerializer;
	}

	public void setKeySerializer(RedisSerializer keySerializer) {
		this.keySerializer = keySerializer;
	}

	public RedisSerializer getValueSerializer() {
		return valueSerializer;
	}

	public void setValueSerializer(RedisSerializer valueSerializer) {
		this.valueSerializer = valueSerializer;
	}

	public int getExpire() {
		return expire;
	}

	public void setExpire(int expire) {
		this.expire = expire;
	}
}
