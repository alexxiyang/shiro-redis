package org.crazycake.shiro.model;

import java.io.Serializable;

public class FakeAuth implements Serializable{
    private Integer id;
    private String role;

    public FakeAuth() {}

    public FakeAuth(Integer id, String role) {
        this.id = id;
        this.role = role;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
