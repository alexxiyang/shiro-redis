package org.crazycake.shiro;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.Protocol;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class RedisSentinelManager extends BaseRedisManager implements IRedisManager{

	private static final String DEFAULT_HOST = "127.0.0.1:26379,127.0.0.1:26380,127.0.0.1:26381";
	private String host = DEFAULT_HOST;

	private static final String DEFAULT_MASTER_NAME = "mymaster";
	private String masterName = DEFAULT_MASTER_NAME;

	// timeout for jedis try to connect to redis server, not expire time! In milliseconds
	private int timeout = Protocol.DEFAULT_TIMEOUT;

	private String password;

	private int database = Protocol.DEFAULT_DATABASE;

	private GenericObjectPoolConfig genericObjectPoolConfig = new GenericObjectPoolConfig();

	private void init() {
		synchronized (this) {
			if (jedisPool == null) {
				String[] sentinelHosts = host.split(",\\s+");
				Set<String> sentinels = new HashSet<String>();
				Collections.addAll(sentinels, sentinelHosts);
				jedisPool = new JedisSentinelPool(masterName, sentinels, genericObjectPoolConfig, timeout, password, database);
			}
		}
	}

	@Override
	protected void checkAndInit() {
		if (jedisPool == null) {
			init();
		}
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}



	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public int getDatabase() {
		return database;
	}

	public void setDatabase(int database) {
		this.database = database;
	}

	public String getMasterName() {
		return masterName;
	}

	public void setMasterName(String masterName) {
		this.masterName = masterName;
	}

	public GenericObjectPoolConfig getGenericObjectPoolConfig() {
		return genericObjectPoolConfig;
	}

	public void setGenericObjectPoolConfig(GenericObjectPoolConfig genericObjectPoolConfig) {
		this.genericObjectPoolConfig = genericObjectPoolConfig;
	}
}
