package org.crazycake.shiro.model;

import org.apache.shiro.session.InvalidSessionException;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.SimpleSession;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;

public class FakeSession extends SimpleSession implements Serializable, Session{
    private Integer id;
    private String name;

    public FakeSession() {}

    public FakeSession(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Date getStartTimestamp() {
        return null;
    }

    @Override
    public Date getLastAccessTime() {
        return null;
    }

    @Override
    public void setTimeout(long l) throws InvalidSessionException {

    }

    @Override
    public String getHost() {
        return null;
    }

    @Override
    public void touch() throws InvalidSessionException {

    }

    @Override
    public void stop() throws InvalidSessionException {

    }

    @Override
    public Collection<Object> getAttributeKeys() throws InvalidSessionException {
        return null;
    }

    @Override
    public Object getAttribute(Object o) throws InvalidSessionException {
        return null;
    }

    @Override
    public void setAttribute(Object o, Object o1) throws InvalidSessionException {

    }

    @Override
    public Object removeAttribute(Object o) throws InvalidSessionException {
        return null;
    }
}
