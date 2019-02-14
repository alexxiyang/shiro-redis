package org.crazycake.shiro;

import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.eis.AbstractSessionDAO;
import org.crazycake.shiro.exception.SerializationException;
import org.crazycake.shiro.serializer.ObjectSerializer;
import org.crazycake.shiro.serializer.RedisSerializer;
import org.crazycake.shiro.serializer.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;

public class RedisSessionDAO extends AbstractSessionDAO {

	private static Logger logger = LoggerFactory.getLogger(RedisSessionDAO.class);

	private static final String DEFAULT_SESSION_KEY_PREFIX = "shiro:session:";
	private String keyPrefix = DEFAULT_SESSION_KEY_PREFIX;

	private static final long DEFAULT_SESSION_IN_MEMORY_TIMEOUT = 1000L;
	/**
	 * doReadSession be called about 10 times when login.
	 * Save Session in ThreadLocal to resolve this problem. sessionInMemoryTimeout is expiration of Session in ThreadLocal.
	 * The default value is 1000 milliseconds (1s).
	 * Most of time, you don't need to change it.
	 */
	private long sessionInMemoryTimeout = DEFAULT_SESSION_IN_MEMORY_TIMEOUT;

	private static final boolean DEFAULT_SESSION_IN_MEMORY_ENABLED = true;

	private boolean sessionInMemoryEnabled = DEFAULT_SESSION_IN_MEMORY_ENABLED;

	// expire time in seconds
	private static final int DEFAULT_EXPIRE = -2;
	private static final int NO_EXPIRE = -1;

	/**
	 * Please make sure expire is longer than sesion.getTimeout()
	 */
	private int expire = DEFAULT_EXPIRE;

	private static final int MILLISECONDS_IN_A_SECOND = 1000;

	private IRedisManager redisManager;
	private RedisSerializer keySerializer = new StringSerializer();
	private RedisSerializer valueSerializer = new ObjectSerializer();
	private static ThreadLocal sessionsInThread = new ThreadLocal();
	
	@Override
	public void update(Session session) throws UnknownSessionException {
		this.saveSession(session);
		if (this.sessionInMemoryEnabled) {
			this.setSessionToThreadLocal(session.getId(), session);
		}
	}
	
	/**
	 * save session
	 * @param session
	 * @throws UnknownSessionException
	 */
	private void saveSession(Session session) throws UnknownSessionException {
		if (session == null || session.getId() == null) {
			logger.error("session or session id is null");
			throw new UnknownSessionException("session or session id is null");
		}
		byte[] key;
		byte[] value;
		try {
			key = keySerializer.serialize(getRedisSessionKey(session.getId()));
			value = valueSerializer.serialize(session);
		} catch (SerializationException e) {
			logger.error("serialize session error. session id=" + session.getId());
			throw new UnknownSessionException(e);
		}
		if (expire == DEFAULT_EXPIRE) {
			this.redisManager.set(key, value, (int) (session.getTimeout() / MILLISECONDS_IN_A_SECOND));
			return;
		}
		if (expire != NO_EXPIRE && expire * MILLISECONDS_IN_A_SECOND < session.getTimeout()) {
			logger.warn("Redis session expire time: "
					+ (expire * MILLISECONDS_IN_A_SECOND)
					+ " is less than Session timeout: "
					+ session.getTimeout()
					+ " . It may cause some problems.");
		}
		this.redisManager.set(key, value, expire);
	}

	@Override
	public void delete(Session session) {
		if (session == null || session.getId() == null) {
			logger.error("session or session id is null");
			return;
		}
		try {
			redisManager.del(keySerializer.serialize(getRedisSessionKey(session.getId())));
		} catch (SerializationException e) {
			logger.error("delete session error. session id=" + session.getId());
		}
	}

	@Override
	public Collection<Session> getActiveSessions() {
		Set<Session> sessions = new HashSet<Session>();
		try {
			Set<byte[]> keys = redisManager.keys(this.keySerializer.serialize(this.keyPrefix + "*"));
			if (keys != null && keys.size() > 0) {
				for (byte[] key:keys) {
					Session s = (Session) valueSerializer.deserialize(redisManager.get(key));
					sessions.add(s);
				}
			}
		} catch (SerializationException e) {
			logger.error("get active sessions error.");
		}
		return sessions;
	}

