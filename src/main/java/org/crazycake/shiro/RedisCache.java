package org.crazycake.shiro;

import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheException;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.CollectionUtils;
import org.crazycake.shiro.exception.CacheManagerPrincipalIdNotAssignedException;
import org.crazycake.shiro.exception.PrincipalIdNullException;
import org.crazycake.shiro.exception.PrincipalInstanceException;
import org.crazycake.shiro.exception.SerializationException;
import org.crazycake.shiro.serializer.RedisSerializer;
import org.crazycake.shiro.serializer.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Used for setting/getting authorization information from Redis
 * @param <K>
 * @param <V>
 */
public class RedisCache<K, V> implements Cache<K, V> {

	private static Logger logger = LoggerFactory.getLogger(RedisCache.class);

	private RedisSerializer keySerializer;
	private RedisSerializer valueSerializer;
	private IRedisManager redisManager;
	private String keyPrefix = RedisCacheManager.DEFAULT_CACHE_KEY_PREFIX;
	private int expire;
	private String principalIdFieldName = RedisCacheManager.DEFAULT_PRINCIPAL_ID_FIELD_NAME;

	/**
	 *
	 * @param redisManager redisManager
	 * @param keySerializer keySerializer
	 * @param valueSerializer valueSerializer
	 * @param prefix authorization prefix
	 * @param expire expire
	 * @param principalIdFieldName id field name of principal object
	 */
	public RedisCache(IRedisManager redisManager, RedisSerializer keySerializer, RedisSerializer valueSerializer, String prefix, int expire, String principalIdFieldName) {
		 if (redisManager == null) {
	         throw new IllegalArgumentException("redisManager cannot be null.");
	     }
	     this.redisManager = redisManager;
		 if (keySerializer == null) {
			 throw new IllegalArgumentException("keySerializer cannot be null.");
		 }
		 this.keySerializer = keySerializer;
		if (valueSerializer == null) {
			throw new IllegalArgumentException("valueSerializer cannot be null.");
		}
		 this.valueSerializer = valueSerializer;
		 if (prefix != null && !"".equals(prefix)) {
			 this.keyPrefix = prefix;
		 }
         this.expire = expire;
		 if (principalIdFieldName != null) {
			 this.principalIdFieldName = principalIdFieldName;
		 }
	}

	/**
	 * get shiro authorization redis key-value
	 * @param key key
	 * @return value
	 * @throws CacheException get cache exception
	 */
	@Override
	public V get(K key) throws CacheException {
		logger.debug("get key [" + key + "]");

		if (key == null) {
			return null;
		}

		try {
			Object redisCacheKey = getRedisCacheKey(key);
			byte[] rawValue = redisManager.get(keySerializer.serialize(redisCacheKey));
			if (rawValue == null) {
				return null;
			}
			V value = (V) valueSerializer.deserialize(rawValue);
			return value;
		} catch (SerializationException e) {
			throw new CacheException(e);
		}
	}

	@Override
	public V put(K key, V value) throws CacheException {
		if (key == null) {
			logger.warn("Saving a null key is meaningless, return value directly without call Redis.");
			return value;
		}
		try {
			Object redisCacheKey = getRedisCacheKey(key);
			logger.debug("put key [" + redisCacheKey + "]");
			redisManager.set(keySerializer.serialize(redisCacheKey), value != null ? valueSerializer.serialize(value) : null, expire);
			return value;
		} catch (SerializationException e) {
			throw new CacheException(e);
		}
	}

	@Override
	public V remove(K key) throws CacheException {
		logger.debug("remove key [" + key + "]");
        if (key == null) {
            return null;
        }
		try {
            Object redisCacheKey = getRedisCacheKey(key);
            byte[] rawValue = redisManager.get(keySerializer.serialize(redisCacheKey));
            V previous = (V) valueSerializer.deserialize(rawValue);
            redisManager.del(keySerializer.serialize(redisCacheKey));
            return previous;
        } catch (SerializationException e) {
            throw new CacheException(e);
        }
	}

