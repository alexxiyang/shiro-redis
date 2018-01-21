package org.crazycake.shiro;

import java.lang.reflect.Field;

public class TestUtils {

    public static void setPrivateField(Object obj, String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }
}
