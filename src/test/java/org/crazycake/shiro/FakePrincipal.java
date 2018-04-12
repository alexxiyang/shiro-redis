package org.crazycake.shiro;

public class FakePrincipal implements AuthCachePrincipal {

    private String username;

    @Override
    public String getAuthCacheKey() {
        return username;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
