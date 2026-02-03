package com.svgs.server;

import java.util.ArrayList;

import com.google.gson.Gson;
import com.svgs.dtos.Usr;
import com.svgs.game_model.Match;
import com.svgs.game_model.User;

public class Manager {
    private static final ArrayList<User> users = new ArrayList<>();
    private static final ArrayList<Match> games = new ArrayList<>();
    private static final Gson gson = new Gson();

    public static User createUser() {
        String uid = Helper.generateId(8);
        String name = Names.getRandomName();
        while (uidExists(uid))
            uid = Helper.generateId(8);
        while (nameExists(name))
            name = Names.getRandomName();
        User result = new User(uid, name);
        users.add(result);
        return result;
    }

    public static String createGame(String inputJson) {
        Usr tmp = gson.fromJson(inputJson, Usr.class);
        System.out.println("parsed uid: " + tmp.uid);
        if (!uidExists(tmp.uid)) {
            return(Helper.errorMessage("uid doesn't exist"));
        }
        if (isUserInGame(tmp.uid)) {
            return Helper.errorMessage("User is already in a game.");
        }
        System.out.println("made it past the tests");
        Match game = new Match(tmp.uid, findName(tmp.uid), Helper.initializeGame(), newGid(""));
        games.add(game);
        System.out.println(game.getGid());
        return game.getGid();
    }

    static String gidResult(String gid) {
        record res(String gid) {}
        return gson.toJson(new res(gid), res.class);
    }
 
    public static boolean isUserInGame(String uid) {
        for (Match g : games)
            if (g.isPlaying(uid))
                return true;
        return false;
    }

    public static String newGid(String s) {
        if (s.length() != 4) {
            return newGid(Helper.generateId(4));
        }
        if (gidExists(s)) {
            return newGid(Helper.generateId(4));
        }
        return s;
    }

    public static String newUid(String s) {
        if (s.length() != 8)
            return newUid(Helper.generateId(8));
        if (uidExists(s))
            return newUid(Helper.generateId(8));
        return s;
    }

    public static String newName(String s) {
        if (nameExists(s))
            return newName(Names.getRandomName());
        return s;
    }

    public static boolean gidExists(String gid) {
        for (Match g : games) {
            if (g.getGid().equals(gid)) {
                return true;
            }
        }
        return false;
    }

    public static boolean uidExists(String uid) {
        for (User u : users) {
            if (u.getUid().equals(uid)) {
                return true;
            }
        }
        return false;
    }

    public static String findName(String uid) {
        for (User u : users)
            if (u.getUid().equals(uid))
                return u.getName();
        return "Anthony Tyler";
    }

    public static boolean nameExists(String name) {
        for (User u : users)
            if (u.getName().equals(name))
                return true;
        return false;
    }
}
