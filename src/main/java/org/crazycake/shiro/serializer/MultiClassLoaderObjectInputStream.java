package org.crazycake.shiro.serializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiClassLoaderObjectInputStream extends ObjectInputStream {
	private static Logger log = LoggerFactory.getLogger(MultiClassLoaderObjectInputStream.class);
	
	MultiClassLoaderObjectInputStream(InputStream str) throws IOException {
		super(str);
	}

	@Override
	protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        String name = desc.getName();
        //log.debug("resolveClass:"+name);
        
		try {
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			return Class.forName(name, false, cl);
		}
		catch (Throwable ex) {
			log.debug(ex.getMessage());
			// Cannot access thread context ClassLoader - falling back...
		}
		
		try {
			// No thread context class loader -> use class loader of this class.
			ClassLoader cl = MultiClassLoaderObjectInputStream.class.getClassLoader();
			return Class.forName(name, false, cl);
		}
		catch (Throwable ex) {
			log.debug(ex.getMessage());
			// Cannot access thread context ClassLoader - falling back...
		}
		
		// getClassLoader() returning null indicates the bootstrap ClassLoader
		try {
			ClassLoader cl = ClassLoader.getSystemClassLoader();
			return Class.forName(name, false, cl);
		}
		catch (Throwable ex) {
			log.debug(ex.getMessage());
			// Cannot access system ClassLoader - oh well, maybe the caller can live with null...
		}

        return super.resolveClass(desc);
    }

}
