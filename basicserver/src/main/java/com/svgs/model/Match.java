package com.svgs.model;

public class Match {
    private Player p1;
    private Player p2;
    private String gid;
    private record Player(String uid, String name) {};

    public Match(String uidp1, String namep1, Category[] cats, String gid) {

    } 
    public String getGid() {
        return gid;
    }
    public boolean isPlaying(String uid) {
        return p1.uid.equals(uid)||p2.uid.equals(uid);
    }
}