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
    <version>3.0.0</version>
</dependency>
```

# Before use
Please make sure your principal class implements `org.crazycake.shiro.AuthCachePrincipal`. Because shiro-redis needs a unique key to store authorization object. You will need to implement `getAuthCacheKey()` method to provide the unique key. Best practice is using userId or userName as authCachedKey.


For example:

If you create SimpleAuthenticationInfo like the following:
```java
@Override
protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
    UsernamePasswordToken usernamePasswordToken = (UsernamePasswordToken)token;
    UserInfo userInfo = new UserInfo();
    userInfo.setUsername(usernamePasswordToken.getUsername());
    return new SimpleAuthenticationInfo(userInfo, "123456", getName());
}
```

You need to make sure `UserInfo` implements `org.crazycake.shiro.AuthCachePrincipal`. Like this:
```java
public class UserInfo implements Serializable, AuthCachePrincipal{

    private String username;

    private Integer age;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    @Override
    public String getAuthCacheKey() {
        return getUsername();
    }
}
```


# How to configure ?

You can configure shiro-redis either in `shiro.ini` or in `spring-*.xml`

## shiro.ini
Here is the configuration for shiro.ini.

### Redis Standalone

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

# Redis host. If you don't specify host the default value is 127.0.0.1:6379
redisManager.host = 127.0.0.1:6379

# Redis connect timeout. Timeout for jedis try to connect to redis server(In milliseconds).(Optional)
#
# redisManager.timeout = <timeout>

# Redis password.(Optional)
#
# redisManager.password = <password>

# Redis database. Default value is 0(Optional)
#
# redisManager.database = <database>

# JedisPoolConfig (Optional)
# Most of time, you don't need to set jedisPoolConfig
#
# jedisPoolConfig = redis.clients.jedis.JedisPoolConfig
# jedisPoolConfig.<attribute> = <value>
# redisManager.jedisPoolConfig = jedisPoolConfig

# Scan count. Shiro-redis use Scan to get keys, so you can define the number of elements returned at every iteration. (Optional)
#
# redisManager.count = <count>

#====================================
# Redis-based session configuration
#====================================

# Create redisSessionDAO
redisSessionDAO = org.crazycake.shiro.RedisSessionDAO

# Redis cache key/value expire time. Default value: 1800 .The expire time is in second.
# Note: Shiro session default timeout is 1800s. Make sure expire time is longer than session timeout.  (Optional)
#
# redisManager.expire = <expire>

# Custom your redis key prefix for session management, if you doesn't define this parameter, shiro-redis will use 'shiro_redis_session:' as default prefix
# Note: Remember to add colon at the end of prefix.
#
# redisSessionDAO.keyPrefix = <session keyprefix>

# Use redisManager as cache manager
redisSessionDAO.redisManager = $redisManager

# doReadSession be called about 10 times when login. Save Session in ThreadLocal to resolve this problem. sessionInMemoryTimeout is expiration of Session in ThreadLocal.
# The default value is 1000 milliseconds (1s)
# Most of time, you don't need to change it.
#
# redisSessionDAO.sessionInMemoryTimeout = <timeout>

sessionManager = org.apache.shiro.web.session.mgt.DefaultWebSessionManager

sessionManager.sessionDAO = $redisSessionDAO

securityManager.sessionManager = $sessionManager

#=====================================
# Redis-based cache configuration
#=====================================

# Create cacheManager
cacheManager = org.crazycake.shiro.RedisCacheManager

# Redis cache key/value expire time. Default value: 1800 .The expire time is in second. (Optional)
#
# cacheManager.expire = <expire>

# If you want change charset of keySerializer or use your own custom serializer, you need to define serializer first
#
# cacheManagerKeySerializer = org.crazycake.shiro.serializer.StringSerializer

# Refer to https://docs.oracle.com/javase/8/docs/technotes/guides/intl/encoding.doc.html
# UTF-8, UTF-16, UTF-32, ISO-8859-1, GBK, Big5, etc
#
# cacheManagerKeySerializer.charset = UTF-8

# cacheManager.keySerializer = $cacheManagerKeySerializer

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

### Redis Sentinel
if you're using Redis Sentinel, please change the redisManager configuration into the following:
```properties
#===================================
# Redis Manager
#===================================

# Create redisManager
redisManager = org.crazycake.shiro.RedisSentinelManager

# Sentinel host. If you don't specify host the default value is 127.0.0.1:26379,127.0.0.1:26380,127.0.0.1:26381
redisManager.host = 127.0.0.1:26379,127.0.0.1:26380,127.0.0.1:26381

# Sentinel master name
redisManager.masterName = mymaster

# Redis connect timeout. Timeout for jedis try to connect to redis server(In milliseconds).(Optional)
#
# redisManager.timeout = <timeout>

# Timeout for jedis try to read data from redis server (Optional)
#
# redisManager.soTimeout = <soTimeout>

# Redis password.(Optional)
#
# redisManager.password = <password>

# Redis database. Default value is 0 (Optional)
#
# redisManager.database = <database>

