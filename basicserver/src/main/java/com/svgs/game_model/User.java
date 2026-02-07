package com.svgs.game_model;

import java.util.Objects;

import com.svgs.server.BadRequest;

public class User {
    private String uid;
    private String name;
    
    public User(String uid, String name) throws Exception {
        if (uid == null || uid.length() != 8) {
            throw new BadRequest("INVALID_INPUT", "invalid input for uid");
        }
        if (name == null || name.isEmpty()) {
            throw new BadRequest("INVALID_INPUT", "idk u prolly screwed something up");
        }
        this.uid = uid;
        this.name = name;
    }
    
    public String getUid() {
        return uid;
    }
    
    public String getName() {
        return name;
    }

    public void print() {
        System.out.printf("[USER_DEBUG] uid: %s\tname: %s", uid, name);
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
