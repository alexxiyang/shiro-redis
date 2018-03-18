package org.crazycake.shiro;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.Protocol;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class RedisSentinelManager extends BaseRedisManager implements IRedisManager {

	private static final String DEFAULT_HOST = "127.0.0.1:26379,127.0.0.1:26380,127.0.0.1:26381";
	private String host = DEFAULT_HOST;

	private static final String DEFAULT_MASTER_NAME = "mymaster";
	private String masterName = DEFAULT_MASTER_NAME;

	// timeout for jedis try to connect to redis server, not expire time! In milliseconds
	private int timeout = Protocol.DEFAULT_TIMEOUT;

	// timeout for jedis try to read data from redis server
	private int soTimeout = Protocol.DEFAULT_TIMEOUT;

	private String password;

	private int database = Protocol.DEFAULT_DATABASE;

	private JedisSentinelPool jedisPool;

	@Override
	protected Jedis getJedis() {
		if (jedisPool == null) {
			init();
		}
		return jedisPool.getResource();
	}

	private void init() {
		synchronized (this) {
			if (jedisPool == null) {
				String[] sentinelHosts = host.split(",\\s*");
				Set<String> sentinels = new HashSet<String>();
				Collections.addAll(sentinels, sentinelHosts);
				jedisPool = new JedisSentinelPool(masterName, sentinels, getJedisPoolConfig(), timeout, soTimeout, password, database);
			}
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

	public int getSoTimeout() {
		return soTimeout;
	}

	public void setSoTimeout(int soTimeout) {
		this.soTimeout = soTimeout;
	}

}
