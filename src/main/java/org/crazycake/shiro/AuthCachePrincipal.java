package org.crazycake.shiro;

@Deprecated
public interface AuthCachePrincipal {

    /**
     * AuthCacheKey used to store authorization cache. The authorization cache key in Redis format is {cacheManagePrefix} + {realmName} + ":" + {authCacheKey}.
     * For example:
     * cacheManagePrefix = shiro:mycache:
     * realmName = exampleRealm.authorizationCache
     * authCacheKey = 123
     * authorization redis key = shiro:mycache:exampleRealm.authorizationCache:123
     *
     * In most cases, authCacheKey should be userId or userName
     *
     * @Deprecated use
     *
     * @return
     */
    @Deprecated
    String getAuthCacheKey();
}