	@Override
	protected Serializable doCreate(Session session) {
		if (session == null) {
			logger.error("session is null");
			throw new UnknownSessionException("session is null");
		}
		Serializable sessionId = this.generateSessionId(session);  
        this.assignSessionId(session, sessionId);
        this.saveSession(session);
		return sessionId;
	}

	@Override
	protected Session doReadSession(Serializable sessionId) {
		if (sessionId == null) {
			logger.warn("session id is null");
			return null;
		}

		if (this.sessionInMemoryEnabled) {
			Session session = getSessionFromThreadLocal(sessionId);
			if (session != null) {
				return session;
			}
		}

		Session session = null;
		logger.debug("read session from redis");
		try {
			session = (Session) valueSerializer.deserialize(redisManager.get(keySerializer.serialize(getRedisSessionKey(sessionId))));
			if (this.sessionInMemoryEnabled) {
				setSessionToThreadLocal(sessionId, session);
			}
		} catch (SerializationException e) {
			logger.error("read session error. settionId=" + sessionId);
		}
		return session;
	}

	private void setSessionToThreadLocal(Serializable sessionId, Session s) {
		Map<Serializable, SessionInMemory> sessionMap = (Map<Serializable, SessionInMemory>) sessionsInThread.get();
		if (sessionMap == null) {
            sessionMap = new HashMap<Serializable, SessionInMemory>();
            sessionsInThread.set(sessionMap);
        }

		removeExpiredSessionInMemory(sessionMap);

		SessionInMemory sessionInMemory = new SessionInMemory();
		sessionInMemory.setCreateTime(new Date());
		sessionInMemory.setSession(s);
		sessionMap.put(sessionId, sessionInMemory);
	}

	private void removeExpiredSessionInMemory(Map<Serializable, SessionInMemory> sessionMap) {
		Iterator<Serializable> it = sessionMap.keySet().iterator();
		while(it.hasNext()) {
			Serializable sessionId = it.next();
			SessionInMemory sessionInMemory = sessionMap.get(sessionId);
			if (sessionInMemory == null) {
				it.remove();
				continue;
			}
			long liveTime = getSessionInMemoryLiveTime(sessionInMemory);
			if (liveTime > sessionInMemoryTimeout) {
				it.remove();
			}
		}
	}

	private Session getSessionFromThreadLocal(Serializable sessionId) {

		if (sessionsInThread.get() == null) {
			return null;
		}

		Map<Serializable, SessionInMemory> sessionMap = (Map<Serializable, SessionInMemory>) sessionsInThread.get();
		SessionInMemory sessionInMemory = sessionMap.get(sessionId);
		if (sessionInMemory == null) {
			return null;
		}
		long liveTime = getSessionInMemoryLiveTime(sessionInMemory);
		if (liveTime > sessionInMemoryTimeout) {
			sessionMap.remove(sessionId);
			return null;
		}

		logger.debug("read session from memory");
		return sessionInMemory.getSession();
	}

	private long getSessionInMemoryLiveTime(SessionInMemory sessionInMemory) {
		Date now = new Date();
		return now.getTime() - sessionInMemory.getCreateTime().getTime();
	}

	private String getRedisSessionKey(Serializable sessionId) {
		return this.keyPrefix + sessionId;
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

	public long getSessionInMemoryTimeout() {
		return sessionInMemoryTimeout;
	}

	public void setSessionInMemoryTimeout(long sessionInMemoryTimeout) {
		this.sessionInMemoryTimeout = sessionInMemoryTimeout;
	}

	public int getExpire() {
		return expire;
	}

	public void setExpire(int expire) {
		this.expire = expire;
	}

	public boolean getSessionInMemoryEnabled() {
		return sessionInMemoryEnabled;
	}

	public void setSessionInMemoryEnabled(boolean sessionInMemoryEnabled) {
		this.sessionInMemoryEnabled = sessionInMemoryEnabled;
	}

	public static ThreadLocal getSessionsInThread() {
		return sessionsInThread;
	}
}