# JedisPoolConfig (Optional)
# Most of time, you don't need to set jedisPoolConfig
#
# jedisPoolConfig = redis.clients.jedis.JedisPoolConfig
# jedisPoolConfig.<attribute> = <value>
# redisManager.jedisPoolConfig = jedisPoolConfig

# Scan count. Shiro-redis use Scan to get keys, so you can define the number of elements returned at every iteration. (Optional)
#
# redisManager.count = <count>
```

### Redis Cluster
If you're using redis cluster, here is an example of configuration :

```properties
# Create redisManager
redisManager = org.crazycake.shiro.RedisClusterManager

# Redis host and port list
redisManager.host = 192.168.21.3:7000,192.168.21.3:7001,192.168.21.3:7002,192.168.21.3:7003,192.168.21.3:7004,192.168.21.3:7005

# Redis connect timeout. Timeout for jedis try to connect to redis server(In milliseconds).(Optional)
#
# redisManager.timeout = 2000

# timeout for jedis try to read data from redis server (Optional)
#
# redisManager.soTimeout = 2000

# max attempts to connect to server (Optional)
#
# redisManager.maxAttempts = 3

# Redis password.(Optional)
#
# redisManager.password = <password>

```

## Spring

### Redis Standalone
spring.xml:
```xml
<!-- shiro-redis configuration [start] -->
<!-- shiro redisManager -->
<bean id="redisManager" class="org.crazycake.shiro.RedisManager">
    <property name="host" value="127.0.0.1:6379"/>
    <!-- optional properties:
    <property name="timeout" value="10000"/>
    <property name="password" value="123456"/>
    <property name="database" value="1"/>
    <property name="jedisPoolConfig" ref="jedisPoolConfig"/>
    <property name="count" value="100"/>
    -->
</bean>

<!-- Redis-based session configuration -->
<bean id="redisSessionDAO" class="org.crazycake.shiro.RedisSessionDAO">
    <property name="redisManager" ref="redisManager" />
    <property name="expire" value="1800"/>
    <property name="keyPrefix" value="shiro:session:" />
</bean>
<bean id="sessionManager" class="org.apache.shiro.web.session.mgt.DefaultWebSessionManager">
    <property name="sessionDAO" ref="redisSessionDAO" />
</bean>

<!-- Redis-based cache configuration -->
<bean id="cacheManager" class="org.crazycake.shiro.RedisCacheManager">
    <property name="redisManager" ref="redisManager" />
    <property name="expire" value="1800"/>
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

### Redis Sentinel
If you use redis sentinel, here is an example of configuration :
```xml
<!-- shiro-redis configuration [start] -->
<!-- shiro redisManager -->
<bean id="redisManager" class="org.crazycake.shiro.RedisSentinelManager">
    <property name="host" value="127.0.0.1:26379,127.0.0.1:26380,127.0.0.1:26381"/>
    <property name="masterName" value="mymaster"/>
    <!-- optional properties:、
    <property name="timeout" value="2000"/>
    <property name="soTimeout" value="2000"/>
    <property name="password" value=""/>
    <property name="database" value="0"/>
    <property name="count" value="100"/>
    -->
</bean>
```

### Redis Cluster
If you use redis cluster, here is an example of configuration :
```xml
<!-- shiro-redis configuration [start] -->
<!-- shiro redisManager -->
<bean id="redisManager" class="org.crazycake.shiro.RedisClusterManager">
    <property name="host" value="192.168.21.3:7000,192.168.21.3:7001,192.168.21.3:7002,192.168.21.3:7003,192.168.21.3:7004,192.168.21.3:7005"/>
    <!-- optional properties:
    <property name="timeout" value="10000"/>
    <property name="soTimeout" value="10000"/>
    <property name="maxAttempts" value="2"/>
    <property name="password" value="123456"/>
    -->
</bean>
```

## Serializer
Since redis only accept `byte[]`, there comes to a serializer problem.
Shiro-redis is using StringSerializer as key serializer and ObjectSerializer as value serializer.
You can use your own custom serializer, as long as this custom serializer implemens `org.crazycake.shiro.serializer.RedisSerializer`

For example, you need to change the charset of keySerializer.
```properties
# If you want change charset of keySerializer or use your own custom serializer, you need to define serializer first
#
# cacheManagerKeySerializer = org.crazycake.shiro.serializer.StringSerializer

# Refer to https://docs.oracle.com/javase/8/docs/technotes/guides/intl/encoding.doc.html
# UTF-8, UTF-16, UTF-32, ISO-8859-1, GBK, Big5, etc
#
# cacheManagerKeySerializer.charset = UTF-8

# cacheManager.keySerializer = $cacheManagerKeySerializer
```

These 4 Serializers are replaceable:
- cacheManager.keySerializer
- cacheManager.valueSerializer
- redisSessionDAO.keySerializer
- redisSessionDAO.valueSerializer


# If you found any bugs

Please send email to alexxiyang@gmail.com

可以用中文
