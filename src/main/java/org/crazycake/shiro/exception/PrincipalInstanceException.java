package org.crazycake.shiro.exception;

public class PrincipalInstanceException extends RuntimeException  {

    private static final String MESSAGE = "Principal must implement org.crazycake.shiro.AuthCachePrincipal.\n"
            + "shiro-redis will get the key for store authorization object in Redis from org.crazycake.shiro.AuthCachePrincipal\n"
            + "So please use AuthCachePrincipal to tell shiro-redis how to get the cache key\n"
            + " For example: There is a class UserInfo which implements org.crazycake.shiro.AuthCachePrincipal."
            + " You can use this class to initial SimpleAuthenticationInfo like this:\n"
            + " UserInfo userInfo = new userInfo();\n"
            + " new SimpleAuthenticationInfo(userInfo, \"123456\", \"realm1\")";

    public PrincipalInstanceException() {
        super(MESSAGE);
    }
}
