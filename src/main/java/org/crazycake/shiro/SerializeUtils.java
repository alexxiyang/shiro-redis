package org.crazycake.shiro;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SerializeUtils {

	private static Logger logger = LoggerFactory.getLogger(SerializeUtils.class);

	/**
	 * 反序列化
	 *
	 * @param bytes
	 * @return
	 */
	public static Object deserialize(byte[] bytes) {
		long start = System.currentTimeMillis();
		Object result = null;

		if (isEmpty(bytes)) {
			return null;
		}

		try {
			ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
			try {
				ObjectInputStream objectInputStream = new ObjectInputStream(byteStream);
				try {
					result = objectInputStream.readObject();
				} catch (ClassNotFoundException ex) {
					throw new Exception("Failed to deserialize object type", ex);
				}
			} catch (Throwable ex) {
				throw new Exception("Failed to deserialize", ex);
			}
		} catch (Exception e) {
			logger.error("Failed to deserialize", e);
		}
		if (logger.isDebugEnabled()) {
			logger.debug("deserialize object time is:" + (System.currentTimeMillis() - start));
		}
		return result;
	}

	public static boolean isEmpty(byte[] data) {
		return (data == null || data.length == 0);
	}

	/**
	 * 序列化
	 *
	 * @param object
	 * @return
	 */
	public static byte[] serialize(Object object) {
		long start = System.currentTimeMillis();
		byte[] result = null;

		if (object == null) {
			return new byte[0];
		}
		try {
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream(128);
			try {
				if (!(object instanceof Serializable)) {
					throw new IllegalArgumentException(SerializeUtils.class.getSimpleName() + " requires a Serializable payload " +
							"but received an object of type [" + object.getClass().getName() + "]");
				}
				ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteStream);
				objectOutputStream.writeObject(object);
				objectOutputStream.flush();
				result = byteStream.toByteArray();
			} catch (Throwable ex) {
				throw new Exception("Failed to serialize", ex);
			}
		} catch (Exception ex) {
			logger.error("Failed to serialize", ex);
		}
		if (logger.isDebugEnabled()) {
			logger.debug("serialize object time is:" + (System.currentTimeMillis() - start));
		}
		return result;
	}
}
