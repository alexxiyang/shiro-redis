package org.crazycake.shiro;

import org.apache.shiro.subject.PrincipalCollection;
import org.crazycake.shiro.exception.SerializationException;
import org.crazycake.shiro.serializer.ObjectSerializer;
import org.crazycake.shiro.serializer.StringSerializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;
import static org.mockito.Mockito.*;

public class RedisCacheTest {
    private IRedisManager redisManager;
    private StringSerializer keySerializer = new StringSerializer();
    private ObjectSerializer valueSerializer = new ObjectSerializer();

    @BeforeEach
    public void setUp() {
        redisManager = mock(IRedisManager.class);
    }

    private RedisCache mountRedisCache() {
        return new RedisCache(redisManager, new StringSerializer(), new ObjectSerializer(), "employee:", 1, RedisCacheManager.DEFAULT_PRINCIPAL_ID_FIELD_NAME);
    }

    @Test
    public void testInitialize() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new RedisCache<String, String>(null, null, null, "abc:", 1, RedisCacheManager.DEFAULT_PRINCIPAL_ID_FIELD_NAME));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new RedisCache<String, String>(new RedisManager(), null, null, "abc:", 1, RedisCacheManager.DEFAULT_PRINCIPAL_ID_FIELD_NAME));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new RedisCache<String, String>(new RedisManager(), new StringSerializer(), null, "abc:", 1, RedisCacheManager.DEFAULT_PRINCIPAL_ID_FIELD_NAME));
    }

    @Test
    public void testPut() throws SerializationException {
        RedisCache rc = mountRedisCache();
        Object value = rc.put("foo", "bar");
        assertThat(value, is("bar"));
        verify(redisManager).set(keySerializer.serialize("employee:foo"), valueSerializer.serialize("bar"), 1);

        PrincipalCollection principal = new EmployeePrincipal(3);
        rc.put(principal, "account information");

        verify(redisManager).set(keySerializer.serialize("employee:3"), valueSerializer.serialize("account information"), 1);
    }
}

class Employee {
    private int id;

    public Employee(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }
}

class EmployeePrincipal implements PrincipalCollection {

    private Employee primaryPrincipal;

    public EmployeePrincipal(int id) {
        this.primaryPrincipal = new Employee(id);
    }

    @Override
    public Employee getPrimaryPrincipal() {
        return this.primaryPrincipal;
    }

    @Override
    public <T> T oneByType(Class<T> aClass) {
        return null;
    }

    @Override
    public <T> Collection<T> byType(Class<T> aClass) {
        return null;
    }

    @Override
    public List asList() {
        return null;
    }

    @Override
    public Set asSet() {
        return null;
    }

    @Override
    public Collection fromRealm(String s) {
        return null;
    }

    @Override
    public Set<String> getRealmNames() {
        return null;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public Iterator iterator() {
        return null;
    }
}
