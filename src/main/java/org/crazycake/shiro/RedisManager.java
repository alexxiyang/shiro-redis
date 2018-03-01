package org.crazycake.shiro;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

public class RedisManager extends BaseRedisManager implements IRedisManager{
	
	private String host = "127.0.0.1";

	private int port = Protocol.DEFAULT_PORT ;
	
	// timeout for jedis try to connect to redis server, not expire time! In milliseconds
	private int timeout = Protocol.DEFAULT_TIMEOUT;
	
	private String password;

	private int database = Protocol.DEFAULT_DATABASE;

	private JedisPool jedisPool;

	private void init() {
		synchronized (this) {
			if (jedisPool == null) {
				if(host.contains(":")){
					// support host:port config style
					String[] hostAndPort = host.split(":");
					jedisPool = new JedisPool(jedisPoolConfig, hostAndPort[0], Integer.parseInt(hostAndPort[1]), timeout, password, database);
				}else{
					jedisPool = new JedisPool(jedisPoolConfig, host, port, timeout, password, database);
				}
			}
		}
	}

	@Override
	protected Jedis getJedis() {
		if (jedisPool == null) {
			init();
		}
		return jedisPool.getResource();
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

	public JedisPool getJedisPool() {
		return jedisPool;
	}

	public void setJedisPool(JedisPool jedisPool) {
		this.jedisPool = jedisPool;
	}
}
