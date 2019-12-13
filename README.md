shiro-redis
=============

## Introduction

shiro only provide the support of ehcache and concurrentHashMap. Here is an implement of redis cache can be used by shiro. Hope it will help you!

## Documentation

Official documentation [is located here](http://alexxiyang.github.io/shiro-redis/).

## 发现了个原作者的bug

由于jedis3.0改变了api,导致 直接调用作者的 redisSessionDao的getActiviesSessions方法会导致崩溃
所以我做出了修改,并向作者发起pr pr未合并之前 请引用我发布的版本

