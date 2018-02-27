package org.crazycake.shiro;

import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.SimpleSession;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RedisSessionDAOTest {

    private RedisManager redisManager;
    private RedisSessionDAO redisSessionDAO;
    private StringSerializer keySerializer;
    private String testKey;
    private String testPrefix;private Set<byte[]> testSet;
    private SimpleSession testValue;
    private Collection<FakeSession> testValues;
    private FakeSession tomSession;
    private FakeSession paulSession;
    private FakeSession billySession;
    private ObjectSerializer valueSeralizer;

    @Before
    public void setUp() throws SerializationException {
        keySerializer = new StringSerializer();
        valueSeralizer = new ObjectSerializer();
        testKey = "testKey";
        testPrefix = "testPrefix:";
        testValue = new SimpleSession();
        testValue.setId(3);
        testSet = new HashSet<byte[]>();
        testSet.add(keySerializer.serialize(testPrefix + "tom"));
        testSet.add(keySerializer.serialize(testPrefix + "paul"));
        testSet.add(keySerializer.serialize(testPrefix + "billy"));
        testValues = new ArrayList<FakeSession>();
        tomSession = new FakeSession(1, "tom");
        testValues.add(tomSession);
        paulSession = new FakeSession(2, "paul");
        testValues.add(paulSession);
        billySession = new FakeSession(3, "billy");
        testValues.add(billySession);
        redisManager = mock(RedisManager.class);
        when(redisManager.dbSize()).thenReturn(2L);
        when(redisManager.get(keySerializer.serialize(testPrefix + testKey))).thenReturn(valueSeralizer.serialize(testValue));
        when(redisManager.keys(keySerializer.serialize(testPrefix + "*"))).thenReturn(testSet);
        when(redisManager.get(keySerializer.serialize(testPrefix + "tom"))).thenReturn(valueSeralizer.serialize(tomSession));
        when(redisManager.get(keySerializer.serialize(testPrefix + "paul"))).thenReturn(valueSeralizer.serialize(paulSession));
        when(redisManager.get(keySerializer.serialize(testPrefix + "billy"))).thenReturn(valueSeralizer.serialize(billySession));
        redisSessionDAO = new RedisSessionDAO();
        redisSessionDAO.setRedisManager(redisManager);
        redisSessionDAO.setKeyPrefix(testPrefix);
    }

    @Test
    public void testUpdate() {
        redisSessionDAO.update(testValue);
        try {
            redisSessionDAO.update(null);
            fail();
        } catch (UnknownSessionException e) {
            assertThat(e.getMessage(), is("session or session id is null"));
        }
    }

    @Test
    public void testDelete() {
        redisSessionDAO.delete(null);
        redisSessionDAO.delete(testValue);
    }

    @Test
    public void testDoCreate() {
        redisSessionDAO.doCreate(testValue);
        try {
            redisSessionDAO.doCreate(null);
            fail();
        } catch (UnknownSessionException e) {
            assertThat(e.getMessage(), is("session is null"));
        }
    }

    @Test
    public void testDoReadSession() {
        Session actualSession = redisSessionDAO.doReadSession(testKey);
        assertThat(actualSession.getId().toString(), is("3"));
        redisSessionDAO.doReadSession(null);
    }

    @Test
    public void testGetActiveSessions() {
        Collection<Session> activeSessions = redisSessionDAO.getActiveSessions();
        assertThat(activeSessions.size(), is(3));
        for (Iterator<Session> iterator = activeSessions.iterator(); iterator.hasNext(); ) {
            FakeSession next = (FakeSession)iterator.next();
            if (next.getId() == 2) {
                assertThat(next.getName(), is("paul"));
            }
        }
    }
}
