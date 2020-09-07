package org.crazycake.shiro;

import org.apache.shiro.session.InvalidSessionException;
import org.apache.shiro.session.Session;
import org.crazycake.shiro.exception.SerializationException;
import org.crazycake.shiro.serializer.ObjectSerializer;
import org.crazycake.shiro.serializer.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;

public class RedisSessionDAOTest {
    private IRedisManager redisManager;
    private StringSerializer keySerializer = new StringSerializer();
    private ObjectSerializer valueSerializer = new ObjectSerializer();

    @BeforeEach
    public void setUp() {
        redisManager = mock(IRedisManager.class);
    }

    private RedisSessionDAO mountRedisSessionDAO(Integer expire) {
        RedisSessionDAO redisSessionDAO = new RedisSessionDAO();
        if (expire != null) {
            redisSessionDAO.setExpire(expire);
        }
        redisSessionDAO.setKeyPrefix("student:");
        redisSessionDAO.setRedisManager(redisManager);
        return redisSessionDAO;
    }

    @Test
    public void testUpdate() throws SerializationException {
        RedisSessionDAO sessionDAO = mountRedisSessionDAO(null);
        StudentSession session = new StudentSession(99, 2000);
        sessionDAO.update(session);
        verify(redisManager).set(keySerializer.serialize("student:99"), valueSerializer.serialize(session), 2);
    }

    @Test
    public void testUpdateByCustomExpire() throws SerializationException {
        RedisSessionDAO sessionDAO = mountRedisSessionDAO(3);
        StudentSession session = new StudentSession(98, 2000);
        sessionDAO.update(session);
        verify(redisManager).set(keySerializer.serialize("student:98"), valueSerializer.serialize(session), 3);
    }

    @Test
    public void testUpdateByNoExpire() throws SerializationException {
        RedisSessionDAO sessionDAO = mountRedisSessionDAO(-1);
        StudentSession session = new StudentSession(97, 2000);
        sessionDAO.update(session);
        verify(redisManager).set(keySerializer.serialize("student:97"), valueSerializer.serialize(session), -1);
    }

    @Test
    public void testDelete() throws SerializationException {
        RedisSessionDAO sessionDAO = mountRedisSessionDAO(null);
        StudentSession session = new StudentSession(96, 1000);
        sessionDAO.delete(session);
        verify(redisManager).del(keySerializer.serialize("student:96"));
    }

    @Test
    public void testGetActiveSessions() throws SerializationException {
        Set<byte[]> mockKeys = new HashSet<byte[]>();
        mockKeys.add(keySerializer.serialize("student:1"));
        mockKeys.add(keySerializer.serialize("student:2"));
        when(redisManager.keys(keySerializer.serialize("student:*"))).thenReturn(mockKeys);

        StudentSession mockSession1 = new StudentSession(1, 2000);
        StudentSession mockSession2 = new StudentSession(2, 2000);
        when(redisManager.get(keySerializer.serialize("student:1"))).thenReturn(valueSerializer.serialize(mockSession1));
        when(redisManager.get(keySerializer.serialize("student:2"))).thenReturn(valueSerializer.serialize(mockSession2));

        RedisSessionDAO sessionDAO = mountRedisSessionDAO(null);
        assertThat(sessionDAO.getActiveSessions().size(), is(2));
    }
}

class StudentSession implements Session, Serializable {
    private Integer id;
    private long timeout;

    public StudentSession(Integer id, long timeout) {
        this.id = id;
        this.timeout = timeout;
    }

    @Override
    public Serializable getId() {
        return id;
    }

    @Override
    public Date getStartTimestamp() {
        return null;
    }

    @Override
    public Date getLastAccessTime() {
        return null;
    }

    @Override
    public long getTimeout() throws InvalidSessionException {
        return timeout;
    }

    @Override
    public void setTimeout(long l) throws InvalidSessionException {

    }

    @Override
    public String getHost() {
        return null;
    }

    @Override
    public void touch() throws InvalidSessionException {

    }

    @Override
    public void stop() throws InvalidSessionException {

    }

    @Override
    public Collection<Object> getAttributeKeys() throws InvalidSessionException {
        return null;
    }

    @Override
    public Object getAttribute(Object o) throws InvalidSessionException {
        return null;
    }

    @Override
    public void setAttribute(Object o, Object o1) throws InvalidSessionException {

    }

    @Override
    public Object removeAttribute(Object o) throws InvalidSessionException {
        return null;
    }
}
