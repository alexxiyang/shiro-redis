shiro-redis
=============

[![Build Status](https://travis-ci.org/alexxiyang/shiro-redis.svg?branch=master)](https://travis-ci.org/alexxiyang/shiro-redis)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.crazycake/shiro-redis/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.crazycake/shiro-redis)

shiro only provide the support of ehcache and concurrentHashMap. Here is an implement of redis cache can be used by shiro. Hope it will help you!

# Download

You can choose these 2 ways to include shiro-redis into your project
* use "git clone https://github.com/alexxiyang/shiro-redis.git" to clone project to your local workspace and build jar file by your self
* add maven dependency 

```xml
<dependency>
    <groupId>org.crazycake</groupId>
    <artifactId>shiro-redis</artifactId>
    <version>3.2.3</version>
</dependency>
```

> **Note:**\
> Do not use version < 3.1.0\
> **注意**：\
> 请不要使用3.1.0以下版本

# Before use
Here is the first thing you need to know. Shiro-redis needs an id field to identify your authorization object in Redis. So please make sure your principal class has a field which you can get unique id of this object. Please setting this id field name by `cacheManager.principalIdFieldName = <your id field name of principal object>`

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

Then the userInfo object is your principal object. You need to make sure `UserInfo` has an unique field to identify it in Redis. Take userId as an example:
```java
public class UserInfo implements Serializable{

    private Integer userId

    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getUserId() {
        return this.userId;
    }
}
```

Put userId as the value of `cacheManager.principalIdFieldName`, like this:
```properties
cacheManager.principalIdFieldName = userId
```

If you're using Spring, the configuration should be
```xml
<property name="principalIdFieldName" value="userId" />
```

Then shiro-redis will call `userInfo.getUserId()` to get the id for storing Redis object.

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
# Redis Manager [start]
#===================================

# Create redisManager
redisManager = org.crazycake.shiro.RedisManager

# Redis host. If you don't specify host the default value is 127.0.0.1:6379
redisManager.host = 127.0.0.1:6379

#===================================
# Redis Manager [end]
#===================================

#=========================================
# Redis session DAO [start]
#=========================================

# Create redisSessionDAO
redisSessionDAO = org.crazycake.shiro.RedisSessionDAO

# Use redisManager as cache manager
redisSessionDAO.redisManager = $redisManager

sessionManager = org.apache.shiro.web.session.mgt.DefaultWebSessionManager

sessionManager.sessionDAO = $redisSessionDAO

securityManager.sessionManager = $sessionManager

#=========================================
# Redis session DAO [end]
#=========================================

#==========================================
# Redis cache manager [start]
#==========================================

# Create cacheManager
cacheManager = org.crazycake.shiro.RedisCacheManager

# Principal id field name. The field which you can get unique id to identify this principal.
# For example, if you use UserInfo as Principal class, the id field maybe `id`, `userId`, `email`, etc.
# Remember to add getter to this id field. For example, `getId()`, `getUserId()`, `getEmail()`, etc.
# Default value is id, that means your principal object must has a method called `getId()`
#
cacheManager.principalIdFieldName = id

# Use redisManager as cache manager
cacheManager.redisManager = $redisManager

securityManager.cacheManager = $cacheManager

#==========================================
# Redis cache manager [end]
#==========================================

#=================================
# shiro-redis configuration [end]
#=================================
```

For complete configurable options list, check [Configurable Options](#configurable-options).

Here is a [tutorial project](https://github.com/alexxiyang/shiro-redis-tutorial) for you to understand how to configure `shiro-redis` in `shiro.ini`.

### Redis Sentinel
if you're using Redis Sentinel, please change the redisManager configuration into the following:
```properties
#===================================
# Redis Manager [start]
#===================================

# Create redisManager
redisManager = org.crazycake.shiro.RedisSentinelManager

# Sentinel host. If you don't specify host the default value is 127.0.0.1:26379,127.0.0.1:26380,127.0.0.1:26381
redisManager.host = 127.0.0.1:26379,127.0.0.1:26380,127.0.0.1:26381

# Sentinel master name
redisManager.masterName = mymaster

#===================================
# Redis Manager [end]
#===================================
```

For complete configurable options list, check [Configurable Options](#configurable-options).

### Redis Cluster
If you're using redis cluster, here is an example of configuration :

```properties
#===================================
# Redis Manager [start]
#===================================

# Create redisManager
redisManager = org.crazycake.shiro.RedisClusterManager

# Redis host and port list
redisManager.host = 192.168.21.3:7000,192.168.21.3:7001,192.168.21.3:7002,192.168.21.3:7003,192.168.21.3:7004,192.168.21.3:7005

#===================================
# Redis Manager [end]
#===================================
```

For complete configurable options list, check [Configurable Options](#configurable-options).

## Spring

### Redis Standalone
spring.xml:
```xml
<!-- shiro-redis configuration [start] -->

<!-- Redis Manager [start] -->
<bean id="redisManager" class="org.crazycake.shiro.RedisManager">
    <property name="host" value="127.0.0.1:6379"/>
</bean>
<!-- Redis Manager [end] -->

<!-- Redis session DAO [start] -->
<bean id="redisSessionDAO" class="org.crazycake.shiro.RedisSessionDAO">
    <property name="redisManager" ref="redisManager" />
</bean>
<bean id="sessionManager" class="org.apache.shiro.web.session.mgt.DefaultWebSessionManager">
    <property name="sessionDAO" ref="redisSessionDAO" />
</bean>
<!-- Redis session DAO [end] -->

<!-- Redis cache manager [start] -->
<bean id="cacheManager" class="org.crazycake.shiro.RedisCacheManager">
    <property name="redisManager" ref="redisManager" />
</bean>
<!-- Redis cache manager [end] -->

<bean id="securityManager" class="org.apache.shiro.web.mgt.DefaultWebSecurityManager">
    <property name="sessionManager" ref="sessionManager" />
    <property name="cacheManager" ref="cacheManager" />

    <!-- other configurations -->
    <property name="realm" ref="exampleRealm"/>
    <property name="rememberMeManager.cipherKey" value="kPH+bIxk5D2deZiIxcaaaA==" />
</bean>

<!-- shiro-redis configuration [end] -->
```

For complete configurable options list, check [Configurable Options](#configurable-options).

Here is a [tutorial project](https://github.com/alexxiyang/shiro-redis-spring-tutorial) for you to understand how to configure `shiro-redis` in spring configuration file.

### Redis Sentinel
If you use redis sentinel, here is an example of configuration :
```xml
<!-- shiro-redis configuration [start] -->
<!-- shiro redisManager -->
<bean id="redisManager" class="org.crazycake.shiro.RedisSentinelManager">
    <property name="host" value="127.0.0.1:26379,127.0.0.1:26380,127.0.0.1:26381"/>
    <property name="masterName" value="mymaster"/>
</bean>
```

For complete configurable options list, check [Configurable Options](#configurable-options).

### Redis Cluster
If you use redis cluster, here is an example of configuration :
```xml
<!-- shiro-redis configuration [start] -->
<!-- shiro redisManager -->
<bean id="redisManager" class="org.crazycake.shiro.RedisClusterManager">
    <property name="host" value="192.168.21.3:7000,192.168.21.3:7001,192.168.21.3:7002,192.168.21.3:7003,192.168.21.3:7004,192.168.21.3:7005"/>
</bean>
```

For complete configurable options list, check [Configurable Options](#configurable-options).

## Serializer
Since redis only accept `byte[]`, there comes to a serializer problem.
Shiro-redis is using StringSerializer as key serializer and ObjectSerializer as value serializer.
You can use your own custom serializer, as long as this custom serializer implemens `org.crazycake.shiro.serializer.RedisSerializer`

For example, let's change the charset of keySerializer.
```properties
# If you want change charset of keySerializer or use your own custom serializer, you need to define serializer first
#
# cacheManagerKeySerializer = org.crazycake.shiro.serializer.StringSerializer

# Supported encodings refer to https://docs.oracle.com/javase/8/docs/technotes/guides/intl/encoding.doc.html
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

## Configurable Options

### RedisManager

| Title              | Default              | Description                 |
| :------------------| :------------------- | :---------------------------|
| host               | `127.0.0.1:6379`     | Redis host. If you don't specify host the default value is `127.0.0.1:6379`. If you run redis in sentinel mode or cluster mode, separate host names with comma, like `127.0.0.1:26379,127.0.0.1:26380,127.0.0.1:26381` |
| masterName         | `mymaster`           | **Only used for sentinel mode**<br>The master node of Redis sentinel mode |
| timeout            | `2000`               | Redis connect timeout. Timeout for jedis try to connect to redis server(In milliseconds)  |
| soTimeout          | `2000`               | **Only used for sentinel mode or cluster mode**<br>The timeout for jedis try to read data from redis server |
| maxAttempts        | `3`                  | **Only used for cluster mode**<br>Max attempts to connect to server |
| password           |                      | Redis password |
| database           | `0`                  | Redis database. Default value is 0 |
| jedisPoolConfig    | `new redis.clients.jedis.JedisPoolConfig()` | JedisPoolConfig. You can create your own JedisPoolConfig instance and set attributes as you wish<br>Most of time, you don't need to set jedisPoolConfig<br>Here is an example.<br>`jedisPoolConfig = redis.clients.jedis.JedisPoolConfig`<br>`jedisPoolConfig.testWhileIdle = false`<br>`redisManager.jedisPoolConfig = jedisPoolConfig` |
| count              | `100`                |  Scan count. Shiro-redis use Scan to get keys, so you can define the number of elements returned at every iteration. |
| jedisPool          | `null`               | **Only used for sentinel mode or single mode**<br>You can create your own JedisPool instance and set attributes as you wish |

### RedisSessionDAO

| Title              | Default              | Description                 |
| :------------------| :------------------- | :---------------------------|
| redisManager       |                      | RedisManager which you just configured above (Required) |
| expire             | `-2`                 | Redis cache key/value expire time. The expire time is in second.<br>Special values:<br>`-1`: no expire<br>`-2`: the same timeout with session<br>Default value: `-2`<br>**Note**: Make sure expire time is longer than session timeout. |
| keyPrefix          | `shiro:session:`     | Custom your redis key prefix for session management<br>**Note**: Remember to add colon at the end of prefix. |
| sessionInMemoryTimeout | `1000`           | When we do signin, `doReadSession(sessionId)` will be called by shiro about 10 times. So shiro-redis save Session in ThreadLocal to remit this problem. sessionInMemoryTimeout is expiration of Session in ThreadLocal. <br>Most of time, you don't need to change it. |
| sessionInMemoryEnabled | `true`           | Whether or not enable temporary save session in ThreadLocal      |
| keySerializer        | `org.crazycake.shiro.serializer.StringSerializer` | The key serializer of cache manager<br>You can change the implement of key serializer or the encoding of StringSerializer.<br>Supported encodings refer to [Supported Encodings](https://docs.oracle.com/javase/8/docs/technotes/guides/intl/encoding.doc.html). Such as `UTF-8`, `UTF-16`, `UTF-32`, `ISO-8859-1`, `GBK`, `Big5`, etc<br>For more detail, check [Serializer](#serializer) |
| valueSerializer      | `org.crazycake.shiro.serializer.ObjectSerializer` | The value serializer of cache manager<br>You can change the implement of value serializer<br>For more detail, check [Serializer](#serializer) |

### CacheManager

| Title                | Default              | Description                 |
| :--------------------| :------------------- | :---------------------------|
| redisManager         |                      | RedisManager which you just configured above (Required) |
| principalIdFieldName | `id`                 | Principal id field name. The field which you can get unique id to identify this principal.<br>For example, if you use UserInfo as Principal class, the id field maybe `id`, `userId`, `email`, etc.<br>Remember to add getter to this id field. For example, `getId()`, `getUserId(`), `getEmail()`, etc.<br>Default value is `id`, that means your principal object must has a method called `getId()` |
| expire               | `1800`               | Redis cache key/value expire time. <br>The expire time is in second. |
| keyPrefix            | `shiro:cache:`       | Custom your redis key prefix for cache management<br>**Note**: Remember to add colon at the end of prefix. |
| keySerializer        | `org.crazycake.shiro.serializer.StringSerializer` | The key serializer of cache manager<br>You can change the implement of key serializer or the encoding of StringSerializer.<br>Supported encodings refer to [Supported Encodings](https://docs.oracle.com/javase/8/docs/technotes/guides/intl/encoding.doc.html). Such as `UTF-8`, `UTF-16`, `UTF-32`, `ISO-8859-1`, `GBK`, `Big5`, etc<br>For more detail, check [Serializer](#serializer) |
| valueSerializer      | `org.crazycake.shiro.serializer.ObjectSerializer` | The value serializer of cache manager<br>You can change the implement of value serializer<br>For more detail, check [Serializer](#serializer) |

# Spring boot starter

Shiro-redis’s Spring-Boot integration is the easiest way to integrate Shiro-redis into a Spring-base application.

> Note: `shiro-redis-spring-boot-starter` version `3.2.1` is based on `shiro-spring-boot-web-starter` version `1.4.0-RC2`

First include the Shiro-redis Spring boot starter dependency in you application classpath

```xml
<dependency>
    <groupId>org.crazycake</groupId>
    <artifactId>shiro-redis-spring-boot-starter</artifactId>
    <version>3.2.1</version>
</dependency>
```

The next step depends on whether you've created your own `SessionManager` or `SessionsSecurityManager`.
Because `shiro-redis-spring-boot-starter` will create `RedisSessionDAO` and `RedisCacheManager` for you. Then inject them into `SessionManager` and `SessionsSecurityManager` automatically.

But if you've created your own `SessionManager` or `SessionsSecurityManager` as below:
```java
@Bean
public SessionsSecurityManager securityManager(List<Realm> realms) {
    DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager(realms);
    // other stuff
    return securityManager;
}
```
You will have to inject them by yourself. for more deail, see below

## If you haven't created your own `SessionManager` or `SessionsSecurityManager`

You are all set. Enjoy it!

## If you have created your own `SessionManager` or `SessionsSecurityManager`

Inject `redisSessionDAO` and `redisCacheManager` which created by `shiro-redis-spring-boot-starter` already
```java
@Autowired
RedisSessionDAO redisSessionDAO;

@Autowired
RedisCacheManager redisCacheManager;
```

Inject them into `SessionManager` and `SessionsSecurityManager`

```java
@Bean
public SessionManager sessionManager() {
    DefaultWebSessionManager sessionManager = new DefaultWebSessionManager();

    // inject redisSessionDAO
    sessionManager.setSessionDAO(redisSessionDAO);
    return sessionManager;
}

@Bean
public SessionsSecurityManager securityManager(List<Realm> realms, SessionManager sessionManager) {
    DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager(realms);

    //inject sessionManager
    securityManager.setSessionManager(sessionManager);

    // inject redisCacheManager
    securityManager.setCacheManager(redisCacheManager);
    return securityManager;
}
```

For full example, see [shiro-redis-spring-boot-tutorial](https://github.com/alexxiyang/shiro-redis-spring-boot-tutorial)

### Configuration Properties

| Title                                              | Default              | Description                 |
| :--------------------------------------------------| :------------------- | :---------------------------|
| shiro-redis.enabled                                | `true`               | Enables shiro-redis’s Spring module |
| shiro-redis.redis-manager.deploy-mode               | `standalone`         | Redis deploy mode. Options: `standalone`, `sentinel`, 'cluster' |
| shiro-redis.redis-manager.host                      | `127.0.0.1:6379`     | Redis host. If you don't specify host the default value is `127.0.0.1:6379`. If you run redis in sentinel mode or cluster mode, separate host names with comma, like `127.0.0.1:26379,127.0.0.1:26380,127.0.0.1:26381` |
| shiro-redis.redis-manager.master-name               | `mymaster`           | **Only used for sentinel mode**<br>The master node of Redis sentinel mode |
| shiro-redis.redis-manager.timeout                   | `2000`               | Redis connect timeout. Timeout for jedis try to connect to redis server(In milliseconds)  |
| shiro-redis.redis-manager.so-timeout                | `2000`               | **Only used for sentinel mode or cluster mode**<br>The timeout for jedis try to read data from redis server |
| shiro-redis.redis-manager.max-attempts              | `3`                  | **Only used for cluster mode**<br>Max attempts to connect to server |
| shiro-redis.redis-manager.password                  |                      | Redis password |
| shiro-redis.redis-manager.database                  | `0`                  | Redis database. Default value is 0 |
| shiro-redis.redis-manager.count                     | `100`                |  Scan count. Shiro-redis use Scan to get keys, so you can define the number of elements returned at every iteration. |
| shiro-redis.session-dao.expire                     | `-2`                 | Redis cache key/value expire time. The expire time is in second.<br>Special values:<br>`-1`: no expire<br>`-2`: the same timeout with session<br>Default value: `-2`<br>**Note**: Make sure expire time is longer than session timeout. |
| shiro-redis.session-dao.key-prefix                 | `shiro:session:`     | Custom your redis key prefix for session management<br>**Note**: Remember to add colon at the end of prefix. |
| shiro-redis.session-dao.session-in-memory-timeout  | `1000`           | When we do signin, `doReadSession(sessionId)` will be called by shiro about 10 times. So shiro-redis save Session in ThreadLocal to remit this problem. sessionInMemoryTimeout is expiration of Session in ThreadLocal. <br>Most of time, you don't need to change it. |
| shiro-redis.session-dao.session-in-memory-enabled  | `true`           | Whether or not enable temporary save session in ThreadLocal      |
| shiro-redis.cache-manager.principal-id-field-name  | `id`                 | Principal id field name. The field which you can get unique id to identify this principal.<br>For example, if you use UserInfo as Principal class, the id field maybe `id`, `userId`, `email`, etc.<br>Remember to add getter to this id field. For example, `getId()`, `getUserId(`), `getEmail()`, etc.<br>Default value is `id`, that means your principal object must has a method called `getId()` |
| shiro-redis.cache-manager.expire                   | `1800`               | Redis cache key/value expire time. <br>The expire time is in second. |
| shiro-redis.cache-manager.key-prefix               | `shiro:cache:`       | Custom your redis key prefix for cache management<br>**Note**: Remember to add colon at the end of prefix. |


## Working with `spring-boot-devtools`
If you are using `shiro-redis` with `spring-boot-devtools`. Please add this line to `resources/META-INF/spring-devtools.properties` (Create it if there is no this file):
```ini
restart.include.shiro-redis=/shiro-[\\w-\\.]+jar
```

# If you found any bugs

Please create the issue

可以用中文
