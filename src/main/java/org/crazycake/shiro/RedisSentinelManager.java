package org.crazycake.shiro;

import redis.clients.jedis.*;

import java.util.HashSet;
import java.util.Set;

/**
 * support jedis sentinel
 * @create 2018-02-26 11:16
 **/

public class RedisSentinelManager extends RedisSingletonManager {

    private String masterName = "mymaster";

    private volatile JedisSentinelPool jedisSentinelPool = null;

    private void init() {
        synchronized (this) {
            if (jedisSentinelPool == null) {
                jedisSentinelPool = new JedisSentinelPool(masterName,getJedisSentinelSet(),new JedisPoolConfig(),timeout,soTimeout,password,database);
            }
        }
    }

    private Set<String> getJedisSentinelSet(){
        String[] hostAndPortArr = host.split(",");
        Set<String> hostAndPorts = new HashSet<String>();
        for(String host : hostAndPortArr){
            hostAndPorts.add(host);
        }
        return hostAndPorts;
    }

    @Override
    protected Jedis getJedis() {
        if (jedisSentinelPool == null) {
            init();
        }
        return jedisSentinelPool.getResource();
    }

    public String getMasterName() {
        return masterName;
    }

    public void setMasterName(String masterName) {
        this.masterName = masterName;
    }

}
