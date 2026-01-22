package com.svgs.game_model;

import java.util.Objects;

public class User {
    private String uid;
    private String name;
    
    public User(String uid, String name) {
        this.uid = uid;
        this.name = name;
    }
    
    public String getUid() {
        return uid;
    }
    
    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(uid, name);
    }

    @Override
    public boolean equals(Object o) {
        if (o==null) return false;
        if (!(o instanceof User)) return false;
        User u = (User)o;
        return this.hashCode() == u.hashCode();
    }
}
