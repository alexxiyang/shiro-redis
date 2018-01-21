package org.crazycake.shiro;

import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.eis.AbstractSessionDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class RedisSessionDAO extends AbstractSessionDAO {

	private static Logger logger = LoggerFactory.getLogger(RedisSessionDAO.class);
	private static final String DEFAULT_SESSION_KEY_PREFIX = "shiro:session:";

	private RedisManager redisManager;
	private String keyPrefix = DEFAULT_SESSION_KEY_PREFIX;
	private RedisSerializer keySerializer = new StringSerializer();
	private RedisSerializer valueSerializer = new ObjectSerializer();
	
	@Override
	public void update(Session session) throws UnknownSessionException {
		this.saveSession(session);
	}
	
	/**
	 * save session
	 * @param session
	 * @throws UnknownSessionException
	 */
	private void saveSession(Session session) throws UnknownSessionException{
		if(session == null || session.getId() == null){
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

		session.setTimeout(redisManager.getExpire()*1000);		
		this.redisManager.set(key, value, redisManager.getExpire());
	}

	@Override
	public void delete(Session session) {
		if(session == null || session.getId() == null){
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
			if(keys != null && keys.size()>0){
				for(byte[] key:keys){
					Session s = (Session)valueSerializer.deserialize(redisManager.get(key));
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
		if(session == null){
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
		if(sessionId == null){
			logger.warn("session id is null");
			return null;
		}
		Session s = null;
		try {
			s = (Session)valueSerializer.deserialize(redisManager.get(keySerializer.serialize(getRedisSessionKey(sessionId))));
		} catch (SerializationException e) {
			logger.error("read session error. settionId=" + sessionId);
		}
		return s;
	}

	private String getRedisSessionKey(Serializable sessionId) {
		return this.keyPrefix + sessionId;
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
