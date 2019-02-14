package org.crazycake.shiro;

import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.crazycake.shiro.exception.SerializationException;
import org.crazycake.shiro.model.FakeSession;
import org.crazycake.shiro.serializer.StringSerializer;
import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import static fixture.TestFixture.*;
import static org.junit.Assert.fail;

public class RedisSessionDAOTest {

    private RedisSessionDAO redisSessionDAO;
    private FakeSession session1;
    private FakeSession session2;
    private FakeSession emptySession;
    private String name1;
    private String prefix;
    private void blast() {
        blastRedis();
    }

    private void scaffold() {
        prefix = scaffoldPrefix();
        RedisManager redisManager = scaffoldStandaloneRedisManager();
        redisSessionDAO = scaffoldRedisSessionDAO(redisManager, prefix);
        session1 = scaffoldSession();
        session2 = scaffoldSession();
        emptySession = scaffoldEmptySession();
        name1 = scaffoldUsername();
    }

    @Before
    public void setUp() {
        blast();
        scaffold();
    }

    @Test
    public void testDoCreateNull() {
        try {
            redisSessionDAO.doCreate(null);
            fail();
        } catch (UnknownSessionException e) {
            assertEquals(e.getMessage(), "session is null");
        }
    }

    @Test
    public void testDoCreate() {
        redisSessionDAO.doCreate(session1);
        Session actualSession = redisSessionDAO.doReadSession(session1.getId());
        assertSessionEquals(actualSession, session1);
    }

    @Test
    public void testDoCreateWithSessionTimeout() {
        doSetSessionDAOExpire(redisSessionDAO, -2);
        redisSessionDAO.doCreate(session2);
        assertEquals(getRedisTTL(prefix + session2.getId(), new StringSerializer()), 1800L);
    }

    @Test
    public void testUpdateNull() {
        try {
            redisSessionDAO.update(null);
            fail();
        } catch (UnknownSessionException e) {
            assertEquals(e.getMessage(), "session or session id is null");
        }
    }

    @Test
    public void testUpdateEmptySession() {
        try {
            redisSessionDAO.update(emptySession);
            fail();
        } catch (UnknownSessionException e) {
            assertEquals(e.getMessage(), "session or session id is null");
        }
    }

    @Test
    public void testUpdate() {
        redisSessionDAO.doCreate(session1);
        redisSessionDAO.doReadSession(session1.getId());
        doChangeSessionName(session1, name1);
        redisSessionDAO.update(session1);
        FakeSession actualSession = (FakeSession)redisSessionDAO.doReadSession(session1.getId());
        assertEquals(actualSession.getName(), name1);
    }

    @Test
    public void testUpdateWithoutSessionInMemory() {
        redisSessionDAO.setSessionInMemoryEnabled(false);
        redisSessionDAO.doCreate(session1);
        redisSessionDAO.doReadSession(session1.getId());
        doChangeSessionName(session1, name1);
        redisSessionDAO.update(session1);
        FakeSession actualSession = (FakeSession)redisSessionDAO.doReadSession(session1.getId());
        assertEquals(actualSession.getName(), name1);
    }

    @Test
    public void testDelete() {
        redisSessionDAO.doCreate(session1);
        redisSessionDAO.delete(session1);
        assertRedisEmpty();
    }

    @Test
    public void testGetActiveSessions() {
        redisSessionDAO.doCreate(session1);
        redisSessionDAO.doCreate(session2);
        Collection<Session> activeSessions = redisSessionDAO.getActiveSessions();
        assertEquals(activeSessions.size(), 2);
    }

    @Test
    public void testRemoveExpiredSessionInMemory() throws InterruptedException, SerializationException {
        redisSessionDAO.setSessionInMemoryTimeout(500L);
        redisSessionDAO.doCreate(session1);
        redisSessionDAO.doReadSession(session1.getId());
        Thread.sleep(1000);
        redisSessionDAO.doCreate(session2);
        redisSessionDAO.doReadSession(session2.getId());
        Map<Serializable, SessionInMemory> sessionMap = (Map<Serializable, SessionInMemory>) redisSessionDAO.getSessionsInThread().get();
        assertEquals(sessionMap.size(), 1);
    }
}
