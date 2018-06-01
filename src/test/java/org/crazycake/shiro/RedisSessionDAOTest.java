package org.crazycake.shiro;

import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.crazycake.shiro.exception.SerializationException;
import org.crazycake.shiro.serializer.ObjectSerializer;
import org.crazycake.shiro.serializer.StringSerializer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.Serializable;
import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class RedisSessionDAOTest {

    private RedisManager redisManager;
    private RedisSessionDAO redisSessionDAO;
    private StringSerializer keySerializer;

    private String testPrefix;
    private ObjectSerializer valueSeralizer;

    @Before
    public void setUp() throws SerializationException {
        keySerializer = new StringSerializer();
        valueSeralizer = new ObjectSerializer();
        testPrefix = "testPrefix:";
        redisManager = mock(RedisManager.class);
        redisSessionDAO = new RedisSessionDAO();
        redisSessionDAO.setRedisManager(redisManager);
        redisSessionDAO.setKeyPrefix(testPrefix);
        redisSessionDAO.setExpire(1);
    }

    @Test
    public void testDoCreate() throws SerializationException {
        try {
            redisSessionDAO.doCreate(null);
            fail();
        } catch (UnknownSessionException e) {
            assertThat(e.getMessage(), is("session is null"));
        }
        verify(redisManager, times(0)).set(any((new byte[0]).getClass()), any((new byte[0]).getClass()), eq(1));

        RedisSessionDAO prefixTestRedisSessionDao = new RedisSessionDAO();
        prefixTestRedisSessionDao.setKeyPrefix("abc:");
        prefixTestRedisSessionDao.setRedisManager(redisManager);
        prefixTestRedisSessionDao.setExpire(2);
        FakeSession fakeSession = new FakeSession(1, "Tom");
        String sessionId = (String)prefixTestRedisSessionDao.doCreate(fakeSession);
        verify(redisManager, times(0)).set(eq(keySerializer.serialize("abc:" + sessionId)), any((new byte[0]).getClass()), eq(2));
    }

    @Test
    public void testDoCreateWithSessionTimeout() {
        redisSessionDAO.setExpire(-2);
        FakeSession fakeSession = new FakeSession(2, "Jack");
        redisSessionDAO.doCreate(fakeSession);

        ArgumentCaptor<Integer> expireArg = ArgumentCaptor.forClass(Integer.class);
        verify(redisManager).set(any((new byte[0]).getClass()), any((new byte[0]).getClass()), expireArg.capture());
        assertThat(expireArg.getValue(), is(1800));
    }

    @Test
    public void testUpdate() throws SerializationException {
        FakeSession testSession = new FakeSession(1, "jack");
        byte[] testSessionKeyBytes = keySerializer.serialize(testPrefix + "1");
        byte[] testSessionValueBytes = valueSeralizer.serialize(testSession);
        redisSessionDAO.update(testSession);
        verify(redisManager, times(1)).set(testSessionKeyBytes, testSessionValueBytes, 1);
        try {
            redisSessionDAO.update(null);
            fail();
        } catch (UnknownSessionException e) {
            assertThat(e.getMessage(), is("session or session id is null"));
        }

        try {
            FakeSession nullIdSession = new FakeSession();
            redisSessionDAO.update(nullIdSession);
            fail();
        } catch (UnknownSessionException e) {
            assertThat(e.getMessage(), is("session or session id is null"));
        }
    }

    @Test
    public void testDelete() {
        redisSessionDAO.delete(null);
        verify(redisManager, times(0)).del(any((new byte[0]).getClass()));
        FakeSession nullIdSession = new FakeSession();
        redisSessionDAO.delete(nullIdSession);
        verify(redisManager, times(0)).del(any((new byte[0]).getClass()));
        FakeSession testSession = new FakeSession(2, "Tom");
        redisSessionDAO.delete(testSession);
        verify(redisManager, times(1)).del(any((new byte[0]).getClass()));
    }

    @Test
    public void testDoReadSession() throws NoSuchFieldException, IllegalAccessException {
        Session nullSession = redisSessionDAO.doReadSession(null);
        assertThat(nullSession, is(nullValue()));

        RedisSessionDAO redisSessionDAO2 = new RedisSessionDAO();
        redisSessionDAO2.setRedisManager(redisManager);
        redisSessionDAO2.setKeyPrefix(testPrefix);
        redisSessionDAO2.setExpire(2);
        ThreadLocal sessionsInThread = mock(ThreadLocal.class);
        Map<Serializable, SessionInMemory> sessionMap = new HashMap<Serializable, SessionInMemory>();
        SessionInMemory sessionInMemory = new SessionInMemory();
        sessionInMemory.setSession(new FakeSession(1, "Billy"));
        sessionInMemory.setCreateTime(new Date());
        sessionMap.put("1", sessionInMemory);
        when(sessionsInThread.get()).thenReturn(sessionMap);
        TestUtils.setPrivateField(redisSessionDAO2, "sessionsInThread", sessionsInThread);
        FakeSession actualSession = (FakeSession)redisSessionDAO2.doReadSession("1");
        assertThat(actualSession.getId().toString(), is("1"));
        assertThat(actualSession.getName(), is("Billy"));
        verify(redisManager, times(0)).get(any((new byte[0]).getClass()));
    }

    @Test
    public void testGetActiveSessions() throws SerializationException {
        Set<byte[]> fakeKeys = new HashSet<byte[]>();
        byte[] firstKeyBytes = keySerializer.serialize("1");
        fakeKeys.add(firstKeyBytes);
        byte[] secondKeyBytes = keySerializer.serialize("2");
        fakeKeys.add(secondKeyBytes);
        when(redisManager.keys(any((new byte[0]).getClass()))).thenReturn(fakeKeys);
        FakeSession firstSession = new FakeSession(1, "Tom");
        when(redisManager.get(firstKeyBytes)).thenReturn(valueSeralizer.serialize(firstSession));
        FakeSession secondSession = new FakeSession(2, "Billy");
        when(redisManager.get(secondKeyBytes)).thenReturn(valueSeralizer.serialize(secondSession));

        Collection<Session> activeSessions = redisSessionDAO.getActiveSessions();
        assertThat(activeSessions.size(), is(2));
    }
}