	/**
	 * get the full Redis key including prefix by Redis key
	 * @param key
	 * @return
	 */
	private Object getRedisCacheKey(K key) {
		if (key == null) {
			return null;
		}
		if (keySerializer instanceof StringSerializer) {
			return this.keyPrefix + getStringRedisKey(key);
		}
		return key;
	}

	/**
	 * get Redis key (not including prefix)
	 * @param key
	 * @return
	 */
	private String getStringRedisKey(K key) {
		String redisKey;
		if (key instanceof PrincipalCollection) {
			redisKey = getRedisKeyFromPrincipalIdField((PrincipalCollection) key);
        } else {
			redisKey = key.toString();
		}
		return redisKey;
	}

	/**
	 * get the Redis key (not including prefix) by PrincipalCollection
	 * @param key
	 * @return
	 */
	private String getRedisKeyFromPrincipalIdField(PrincipalCollection key) {
		Object principalObject = key.getPrimaryPrincipal();
		if (principalObject instanceof String) {
		    return principalObject.toString();
		}
		Method pincipalIdGetter = getPrincipalIdGetter(principalObject);
		return getIdObj(principalObject, pincipalIdGetter);
	}

	private String getIdObj(Object principalObject, Method pincipalIdGetter) {
		String redisKey;
		try {
		    Object idObj = pincipalIdGetter.invoke(principalObject);
		    if (idObj == null) {
		        throw new PrincipalIdNullException(principalObject.getClass(), this.principalIdFieldName);
            }
			redisKey = idObj.toString();
		} catch (IllegalAccessException e) {
			throw new PrincipalInstanceException(principalObject.getClass(), this.principalIdFieldName, e);
		} catch (InvocationTargetException e) {
			throw new PrincipalInstanceException(principalObject.getClass(), this.principalIdFieldName, e);
		}
		return redisKey;
	}

	private Method getPrincipalIdGetter(Object principalObject) {
		Method pincipalIdGetter = null;
		String principalIdMethodName = this.getPrincipalIdMethodName();
		try {
			pincipalIdGetter = principalObject.getClass().getMethod(principalIdMethodName);
		} catch (NoSuchMethodException e) {
			throw new PrincipalInstanceException(principalObject.getClass(), this.principalIdFieldName);
		}
		return pincipalIdGetter;
	}

	private String getPrincipalIdMethodName() {
		if (this.principalIdFieldName == null || "".equals(this.principalIdFieldName)) {
			throw new CacheManagerPrincipalIdNotAssignedException();
		}
		return "get" + this.principalIdFieldName.substring(0, 1).toUpperCase() + this.principalIdFieldName.substring(1);
	}


	@Override
	public void clear() throws CacheException {
		logger.debug("clear cache");
        Set<byte[]> keys = null;
        try {
            keys = redisManager.keys(keySerializer.serialize(this.keyPrefix + "*"));
        } catch (SerializationException e) {
            logger.error("get keys error", e);
        }
        if (keys == null || keys.size() == 0) {
            return;
        }
        for (byte[] key: keys) {
            redisManager.del(key);
        }
	}

	/**
	 * get all authorization key-value quantity
	 * @return key-value size
	 */
	@Override
	public int size() {
		Long longSize = 0L;
		try {
			longSize = new Long(redisManager.dbSize(keySerializer.serialize(this.keyPrefix + "*")));
		} catch (SerializationException e) {
			logger.error("get keys error", e);
		}
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
		for (byte[] key:keys) {
			try {
				convertedKeys.add((K) keySerializer.deserialize(key));
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

		if (CollectionUtils.isEmpty(keys)) {
			return Collections.emptySet();
		}

		List<V> values = new ArrayList<V>(keys.size());
		for (byte[] key : keys) {
			V value = null;
			try {
				value = (V) valueSerializer.deserialize(redisManager.get(key));
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

	public String getPrincipalIdFieldName() {
		return principalIdFieldName;
	}

	public void setPrincipalIdFieldName(String principalIdFieldName) {
		this.principalIdFieldName = principalIdFieldName;
	}
}
