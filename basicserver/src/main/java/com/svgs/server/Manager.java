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
        String uid = newUid("");
        String name = newName("");
        User result = new User(uid, name);
        users.add(result);
        return result;
    }

    public static String fetchGameInfo(String gid) {
        try {
            return gson.toJson(fetchGame(gid).getGameInfo());
        } catch (Exception e) {
            return Helper.genericError(e.getMessage());
        }
    }

    public static String selectQuestion(String uid, String gid, int question_index) {
        User u;
        try {
            u = fetchUser(uid);
        } catch (Exception e) {
            return genericError(e.getMessage());
        }
        Match g;
        try {
            g = fetchGame(gid);
        } catch (Exception e) {
            return genericError(e.getMessage());
        }
        if (!g.hasPlayerTwo()) {
            return genericError(String.format("game %s has not started yet",gid));
        }
        if (g.isGameOver()) {
            return genericError(String.format("game %s has ended", gid));
        }
        if (!g.isPlaying(uid)) {
            return genericError(String.format("user %s is not in game %s", uid, gid));
        }
        if (!g.getStage().equals("select")) {
            return genericError(String.format("game %s is not currently in a selection phase", gid));
        }
        if (!g.getActivePlayer().getUid().equals(uid)) {
            return genericError(String.format("user %s is not currently the active player", uid));
        }
        if (g.isAnswered(question_index)) {
            return genericError(String.format("question with index %d is already answered", question_index));
        }
        return g.selectQuestion(question_index);
    }

    public static String joinGame(String input) {
        record Tmp(String uid, String gid){};
        Tmp tmp;
        record Success(String success){};
        try {
            tmp = gson.fromJson(input, Tmp.class);
        } catch (Exception e) {
            return genericError(e.getMessage());
        }
        String uid = tmp.uid;
        String gid = tmp.gid;
        
        User u;
        try {
            u = fetchUser(uid);
        } catch (Exception e) {
            return genericError(e.getMessage());
        }
        if (isUserInGame(u.getUid())) {
            return genericError(String.format("user %s is already in a game", u.getUid()));
        }
        Match g;
        try {
            g = fetchGame(gid);
        } catch (Exception e) {
            return genericError(e.getMessage());
        }
        if (g.hasPlayerTwo()) {
            return genericError(String.format("game with id %s is full.", gid));
        }
        g.setPlayerTwo(u);
        return gson.toJson(new Success(String.format("user with uid %s added to game %s",u.getUid(), g.getGid())), Success.class);
    }

    public static Match fetchGame(String gid) throws Exception {
        for (Match g : games) {
            if (g.getGid().equals(gid)) {
                return g;
            }
        }
        throw new Exception(String.format("game with gid %s does not exist", gid));
    }
    
    public static String createGame(String inputJson) {
        Usr tmp = gson.fromJson(inputJson, Usr.class);
        System.out.println("parsed uid: " + tmp.uid);
        User u;
        try {
            u = fetchUser(tmp.uid);
        } catch (Exception e) {
            return (Helper.errorMessage("uid doesn't exist"));
        }
        if (isUserInGame(tmp.uid)) {
            return Helper.errorMessage("User is already in a game.");
        }
        System.out.println("made it past the tests");
        Match game = Helper.newMatch(u, newGid(""));
        games.add(game);
        System.out.println(game.getGid());
        return game.getGid();
    }

    static User fetchUser(String uid) throws Exception {
        for (User u : users) {
            if (u.getUid().equals(uid)) {
                return u;
            }
        }
        throw new Exception(String.format("user with uid %s does not exist", uid));
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
        if (nameInUse(s) || s.equals("")) {
            return newName(Names.getRandomName());
        }
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

    public static boolean nameInUse(String name) {
        for (User u : users)
            if (u.getName().equals(name))
                return true;
        return false;
    }
    private static String genericError(String message) {
        record Error(String error){}
        return gson.toJson(new Error(message));
    }
}
