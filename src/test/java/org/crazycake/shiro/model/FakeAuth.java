package org.crazycake.shiro.model;

import java.io.Serializable;

public class FakeAuth implements Serializable{
    private Integer id;
    private Integer role;

    public FakeAuth() {}

    public FakeAuth(Integer id, Integer role) {
        this.id = id;
        this.role = role;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getRole() {
        return role;
    }

    public void setRole(Integer role) {
        this.role = role;
    }
}
