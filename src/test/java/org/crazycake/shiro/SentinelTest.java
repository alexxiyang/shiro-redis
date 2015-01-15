package org.crazycake.shiro;

import static org.junit.Assert.*;

import org.junit.Test;

public class SentinelTest {

  RedisManager m;

  @Test
  public void test() {

    m = new RedisManager();
    m.setSentinels("127.0.1.10:26379 127.0.1.11:26379 127.0.1.12:26379");
    m.setSentinelClusterName("sentinel_sstore");
    m.init();
  }

}
