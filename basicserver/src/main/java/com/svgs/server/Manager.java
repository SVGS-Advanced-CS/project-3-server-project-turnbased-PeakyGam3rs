package com.svgs.server;

import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.svgs.dtos.Usr;
import com.svgs.game_model.Match;
import com.svgs.game_model.User;

public class Manager {
    private static final ConcurrentHashMap<String, User> usersById = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Match> gamesById = new ConcurrentHashMap<>();
    private static final Gson gson = new Gson();

    public static User createUser() throws Exception {
        String uid = newUid();
        String name = newName();
        User result = new User(uid, name);
        usersById.put(uid, result);
        return result;
    }

    public static String fetchGameInfo(String gid) throws Exception {
            return gson.toJson(fetchGame(gid).getGameInfo());
    }

    public static String selectQuestion(String input_json) throws Exception{
        record Input(String uid, String gid, int question_index) {}
        Input input;
        input = gson.fromJson(input_json, Input.class);

        String gid = input.gid;
        String uid = input.uid;

        int question_index = input.question_index;
        if (gid == null || gid.equals("")) {
            throw new BadRequest("BAD_FIELD", "field gid empty");
        }
        if (uid.equals("")) {
            throw new BadRequest("BAD_FIELD", "field uid empty");
        }
        if (question_index < 0 || question_index > 24) {
            throw new BadRequest("INVALID_INPUT", String.format("question index %d out of range [0,24]",question_index));
        }

        User u = fetchUser(uid);
        Match g = fetchGame(gid);
        
        if (!g.hasPlayerTwo()) {
            throw new Conflict("NO_PLAYER_TWO", "waiting on 2nd player");
        }
        if (g.isGameOver()) {
            throw new Conflict("GAME_OVER", "this game is over");
        }
        if (!g.isPlaying(uid)) {
            throw new Conflict("NOT_IN_GAME", String.format("user not in game"));
        }
        if (!g.getStage().equals("select")) {
            throw new Conflict("WRONG_PHASE", "not in selection phase");
        }
        if (!g.getActivePlayer().getUid().equals(uid)) {
            throw new Conflict("WRONG_TURN", "user is not currently the active player");
        }
        if (g.isAnswered(question_index)) {
            throw new Conflict("ALREADY_ANSWERED", "this question is already answered");
        }
        return g.selectQuestion(question_index);
    }

    public static String joinGame(String input) throws Exception {
        record Tmp(String uid, String gid){};
        Tmp tmp = gson.fromJson(input, Tmp.class);
        record Success(String success){};
        String uid = tmp.uid;
        String gid = tmp.gid;
        
        User u = fetchUser(uid);
        
        if (isUserInGame(uid)) {
            throw new Conflict("USER_IN_GAME", String.format("user w/ uid %s in other game", uid));
        }
        Match g = fetchGame(gid);
        
        if (g.hasPlayerTwo()) {
            throw new Conflict("FULL_GAME", String.format("game w/ gid %s is full", gid));
        }
        g.setPlayerTwo(u);
        return gson.toJson(new Success(String.format("user with uid %s added to game %s",u.getUid(), g.getGid())));
    }

    public static Match fetchGame(String gid) throws Exception {
        if (gamesById.containsKey(gid)) {
            return gamesById.get(gid);
        }
        throw new NotFound("NOT_FOUND", String.format("no game with gid %s", gid));
    }
    
    public static String createGame(String inputJson) throws Exception {
        Usr tmp = gson.fromJson(inputJson, Usr.class);
        User u = fetchUser(tmp.uid);
        if (isUserInGame(tmp.uid)) {
            throw new Conflict("IN_GAME", String.format("user w/ uid %s in other game", tmp.uid));
        }
        Match game = Helper.newMatch(u, newGid());
        gamesById.put(game.getGid(), game);
        return game.getGid();
    }

    static User fetchUser(String uid) throws Exception {
        if (usersById.containsKey(uid)) {
            return usersById.get(uid);
        }
        throw new NotFound("NOT_FOUND", String.format("no user with uid %s",uid));
    }
    
    static String gidResult(String gid) throws Exception {
        record res(String gid) {}
        return gson.toJson(new res(gid), res.class);
    }
 
    public static boolean isUserInGame(String uid) throws Exception {
        for (Match g : gamesById.values())
            if (g.isPlaying(uid))
                return true;
        return false;
    }

    public static String newGid() {
        String result = Helper.generateId(4);
        while (gamesById.containsKey(result)) {
            result = Helper.generateId(4);
        }
        return result;
    }

    public static String newUid() {
        String result = Helper.generateId(8);
        while (usersById.containsKey(result)) {
            result = Helper.generateId(8);
        }
        return result;
    }

    public static String newName() throws Exception {
        String result = Names.getRandomName();
        while (nameInUse(result)) {
            result = Names.getRandomName();
        }
        return result;
    }

    public static boolean gidExists(String gid) {
        return gamesById.containsKey(gid);
    }

    public static boolean uidExists(String uid) {
        return usersById.containsKey(uid);
    }

    public static boolean nameInUse(String name) {
        for (User u : usersById.values()){
            if (u.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }
}
