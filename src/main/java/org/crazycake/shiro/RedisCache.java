package org.crazycake.shiro;

import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheException;
import org.apache.shiro.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class RedisCache<K, V> implements Cache<K, V> {
	
	private static Logger logger = LoggerFactory.getLogger(RedisCache.class);
	private static final String DEFAULT_CACHE_KEY_PREFIX = "shiro:cache:";

	private RedisSerializer keySerializer = new StringSerializer();
	private RedisSerializer valueSerializer = new ObjectSerializer();
	private RedisManager redisManager;
	private String keyPrefix = DEFAULT_CACHE_KEY_PREFIX;

	/**
	 * Construction
	 * @param redisManager
	 */
	public RedisCache(RedisManager redisManager, RedisSerializer keySerializer, RedisSerializer valueSerializer){
		 if (redisManager == null) {
	         throw new IllegalArgumentException("Cache argument cannot be null.");
	     }
	     this.redisManager = redisManager;
		 this.keySerializer = keySerializer;
		 this.valueSerializer = valueSerializer;
	}
	
	/**
	 * Constructs a cache instance with the specified
	 * Redis manager and using a custom key prefix.
	 * @param cache The cache manager instance
	 * @param prefix The Redis key prefix
	 */
	public RedisCache(RedisManager cache, RedisSerializer keySerializer, RedisSerializer valueSerializer,
				String prefix){
		this( cache, keySerializer, valueSerializer );
		this.keyPrefix = prefix;
	}
 	
	@Override
	public V get(K key) throws CacheException {
		logger.debug("get key [" + key + "]");

		if (key == null) {
			return null;
		}

		try {
			Object redisCacheKey = getRedisCacheKey(key);
			byte[] rawValue = redisManager.get(keySerializer.serialize(redisCacheKey));
			V value = (V) valueSerializer.deserialize(rawValue);
			return value;
		} catch (SerializationException e) {
			throw new CacheException(e);
		}
	}

	@Override
	public V put(K key, V value) throws CacheException {
		logger.debug("put key [" + key + "]");
		try {
			Object redisCacheKey = getRedisCacheKey(key);
			redisManager.set(keySerializer.serialize(redisCacheKey), valueSerializer.serialize(value), redisManager.getExpire());
			return value;
		} catch (SerializationException e) {
			throw new CacheException(e);
		}
	}

	@Override
	public V remove(K key) throws CacheException {
		logger.debug("remove key [" + key + "]");
		try {
            V previous = get(key);
			Object redisCacheKey = getRedisCacheKey(key);
            redisManager.del(keySerializer.serialize(redisCacheKey));
            return previous;
        } catch (SerializationException e) {
            throw new CacheException(e);
        }
	}

	private Object getRedisCacheKey(K key) {
		if (key == null) {
			return null;
		}
		Object redisKey = key;
		if (keySerializer instanceof StringSerializer) {
            redisKey = key.toString();
        }
        if (redisKey instanceof String) {
		    return this.keyPrefix + (String)redisKey;
        }
		return redisKey;
	}

	@Override
	public void clear() throws CacheException {
		logger.debug("clear cache");
        redisManager.flushDB();
	}

	@Override
	public int size() {
		Long longSize = new Long(redisManager.dbSize());
		return longSize.intValue();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Set<K> keys() {
		Set<byte[]> keys = null;
		try {
			keys = redisManager.keys(keySerializer.serialize(this.keyPrefix + "*"));
		} catch (SerializationException e) {
			logger.error("get keys error", e);
			return Collections.emptySet();
		}

		if (CollectionUtils.isEmpty(keys)) {
			return Collections.emptySet();
		}

		Set<K> convertedKeys = new HashSet<K>();
		for(byte[] key:keys){
			try {
				convertedKeys.add((K)keySerializer.deserialize(key));
			} catch (SerializationException e) {
				logger.error("deserialize keys error", e);
			}
		}
		return convertedKeys;
	}

	@Override
	public Collection<V> values() {
		Set<byte[]> keys = null;
		try {
			keys = redisManager.keys(keySerializer.serialize(this.keyPrefix + "*"));
		} catch (SerializationException e) {
			logger.error("get values error", e);
			return Collections.emptySet();
		}

		if(CollectionUtils.isEmpty(keys)) {
			return Collections.emptySet();
		}

		List<V> values = new ArrayList<V>(keys.size());
		for (byte[] key : keys) {
			V value = null;
			try {
				value = (V)valueSerializer.deserialize(redisManager.get(key));
			} catch (SerializationException e) {
				logger.error("deserialize values= error", e);
			}
			if (value != null) {
				values.add(value);
			}
		}
		return Collections.unmodifiableList(values);
	}

	public String getKeyPrefix() {
		return keyPrefix;
	}

	public void setKeyPrefix(String keyPrefix) {
		this.keyPrefix = keyPrefix;
	}
}
