package org.yqr.shiro;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheException;
import org.apache.shiro.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

public class RedisCache<K, V> implements Cache<K, V> {
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	/**
     * The wrapped Jedis instance.
     */
	private Jedis cache;
	
	/**
	 * 所有对象的超时设置
	 */
	private int expire;
	
	
	/**
	 * 使用一个jedis实例构造RedisCache的方法
	 * @param cache
	 */
	public RedisCache(Jedis cache){
		if (cache == null) {
	         throw new IllegalArgumentException("Cache argument cannot be null.");
	     }
	     this.cache = cache;
	}
	
	
	/**
	 * 通过一个Jedis实例构造RedisCache
	 */
	public RedisCache(Jedis cache,int expire){
		 if (cache == null) {
	         throw new IllegalArgumentException("Cache argument cannot be null.");
	     }
	     this.cache = cache;
	     this.expire = expire;
	}
	
	/**
	 * 获得byte[]型的key
	 * @param key
	 * @return
	 */
	private byte[] getByteKey(K key){
		if(key instanceof String){
    		return ((String) key).getBytes();
    	}else{
    		return serialize(key);
    	}
	}
 	
	@Override
	public V get(K key) throws CacheException {
		logger.debug("根据key从Redis中获取对象 key [" + key + "]");
		try {
			if (key == null) {
	            return null;
	        }else{
	        	byte[] rawValue = cache.get(getByteKey(key));
	        	@SuppressWarnings("unchecked")
				V value = (V)deserialize(rawValue);
	        	return value;
	        }
		} catch (Throwable t) {
			throw new CacheException(t);
		}

	}

	@Override
	public V put(K key, V value) throws CacheException {
		logger.debug("根据key从存储 key [" + key + "]");
		 try {
			 	cache.set(getByteKey(key), serialize(value));
			 	if(expire != 0){
			 		cache.expire(getByteKey(key), expire);
			 	}
	            return value;
	        } catch (Throwable t) {
	            throw new CacheException(t);
	        }
	}

	@Override
	public V remove(K key) throws CacheException {
		logger.debug("从redis中删除 key [" + key + "]");
		try {
            V previous = get(key);
            cache.del(getByteKey(key));
            return previous;
        } catch (Throwable t) {
            throw new CacheException(t);
        }
	}

	@Override
	public void clear() throws CacheException {
		logger.debug("从redis中删除所有元素");
		try {
            cache.flushDB();
        } catch (Throwable t) {
            throw new CacheException(t);
        }
	}

	@Override
	public int size() {
		try {
			Long longSize = new Long(cache.dbSize());
            return longSize.intValue();
        } catch (Throwable t) {
            throw new CacheException(t);
        }
	}

	@SuppressWarnings("unchecked")
	@Override
	public Set<K> keys() {
		try {
            Set<String> keys = cache.keys("*");
            if (CollectionUtils.isEmpty(keys)) {
            	return Collections.emptySet();
            }else{
            	Set<K> newKeys = new HashSet<K>();
            	for(String key:keys){
            		newKeys.add((K)key);
            	}
            	return newKeys;
            }
        } catch (Throwable t) {
            throw new CacheException(t);
        }
	}

	@Override
	public Collection<V> values() {
		try {
            Set<String> keys = cache.keys("*");
            if (!CollectionUtils.isEmpty(keys)) {
                List<V> values = new ArrayList<V>(keys.size());
                for (String key : keys) {
                    @SuppressWarnings("unchecked")
					V value = get((K)key);
                    if (value != null) {
                        values.add(value);
                    }
                }
                return Collections.unmodifiableList(values);
            } else {
                return Collections.emptyList();
            }
        } catch (Throwable t) {
            throw new CacheException(t);
        }
	}
	
	/**
	 * 反序列化
	 * @param bytes
	 * @return
	 */
	public Object deserialize(byte[] bytes) {
		
		Object result = null;
		
		if (isEmpty(bytes)) {
			return null;
		}

		try {
			ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
			try {
				ObjectInputStream objectInputStream = new ObjectInputStream(byteStream);
				try {
					result = objectInputStream.readObject();
				}
				catch (ClassNotFoundException ex) {
					throw new Exception("Failed to deserialize object type", ex);
				}
			}
			catch (Throwable ex) {
				throw new Exception("Failed to deserialize", ex);
			}
		} catch (Exception e) {
			logger.error("Failed to deserialize",e);
		}
		return result;
	}
	
	private boolean isEmpty(byte[] data) {
		return (data == null || data.length == 0);
	}

	/**
	 * 序列化
	 * @param object
	 * @return
	 */
	public byte[] serialize(Object object) {
		
		byte[] result = null;
		
		if (object == null) {
			return new byte[0];
		}
		try {
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream(128);
			try  {
				if (!(object instanceof Serializable)) {
					throw new IllegalArgumentException(getClass().getSimpleName() + " requires a Serializable payload " +
							"but received an object of type [" + object.getClass().getName() + "]");
				}
				ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteStream);
				objectOutputStream.writeObject(object);
				objectOutputStream.flush();
				result =  byteStream.toByteArray();
			}
			catch (Throwable ex) {
				throw new Exception("Failed to serialize", ex);
			}
		} catch (Exception ex) {
			logger.error("Failed to serialize",ex);
		}
		return result;
	}

}
