package org.crazycake.shiro;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.eis.AbstractSessionDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.util.SafeEncoder;

public class RedisSessionDAO extends AbstractSessionDAO {

	private static Logger logger = LoggerFactory.getLogger(RedisSessionDAO.class);
	/**
	 * shiro-redis的session对象前缀
	 */
	private RedisManager redisManager;

	private LoadingCache<Serializable, Session> sessionCache = CacheBuilder.newBuilder().maximumSize(1024).expireAfterWrite(3, TimeUnit.MINUTES).build(
			new CacheLoader<Serializable, Session>() {
				public Session load(Serializable key) {
					long start = System.currentTimeMillis();
					Session s = (Session) SerializeUtils.deserialize(redisManager.get(getByteKey(key)));
					logger.info("get session from redis" + key.toString() + " token time is:" + (System.currentTimeMillis() - start));
					return s;
				}
			});


	/**
	 * The Redis key prefix for the sessions
	 */
	private String keyPrefix = "shiro_redis_session:";

	@Override
	protected Serializable doCreate(Session session) {
		long start = System.currentTimeMillis();
		Serializable sessionId = generateSessionId(session);
		this.assignSessionId(session, sessionId);

		this.saveSession(session);
		if (logger.isDebugEnabled()) {
			logger.debug("Create session :" + (System.currentTimeMillis() - start));
		}
		return sessionId;
	}


	@Override
	public void update(Session session) throws UnknownSessionException {
		long start = System.currentTimeMillis();
		this.saveSession(session);
		if (logger.isDebugEnabled()) {
			logger.debug("update session :" + (System.currentTimeMillis() - start));
		}
	}

	/**
	 * save session
	 *
	 * @param session
	 * @throws UnknownSessionException
	 */
	private void saveSession(Session session) throws UnknownSessionException {
		long start = System.currentTimeMillis();
		if (session == null || session.getId() == null) {
			logger.error("session or session id is null");
			return;
		}

		String key = getByteKey(session.getId());
		byte[] value = SerializeUtils.serialize(session);
		session.setTimeout(redisManager.getExpire() * 1000);
		this.redisManager.set(key, value, redisManager.getExpire());
		if (logger.isDebugEnabled()) {
			logger.debug("saveSession :" + (System.currentTimeMillis() - start));
		}
	}

	@Override
	public void delete(Session session) {
		long start = System.currentTimeMillis();
		if (session == null || session.getId() == null) {
			logger.error("session or session id is null");
			return;
		}
		redisManager.del(this.getByteKey(session.getId()));
		if (logger.isDebugEnabled()) {
			logger.debug("delete :" + (System.currentTimeMillis() - start));
		}
	}

	@Override
	public Collection<Session> getActiveSessions() {
		long start = System.currentTimeMillis();
		Set<Session> sessions = new HashSet<Session>();

		Set<byte[]> keys = redisManager.keys(this.keyPrefix + "*");
		if (keys != null && keys.size() > 0) {
			for (byte[] key : keys) {
				Session s = (Session) SerializeUtils.deserialize(redisManager.get(SafeEncoder.encode(key)));
				sessions.add(s);
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("getActiveSessions :" + (System.currentTimeMillis() - start));
		}
		return sessions;
	}


	@Override
	protected Session doReadSession(Serializable sessionId) {
		long start = System.currentTimeMillis();
		if (sessionId == null) {
			logger.error("session id is null");
			return null;
		}

		Session s = null; // (Session) SerializeUtils.deserialize(redisManager.get(this.getByteKey(sessionId)));
		try {
			s = sessionCache.get(sessionId);
			if (logger.isDebugEnabled()) {
				logger.debug("doReadSession session id:" + sessionId.toString() + " token time is:" + (System.currentTimeMillis() - start));
			}
		} catch (ExecutionException e) {
			logger.error(e.getMessage(), e);
			return s;
		}
		return s;
	}

	/**
	 * 获得byte[]型的key
	 *
	 * @param sessionId
	 * @return
	 */
	private String getByteKey(Serializable sessionId) {
		return this.keyPrefix + sessionId;
	}

	public RedisManager getRedisManager() {
		return redisManager;
	}

	public void setRedisManager(RedisManager redisManager) {
		this.redisManager = redisManager;

		/**
		 * 初始化redisManager
		 */
		this.redisManager.init();
	}

	/**
	 * Returns the Redis session keys
	 * prefix.
	 *
	 * @return The prefix
	 */
	public String getKeyPrefix() {
		return keyPrefix;
	}

	/**
	 * Sets the Redis sessions key
	 * prefix.
	 *
	 * @param keyPrefix The prefix
	 */
	public void setKeyPrefix(String keyPrefix) {
		this.keyPrefix = keyPrefix;
	}


}
