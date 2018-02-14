shiro-redis
=============

[![Build Status](https://travis-ci.org/alexxiyang/shiro-redis.svg?branch=master)](https://travis-ci.org/alexxiyang/shiro-redis)


shiro only provide the support of ehcache and concurrentHashMap. Here is an implement of redis cache can be used by shiro. Hope it will help you!

# Download

You can choose these 2 ways to include shiro-redis into your project
* use "git clone https://github.com/alexxiyang/shiro-redis.git" to clone project to your local workspace and build jar file by your self
* add maven dependency 

```xml
<dependency>
    <groupId>org.crazycake</groupId>
    <artifactId>shiro-redis</artifactId>
    <version>2.4.14</version>
</dependency>
```

# How to configure ?

You can configure shiro-redis either in shiro.ini or in spring-*.xml

## ini
Here is the configuration for shiro.ini.

shiro.ini:

```properties
[main]
#====================================
# shiro-redis configuration [start]
#====================================

#===================================
# Redis Manager
#===================================
# Create redisManager
redisManager = org.crazycake.shiro.RedisManager
# Redis host. If you don't specify host the default value is 127.0.0.1 (Optional)
redisManager.host = 192.168.56.101
# Redis port. Default value: 6379 (Optional)
redisManager.port = 6379
# Redis cache key/value expire time. Default value: 3600 .The expire time is in second (Optional)
redisManager.expire = 600
# Redis connect timeout. Timeout for jedis try to connect to redis server(In milliseconds).(Optional)
redisManager.timeout = 0
# Redis password.(Optional)
#redisManager.password =
# Redis database. Default value is 0(Optional)
#redisManager.database = 0

#====================================
# Redis-based session configuration
#====================================
# Create redisSessionDAO
redisSessionDAO = org.crazycake.shiro.RedisSessionDAO
# Custom your redis key prefix for session management, if you doesn't define this parameter, shiro-redis will use 'shiro_redis_session:' as default prefix
# Note: Remember to add colon at the end of prefix.
redisSessionDAO.keyPrefix = shiro:session:
# Use redisManager as cache manager
redisSessionDAO.redisManager = $redisManager
sessionManager = org.apache.shiro.web.session.mgt.DefaultWebSessionManager
sessionManager.sessionDAO = $redisSessionDAO
securityManager.sessionManager = $sessionManager

#=====================================
# Redis-based cache configuration
#=====================================
# Create cacheManager
cacheManager = org.crazycake.shiro.RedisCacheManager
# Custom your redis key prefix for cache management, if you doesn't define this parameter, shiro-redis will use 'shiro_redis_session:' as default prefix
# Note: Remember to add colon at the end of prefix.
cacheManager.keyPrefix = shiro:cache:
# Use redisManager as cache manager
cacheManager.redisManager = $redisManager
securityManager.cacheManager = $cacheManager

#=================================
# shiro-redis configuration [end]
#=================================
```
Here is a [tutorial project](https://github.com/alexxiyang/shiro-redis-tutorial) for you to understand how to configure `shiro-redis` in `shiro.ini`.

## Spring

spring.xml:
```xml
<!-- shiro-redis configuration [start] -->
<!-- shiro redisManager -->
<bean id="redisManager" class="org.crazycake.shiro.RedisManager">
    <property name="host" value="127.0.0.1"/>
    <property name="port" value="6379"/>
    <property name="expire" value="1800"/>
    <!-- optional properties:
    <property name="timeout" value="10000"/>
    <property name="password" value="123456"/>
    <property name="database" value="1"/>
    -->
</bean>

<!-- Redis-based session configuration -->
<bean id="redisSessionDAO" class="org.crazycake.shiro.RedisSessionDAO">
    <property name="redisManager" ref="redisManager" />
    <property name="keyPrefix" value="shiro:session:" />
</bean>
<bean id="sessionManager" class="org.apache.shiro.web.session.mgt.DefaultWebSessionManager">
    <property name="sessionDAO" ref="redisSessionDAO" />
</bean>

<!-- Redis-based cache configuration -->
<bean id="cacheManager" class="org.crazycake.shiro.RedisCacheManager">
    <property name="redisManager" ref="redisManager" />
    <property name="keyPrefix" value="shiro:cache:" />
</bean>

<!-- securityManager -->
<bean id="securityManager" class="org.apache.shiro.web.mgt.DefaultWebSecurityManager">
    <property name="sessionManager" ref="sessionManager" />
    <property name="cacheManager" ref="cacheManager" />

    <!-- other configurations -->
    <property name="realm" ref="exampleRealm"/>
    <property name="rememberMeManager.cipherKey" value="kPH+bIxk5D2deZiIxcaaaA==" />
</bean>
<!-- shiro-redis configuration [end] -->
```
Here is a [tutorial project](https://github.com/alexxiyang/shiro-redis-spring-tutorial) for you to understand how to configure `shiro-redis` in spring configuration file.

## Serializer
Since redis only accept `byte[]`, there comes to a serializer problem.
Shiro-redis is using StringSerializer as key serializer and ObjectSerializer as value serializer.
You can use your own custom serializer, as long as this custom serializer implemens `org.crazycake.shiro.RedisSerializer`

For example, you need to change the charset of keySerializer.
```properties
#=====================================
# Redis-based cache configuration
#=====================================
# Create cacheManager
cacheManager = org.crazycake.shiro.RedisCacheManager
# If you want change charset of keySerializer or use your own custom serializer, you need to define serializer first
cacheManagerKeySerializer = org.crazycake.shiro.StringSerializer
# Refer to https://docs.oracle.com/javase/8/docs/technotes/guides/intl/encoding.doc.html
# UTF-8, UTF-16, UTF-32, ISO-8859-1, GBK, Big5, etc
cacheManagerKeySerializer.charset = UTF-16
cacheManager.keySerializer = $cacheManagerKeySerializer
```

These 4 Serializers are replaceable:
- cacheManager.keySerializer
- cacheManager.valueSerializer
- redisSessionDAO.keySerializer
- redisSessionDAO.valueSerializer


# If you found any bugs

Please send email to alexxiyang@gmail.com

可以用中文
