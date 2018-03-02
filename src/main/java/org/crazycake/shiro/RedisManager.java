package org.crazycake.shiro;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

public class RedisManager extends BaseRedisManager implements IRedisManager{

	private static final String DEFAULT_HOST = "127.0.0.1:6379";
	private String host = DEFAULT_HOST;

	@Deprecated
	private int port = Protocol.DEFAULT_PORT ;
	
	// timeout for jedis try to connect to redis server, not expire time! In milliseconds
	private int timeout = Protocol.DEFAULT_TIMEOUT;
	
	private String password;

	private int database = Protocol.DEFAULT_DATABASE;

	private JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();

	private void init() {
		synchronized (this) {
			if (jedisPool == null) {
				jedisPool = new JedisPool(jedisPoolConfig, host, port, timeout, password, database);
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

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
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

	public JedisPoolConfig getJedisPoolConfig() {
		return jedisPoolConfig;
	}

	public void setJedisPoolConfig(JedisPoolConfig jedisPoolConfig) {
		this.jedisPoolConfig = jedisPoolConfig;
	}
}
