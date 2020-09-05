package org.crazycake.shiro.integration;

import com.github.javafaker.Faker;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.crazycake.shiro.RedisCacheManager;
import org.crazycake.shiro.RedisManager;
import org.crazycake.shiro.RedisSessionDAO;
import org.crazycake.shiro.exception.SerializationException;
import org.crazycake.shiro.integration.fixture.model.UserInfo;
import org.crazycake.shiro.serializer.ObjectSerializer;
import org.crazycake.shiro.serializer.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.crazycake.shiro.integration.fixture.TestFixture.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;

public class RedisManagerTest {
    private PrincipalCollection user1;
    private RedisManager redisManager;
    private StringSerializer keySerializer = new StringSerializer();
    private ObjectSerializer valueSerializer = new ObjectSerializer();

    private void scaffold() {
        redisManager = scaffoldStandaloneRedisManager();
        user1 = scaffoldAuthKey(scaffoldUser());
    }

    @BeforeEach
    public void setUp() {
        scaffold();
    }

    @AfterEach
    public void tearDown() {
        blastRedis();
    }

    @Test
    public void testSet() throws SerializationException, InterruptedException {
        UserInfo userInfo = (UserInfo)user1.getPrimaryPrincipal();
        this.redisManager.set(this.keySerializer.serialize("user:" + userInfo.getId()), this.valueSerializer.serialize(user1), 1);
        assertThat(this.redisManager.get(this.keySerializer.serialize("user:" + userInfo.getId())), is(this.valueSerializer.serialize(user1)));
        Thread.sleep(1500);
        byte[] aaa = this.redisManager.get(this.keySerializer.serialize("user:" + userInfo.getId()));
        assertThat(valueSerializer.deserialize(this.redisManager.get(this.keySerializer.serialize("user:" + userInfo.getId()))), is(nullValue()));
    }

    @Test
    public void testDel() throws SerializationException {
        UserInfo userInfo = (UserInfo)user1.getPrimaryPrincipal();
        this.redisManager.set(this.keySerializer.serialize("user:" + userInfo.getId()), this.valueSerializer.serialize(user1), 2);
        this.redisManager.del(this.keySerializer.serialize("user:" + userInfo.getId()));
        assertThat(this.redisManager.get(this.keySerializer.serialize("user:" + userInfo.getId())), is(nullValue()));
    }

    @Test
    public void testKeys() throws SerializationException {
        for (int i = 1; i < 121; i++) {
            UserInfo user = new UserInfo();
            user.setId(i);
            SimplePrincipalCollection principal = new SimplePrincipalCollection();
            principal.add(user, "student");
            this.redisManager.set(this.keySerializer.serialize("user:" + user.getId()), this.valueSerializer.serialize(principal), 10);
        }

        Set<byte[]> keys = this.redisManager.keys(this.keySerializer.serialize("user:*"));
        assertThat(keys.size(), is(120));
    }

    @Test
    public void testDbSize() throws SerializationException {
        for (int i = 1; i < 136; i++) {
            UserInfo user = new UserInfo();
            user.setId(i);
            SimplePrincipalCollection principal = new SimplePrincipalCollection();
            principal.add(user, "student");
            this.redisManager.set(this.keySerializer.serialize("user:" + user.getId()), this.valueSerializer.serialize(principal), 10);
        }

        Long dbSize = this.redisManager.dbSize(this.keySerializer.serialize("user:*"));
        assertThat(dbSize, is(135L));
    }
}
