package fixture;

import com.github.javafaker.Faker;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.crazycake.shiro.RedisCache;
import org.crazycake.shiro.RedisManager;
import org.crazycake.shiro.RedisSessionDAO;
import org.crazycake.shiro.exception.SerializationException;
import org.crazycake.shiro.model.FakeAuth;
import org.crazycake.shiro.model.FakeSession;
import org.crazycake.shiro.model.UserInfo;
import org.crazycake.shiro.serializer.RedisSerializer;
import org.junit.Assert;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.hamcrest.CoreMatchers.*;

public class TestFixture {

    private static Properties properties = loadProperties("shiro-standalone.ini");
    private static Faker faker = new Faker();

    //  /$$       /$$                       /$$
    // | $$      | $$                      | $$
    // | $$$$$$$ | $$  /$$$$$$   /$$$$$$$ /$$$$$$
    // | $$__  $$| $$ |____  $$ /$$_____/|_  $$_/
    // | $$  \ $$| $$  /$$$$$$$|  $$$$$$   | $$
    // | $$  | $$| $$ /$$__  $$ \____  $$  | $$ /$$
    // | $$$$$$$/| $$|  $$$$$$$ /$$$$$$$/  |  $$$$/
    // |_______/ |__/ \_______/|_______/    \___/


    public static void blastRedis() {
        Jedis jedis = new Jedis(properties.getProperty("redisManager.host").split(":")[0]);
        jedis.flushAll();
        jedis.close();
    }


    //                                /$$$$$$   /$$$$$$          /$$       /$$
    //                               /$$__  $$ /$$__  $$        | $$      | $$
    //  /$$$$$$$  /$$$$$$$  /$$$$$$ | $$  \__/| $$  \__//$$$$$$ | $$  /$$$$$$$
    // /$$_____/ /$$_____/ |____  $$| $$$$    | $$$$   /$$__  $$| $$ /$$__  $$
    //|  $$$$$$ | $$        /$$$$$$$| $$_/    | $$_/  | $$  \ $$| $$| $$  | $$
    // \____  $$| $$       /$$__  $$| $$      | $$    | $$  | $$| $$| $$  | $$
    // /$$$$$$$/|  $$$$$$$|  $$$$$$$| $$      | $$    |  $$$$$$/| $$|  $$$$$$$
    //|_______/  \_______/ \_______/|__/      |__/     \______/ |__/ \_______/

    public static <K, V> RedisCache scaffoldRedisCache(RedisManager redisManager, RedisSerializer<K> keySerializer, RedisSerializer<V> valueSerializer, String prefix, int expire, String principalIdFieldName) {
        return new RedisCache<K, V>(redisManager, keySerializer, valueSerializer, prefix, expire, principalIdFieldName);
    }

    public static RedisSessionDAO scaffoldRedisSessionDAO(RedisManager redisManager, String prefix) {
        RedisSessionDAO redisSessionDAO = new RedisSessionDAO();
        redisSessionDAO.setRedisManager(redisManager);
        redisSessionDAO.setKeyPrefix(prefix);
        redisSessionDAO.setExpire(NumberUtils.toInt(properties.getProperty("redisSessionDAO.expire")));
        return redisSessionDAO;
    }

    public static String scaffoldPrefix() {
        return faker.university().name().replace(" ", "_") + ":";
    }

    public static RedisManager scaffoldStandaloneRedisManager() {
        RedisManager redisManager = new RedisManager();
        redisManager.setHost(properties.getProperty("redisManager.host"));
        return redisManager;
    }

    public static UserInfo scaffoldUser() {
        UserInfo user = new UserInfo();
        user.setId(faker.number().randomDigitNotZero());
        user.setUsername(faker.name().username());
        user.setAge(faker.number().numberBetween(18, 60));
        user.setRole(faker.number().randomDigitNotZero());
        return user;
    }

    public static FakeSession scaffoldSession() {
        return new FakeSession(faker.number().randomDigitNotZero(), faker.name().username());
    }

    public static FakeSession scaffoldEmptySession() {
        return new FakeSession();
    }

    public static String scaffoldUsername() {
        return faker.name().username();
    }

    public static PrincipalCollection scaffoldAuthKey(UserInfo user) {
        SimplePrincipalCollection key = new SimplePrincipalCollection();
        key.add(user, faker.beer().name());
        return key;
    }

    public static Set scaffoldKeys(Object... users) {
        Set keys = new HashSet();
        for (Object user : users) {
            keys.add(user);
        }
        return keys;
    }

    //                         /$$     /$$
    //                        | $$    |__/
    //    /$$$$$$   /$$$$$$$ /$$$$$$   /$$  /$$$$$$  /$$$$$$$   /$$$$$$$
    //   |____  $$ /$$_____/|_  $$_/  | $$ /$$__  $$| $$__  $$ /$$_____/
    //    /$$$$$$$| $$        | $$    | $$| $$  \ $$| $$  \ $$|  $$$$$$
    //   /$$__  $$| $$        | $$ /$$| $$| $$  | $$| $$  | $$ \____  $$
    //  |  $$$$$$$|  $$$$$$$  |  $$$$/| $$|  $$$$$$/| $$  | $$ /$$$$$$$/
    //   \_______/ \_______/   \___/  |__/ \______/ |__/  |__/|_______/

