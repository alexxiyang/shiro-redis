package org.crazycake.shiro;

import junit.framework.TestCase;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.config.IniSecurityManagerFactory;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.Factory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * 
 * @author Chris Spiliotopoulos
 * 
 */
public class TestRedisCacheManager extends TestCase {

	final static Logger log = LoggerFactory
			.getLogger(TestRedisCacheManager.class);

	@Before
	public void setUp() throws Exception {

		log.info("My First Apache Shiro Application");

		// create a factory instance
		Factory<SecurityManager> factory = new IniSecurityManagerFactory(
				"classpath:shiro.ini");

		// get a new security manager instance
		SecurityManager securityManager = factory.getInstance();

		// set it globally
		SecurityUtils.setSecurityManager(securityManager);
		
		/*
		 * login the current subject
		 */
		Subject subject = SecurityUtils.getSubject();
		
		// use a username/pass token
		UsernamePasswordToken token = new UsernamePasswordToken(
				"lonestarr", "vespa");
		token.setRememberMe(true);
		subject.login(token);
		
		log.info("User successfuly logged in");
	}

	
	@Test
	public void testUserLogin() throws Exception {

		


	}

	@After
	public void tearDown() throws Exception {
		
		// logout the subject
		Subject subject = SecurityUtils.getSubject();
		subject.logout();
		
	}

	
}
