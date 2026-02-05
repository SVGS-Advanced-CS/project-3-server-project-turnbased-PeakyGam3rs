package com.svgs.game_model;

import com.svgs.model.Category;

public class Match {
    private String gid;
    private User p1;
    private User p2;

    public Match(User one, Category[] cats, String gid) {
        this.p1 = one;
        this.gid = gid;
    }
    public boolean hasPlayerTwo() {
        return p2 != null;
    }
    public void setPlayerTwo(User u) {
        p2 = u;
    }
    public String getGid() {
        return gid;
    }
    public boolean isPlaying(String uid) {
        if (p2 == null) return p1.getUid().equals(uid);
        return p1.getUid().equals(uid)||p2.getUid().equals(uid);
    }
}