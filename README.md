shiro-redis
===========

shiro only provide the support of ehcache and concurrentHashMap. Here is an implement of redis cache can be used by shiro. Hope it will help you!

How to use it?
===========

You can chose these 2 ways to include shiro-redis into your project
+ directly download jar file
Download shiro-redis.jar in bin folder and add it into your classpath.
+ add maven dependency
------------------------------------
    <dependency>
  		<groupId>org.crazycake</groupId>
  		<artifactId>shiro-redis</artifactId>
  		<version>2.4.2-RELEASE</version>
  	</dependency>
------------------------------------
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
