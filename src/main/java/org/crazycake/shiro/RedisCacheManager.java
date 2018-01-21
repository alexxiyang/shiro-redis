package org.crazycake.shiro;

import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheException;
import org.apache.shiro.cache.CacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RedisCacheManager implements CacheManager {

	private static final Logger logger = LoggerFactory.getLogger(RedisCacheManager.class);

	// fast lookup by name map
	private final ConcurrentMap<String, Cache> caches = new ConcurrentHashMap<String, Cache>();
	private RedisSerializer keySerializer = new StringSerializer();
	private RedisSerializer valueSerializer = new ObjectSerializer();

	private RedisManager redisManager;

	/**
	 * The Redis key prefix for caches 
	 */
	private String keyPrefix = "shiro:cache:";

	@Override
	public <K, V> Cache<K, V> getCache(String name) throws CacheException {
		logger.debug("get cache, name=" + name);
		
		Cache cache = caches.get(name);
		
		if (cache == null) {
			cache = new RedisCache<K, V>(redisManager, keySerializer, valueSerializer, keyPrefix);
			caches.put(name, cache);
		}
		return cache;
	}

	public RedisManager getRedisManager() {
		return redisManager;
	}

	public void setRedisManager(RedisManager redisManager) {
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
}
