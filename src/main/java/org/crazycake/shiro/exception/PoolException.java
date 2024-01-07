package org.crazycake.shiro.exception;

/**
 * @author Teamo
 * @since 2022/05/18
 */
public class PoolException extends RuntimeException {
    /**
     * Constructs a new <code>LettucePoolException</code> instance.
     *
     * @param msg the detail message.
     */
    public PoolException(String msg) {
        super(msg);
    }

    /**
     * Constructs a new <code>LettucePoolException</code> instance.
     *
     * @param msg   the detail message.
     * @param cause the nested exception.
     */
    public PoolException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
