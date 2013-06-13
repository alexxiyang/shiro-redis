shiro-redis
===========

shiro only provide the support of ehcache and concurrentHashMap. Here is an implement of redis cache can be used by shiro. Hope it will help you!

How to use it?
===========

edit in shiro.ini

#required
cacheManager = org.yqr.shiro.RedisCacheManager
#optional if you don't specify host the default value is 127.0.0.1
cacheManager.host=127.0.0.1
#optional , default value: 6379
cacheManager.port=6379
#optional, default value:0 .The expire time is in second
cacheManager.expire=5
#required
securityManager.cacheManager = $cacheManager
