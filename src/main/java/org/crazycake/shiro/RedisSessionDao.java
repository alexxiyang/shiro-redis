package org.crazycake.shiro;

import java.io.Serializable;
import java.util.Collection;

import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.eis.AbstractSessionDAO;

public class RedisSessionDao extends AbstractSessionDAO {

	/**
	 * shiro-redis的session对象前缀
	 */
	private final String SHIRO_REDIS_SESSION_PRE = "shiro_redis_session:";
	
	@Override
	public void update(Session session) throws UnknownSessionException {
		// TODO Auto-generated method stub

	}

	@Override
	public void delete(Session session) {
		// TODO Auto-generated method stub

	}

	@Override
	public Collection<Session> getActiveSessions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Serializable doCreate(Session session) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Session doReadSession(Serializable sessionId) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * 返回session在redis中要存储的key
	 * @param sessionId
	 * @return
	 */
	private String genSessionRedisKey(String sessionId){
		return SHIRO_REDIS_SESSION_PRE+sessionId;
	}
}
