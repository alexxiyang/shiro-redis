package org.crazycake.shiro;

public interface RedisSerializer<T> {

    byte[] serialize(T t) throws SerializationException;

    T deserialize(byte[] bytes) throws SerializationException;
}
