shiro-redis
=============

## Introduction

shiro only provide the support of ehcache and concurrentHashMap. Here is an implement of redis cache can be used by shiro. Hope it will help you!

## Documentation

Official documentation [is located here](http://alexxiyang.github.io/shiro-redis/).


## This is a fork of [alexxiyang/shiro-redis](https://github.com/alexxiyang/shiro-redis) to fix bugs and add some new features.
## Update
- 3.4.0: Add Lettuce redis client support.
    Usage:  
    ```
    @Bean
    public IRedisManager redisManager() {
        LettuceRedisManager manager = new LettuceRedisManager("127.0.0.1", 6379);
        manager.setDatabase(Integer.parseInt(database));
        // or: LettuceRedisManager manager = LettuceRedisManager.builder().host("127.0.0.1").port(6379).build();
        return manager;
    }
    ```