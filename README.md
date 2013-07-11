shiro-redis
===========

shiro only provide the support of ehcache and concurrentHashMap. Here is an implement of redis cache can be used by shiro. Hope it will help you!

How to use it?
===========

Add sonatype release repository into your setting.xml

```xml
<repository>
	<id>sonatype.maven.release</id>
	<name>Sonatype Release Repository</name>
	<url>https://oss.sonatype.org/content/repositories/releases</url>
</repository>
```

After updated indexes you can add dependency of shiro-redis into your pom.xml

```xml
<dependency>
	<groupId>org.crazycake</groupId>
	<artifactId>shiro-redis</artifactId>
	<version>2.0.0-RELEASE</version>
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

#============redisSessionDAO=============
redisSessionDAO = org.crazycake.shiro.RedisSessionDAO
redisSessionDAO.redisManager = $redisManager
sessionManager = org.apache.shiro.web.session.mgt.DefaultWebSessionManager
sessionManager.sessionDAO = $redisSessionDAO
securityManager.sessionManager = $sessionManager

#============redisCacheManager===========
cacheManager = org.crazycake.shiro.RedisCacheManager
cacheManager.redisManager = $redisManager
securityManager.cacheManager = $cacheManager
```


If you found any bugs
===========

Please send email to idante@qq.com
