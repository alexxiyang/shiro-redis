package org.crazycake.shiro;

import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.crazycake.shiro.common.SessionInMemory;
import org.crazycake.shiro.exception.SerializationException;
import org.crazycake.shiro.integration.fixture.model.FakeSession;
import org.crazycake.shiro.serializer.ObjectSerializer;
import org.crazycake.shiro.serializer.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import static org.crazycake.shiro.integration.fixture.TestFixture.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * RedisSessionDAO integration test was put under org.crazycake.shiro
 * is because I want to test protected method `doReadSession`
 */
public class RedisSessionDAOIntegrationTest {

    private RedisSessionDAO redisSessionDAO;
    private FakeSession session1;
    private FakeSession session2;
    private FakeSession emptySession;
    private String name1;
    private String prefix;
    private StringSerializer keySerializer = new StringSerializer();
    private ObjectSerializer valueSerializer = new ObjectSerializer();

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

    @BeforeEach
    public void setUp() {
        blast();
        scaffold();
    }

    @AfterEach
    public void tearDown() {
        blast();
    }

    @Test
    public void testDoCreateNull() {
        Assertions.assertThrows(UnknownSessionException.class, () -> {
            redisSessionDAO.doCreate(null);
        });
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
        Assertions.assertThrows(UnknownSessionException.class, () -> {
            redisSessionDAO.update(null);
        });
    }

    @Test
    public void testUpdateEmptySession() {
        Assertions.assertThrows(UnknownSessionException.class, () -> {
            redisSessionDAO.update(emptySession);
        });
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

    @Test
    public void testTurnOffSessionInMemoryEnabled() throws InterruptedException, SerializationException {
        redisSessionDAO.setSessionInMemoryTimeout(2000L);
        session1.setCompany("apple");
        redisSessionDAO.doCreate(session1);
        // Load session into SessionInThread
        redisSessionDAO.doReadSession(session1.getId());
        // Directly update session in Redis
        session1.setCompany("google");
        RedisManager redisManager = scaffoldStandaloneRedisManager();
        String sessionRedisKey = prefix + session1.getId();
        redisManager.set(keySerializer.serialize(sessionRedisKey), valueSerializer.serialize(session1), 10);
        // Try to read session again
        Thread.sleep(500);
        FakeSession sessionFromThreadLocal = (FakeSession)redisSessionDAO.doReadSession(session1.getId());
        // The company should be the old value
        assertThat(sessionFromThreadLocal.getCompany(), is("apple"));
        // Turn off sessionInMemoryEnabled
        redisSessionDAO.setSessionInMemoryEnabled(false);
        // Try to read session again. It should get the version in Redis
        FakeSession sessionFromRedis = (FakeSession)redisSessionDAO.doReadSession(session1.getId());
        assertThat(sessionFromRedis.getCompany(), is("google"));
    }
}
