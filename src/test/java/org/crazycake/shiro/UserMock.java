/*
 * Copyright (C), 1996-2014
 * FileName: User.java
 * Author:   王华君
 * Date:     Nov 19, 2014 2:47:57 PM
 * Description: //模块目的、功能描述      
 * History: //修改记录
 * <author>      <time>      <version>    <desc>
 * 修改人姓名             修改时间            版本号                  描述
 */
package org.crazycake.shiro;

import java.io.Serializable;

/**
 * 〈一句话功能简述〉<br>
 * 〈功能详细描述〉
 * 
 * @author 王华君
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
public class UserMock implements Serializable{
	/**
	 */
	private static final long serialVersionUID = 1L;
	private String id;
	private String username;
	private String password;
	private String salt;
	public static final String OBJECT_KEY = "USER";

	private Boolean locked = Boolean.FALSE;

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @param username
	 *            the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @param password
	 *            the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * @return the salt
	 */
	public String getSalt() {
		return salt;
	}

	/**
	 * @param salt
	 *            the salt to set
	 */
	public void setSalt(String salt) {
		this.salt = salt;
	}

	/**
	 * @return the locked
	 */
	public Boolean getLocked() {
		return locked;
	}

	/**
	 * @param locked
	 *            the locked to set
	 */
	public void setLocked(Boolean locked) {
		this.locked = locked;
	}
	

    public String getCredentialsSalt() {
        return username + salt;
    }

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "User [id=" + id + ", username=" + username + ", password=" + password + ", salt=" + salt + ", locked="
				+ locked + "]";
	}

}
