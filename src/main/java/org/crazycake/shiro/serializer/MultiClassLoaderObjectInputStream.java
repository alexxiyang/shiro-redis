package org.crazycake.shiro.serializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * For fixing https://github.com/alexxiyang/shiro-redis/issues/84
 */
public class MultiClassLoaderObjectInputStream extends ObjectInputStream {
	private static Logger logger = LoggerFactory.getLogger(MultiClassLoaderObjectInputStream.class);
	
	MultiClassLoaderObjectInputStream(InputStream str) throws IOException {
		super(str);
	}

	/**
	 * Try :
	 * 1. thread class loader
	 * 2. application class loader
	 * 3. system class loader
	 * @param desc
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	@Override
	protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        String name = desc.getName();

		try {
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			return Class.forName(name, false, cl);
		} catch (Throwable ex) {
			logger.debug("Cannot access thread context ClassLoader!", ex);
		}
		
		try {
			ClassLoader cl = MultiClassLoaderObjectInputStream.class.getClassLoader();
			return Class.forName(name, false, cl);
		} catch (Throwable ex) {
			logger.debug("Cannot access application ClassLoader", ex);
		}
		
		try {
			ClassLoader cl = ClassLoader.getSystemClassLoader();
			return Class.forName(name, false, cl);
		} catch (Throwable ex) {
			logger.debug("Cannot access system ClassLoader", ex);
		}

        return super.resolveClass(desc);
    }

}
