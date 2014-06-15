# shiro-redis

[![Build Status](https://travis-ci.org/alexxiyang/shiro-redis.svg?branch=master)](https://travis-ci.org/alexxiyang/shiro-redis)


shiro only provide the support of ehcache and concurrentHashMap. Here is an implement of redis cache can be used by shiro. Hope it will help you!

How to use it?
===========

You can choose these 2 ways to include shiro-redis into your project
* use "git clone https://github.com/alexxiyang/shiro-redis.git" to clone project to your local workspace and build jar file by your self
* add maven dependency 

```xml
    <dependency>
  		<groupId>org.crazycake</groupId>
  		<artifactId>shiro-redis</artifactId>
  		<version>2.4.2.1-RELEASE</version>
  	</dependency>
```

Edit shiro.ini

```properties
#redisManager
redisManager = org.crazycake.shiro.RedisManager
#optional if you don't specify host the default value is 127.0.0.1
redisManager.host = 127.0.0.1
#optional , default value: 6379
redisManager.port = 6379
#optional, default value:0 .The expire time is in second
redisManager.expire = 30
#optional, timeout for jedis try to connect to redis server(In milliseconds), not equals to expire time! 
redisManager.timeout = 0
#optional, password for redis server
redisManager.password = 

#============redisSessionDAO=============
redisSessionDAO = org.crazycake.shiro.RedisSessionDAO
redisSessionDAO.redisManager = $redisManager
sessionManager = org.apache.shiro.web.session.mgt.DefaultWebSessionManager
sessionManager.sessionDAO = $redisSessionDAO
securityManager.sessionManager = $sessionManager

#============redisCacheManager===========
cacheManager = org.crazycake.shiro.RedisCacheManager
cacheManager.redisManager = $redisManager
#custom your redis key prefix, if you doesn't define this parameter shiro-redis will use 'shiro_redis_session:' as default prefix
shiroCacheManager.keyPrefix = users:security:authz:
securityManager.cacheManager = $cacheManager
```


If you found any bugs
===========

Please send email to idante@qq.com