    public static void doPutAuth(RedisCache redisCache, PrincipalCollection user) {
        if (user == null) {
            redisCache.put(null, null);
            return;
        }
        redisCache.put(user, turnUserToFakeAuth((UserInfo)user.getPrimaryPrincipal()));
    }

    public static void doRemoveAuth(RedisCache redisCache, PrincipalCollection user) {
        redisCache.remove(user);
    }

    public static void doClearAuth(RedisCache redisCache) {
        redisCache.clear();
    }

    public static Set doKeysAuth(RedisCache redisCache) {
        return redisCache.keys();
    }

    public static void doSetSessionDAOExpire(RedisSessionDAO redisSessionDAO, int expire) {
        redisSessionDAO.setExpire(expire);
    }

    public static void doChangeSessionName(FakeSession session, String name) {
        session.setName(name);
    }

    //                                                      /$$
    //                                                     | $$
    //    /$$$$$$   /$$$$$$$ /$$$$$$$  /$$$$$$   /$$$$$$  /$$$$$$
    //   |____  $$ /$$_____//$$_____/ /$$__  $$ /$$__  $$|_  $$_/
    //    /$$$$$$$|  $$$$$$|  $$$$$$ | $$$$$$$$| $$  \__/  | $$
    //   /$$__  $$ \____  $$\____  $$| $$_____/| $$        | $$ /$$
    //  |  $$$$$$$ /$$$$$$$//$$$$$$$/|  $$$$$$$| $$        |  $$$$/
    //   \_______/|_______/|_______/  \_______/|__/         \___/

    public static void assertRedisEmpty() {
        Jedis jedis = new Jedis(properties.getProperty("redisManager.host").split(":")[0]);
        assertThat("Redis should be empty",jedis.dbSize(), is(0L));
    }

    public static void assertKeysEquals(Set actualKeys, Set expectKeys) {
        Assert.assertEquals(expectKeys, actualKeys);
    }

    public static void assertAuthEquals(FakeAuth actualAuth, FakeAuth expectAuth) {
        assertThat(actualAuth.getId(), is(expectAuth.getId()));
        assertThat(actualAuth.getRole(), is(expectAuth.getRole()));
    }

    public static void assertPrincipalInstanceException(Exception e) {
        assertThat(e, is(notNullValue()));
        assertThat(e.getMessage(), containsString("must has getter for field: " + properties.getProperty("cacheManager.principalIdFieldName")));
    }

    public static void assertEquals(Object actual, Object expect) {
        assertThat(actual,is(expect));
    }

    public static void assertSessionEquals(Session actualSession, Session expectSession) {
        assertThat(actualSession.getId(), is(expectSession.getId()));
        assertThat(((FakeSession)actualSession).getName(), is(((FakeSession)expectSession).getName()));
    }

    //     /$$                                             /$$$$$$
    //    | $$                                            /$$__  $$
    //   /$$$$$$    /$$$$$$  /$$$$$$  /$$$$$$$   /$$$$$$$| $$  \__//$$$$$$   /$$$$$$
    //  |_  $$_/   /$$__  $$|____  $$| $$__  $$ /$$_____/| $$$$   /$$__  $$ /$$__  $$
    //    | $$    | $$  \__/ /$$$$$$$| $$  \ $$|  $$$$$$ | $$_/  | $$$$$$$$| $$  \__/
    //    | $$ /$$| $$      /$$__  $$| $$  | $$ \____  $$| $$    | $$_____/| $$
    //    |  $$$$/| $$     |  $$$$$$$| $$  | $$ /$$$$$$$/| $$    |  $$$$$$$| $$
    //     \___/  |__/      \_______/|__/  |__/|_______/ |__/     \_______/|__/
    //

    public static Set turnPrincipalCollectionToString(Set<PrincipalCollection> users, String prefix) {
        Set<String> keys = new HashSet<String>();
        for (PrincipalCollection user : users) {
            keys.add(prefix + ((UserInfo)user.getPrimaryPrincipal()).getId());
        }
        return keys;
    }

    public static FakeAuth turnUserToFakeAuth(UserInfo user) {
        FakeAuth auth = new FakeAuth();
        auth.setId(user.getId());
        auth.setRole(user.getRole());
        return auth;
    }

    //     /$$                         /$$
    //    | $$                        | $$
    //   /$$$$$$    /$$$$$$   /$$$$$$ | $$  /$$$$$$$
    //  |_  $$_/   /$$__  $$ /$$__  $$| $$ /$$_____/
    //    | $$    | $$  \ $$| $$  \ $$| $$|  $$$$$$
    //    | $$ /$$| $$  | $$| $$  | $$| $$ \____  $$
    //    |  $$$$/|  $$$$$$/|  $$$$$$/| $$ /$$$$$$$/
    //     \___/   \______/  \______/ |__/|_______/

    public static Properties loadProperties(String propFileName) {

        Properties props = new Properties();
        InputStream inputStream = TestFixture.class.getClassLoader()
                .getResourceAsStream(propFileName);

        if (inputStream != null) {
            try {
                props.load(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return props;
    }

    public static Long getRedisTTL(String key, RedisSerializer keySerializer) {
        Jedis jedis = new Jedis(properties.getProperty("redisManager.host").split(":")[0]);
        Long ttl = 0L;
        try {
            ttl = jedis.ttl(keySerializer.serialize(key));
        } catch (SerializationException e) {
            e.printStackTrace();
        }
        jedis.close();
        return ttl;
    }

}
