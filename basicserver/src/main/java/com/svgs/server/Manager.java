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





// uids are considered private, and when a user creates a game they will by default be player 1.
// if you join a game, you are player two. this has no gameplay effect, just internal management.
//"game_over" : "true/false"
// if the game is ended, all results will be full b
//"player_1_pts" : <example>,
//"player_2_pts":<example>,
// names of players below
//"player_1_name" : "<example>",
//"player_2_name" : "<example>", // if no 2nd player (i.e blank name), all game instance data is blank.
//"round_type" : "<example "Jeopardy!">",
//"current_stage" : "waiting"/"answer"/"select",
// waiting means no 2nd player, answer and select are self explanatory.
//"active_player" : <active player num>,
// next, an array of 5 categories
// these categories have arrays of five questions.
// each question simply has an int value and an int index for the respective category.
// if a question is active, the below question text and index will not be empty and contain the //question.
//"question_text" : "<example>",
//"question_index" : <example>,
//{
//[
//"category_title" : "<some name>"
//"category_index" : <some int>,
// below is arr of five questions
//{
//[
//"question_index" : <example>,
//"point_value" : <example>,
//"is_answered" : "true"/"false"
//],
// rest of questions...
//}
//],
// rest of categories...
//}
//}
    public static String fetchGameInfo(String input) {
        record Parsed(String gid) {}
        String gid;
        try {
            gid = gson.fromJson(input, Parsed.class).gid;
        } catch (Exception e) {
            return Helper.genericError(e.getMessage());
        }
        if (!gidExists(gid)) {
            return Helper.genericError("game with gid %s does not exist", gid);
        }
        
        
    }

    public static String joinGame(String input) {
        record Tmp(String uid, String gid){};
        Tmp tmp;
        record Success(String success){};
        record Error(String success, String error) {};
        try {
            tmp = gson.fromJson(input, Tmp.class);
        } catch (Exception e) {
            return gson.toJson(new Error("false",e.getMessage()), Error.class);
        }
        String uid = tmp.uid;
        String gid = tmp.gid;
        
        User u;
        try {
            u = fetchUser(uid);
        } catch (Exception e) {
            return gson.toJson(new Error("false", e.getMessage()), Error.class);
        }
        if (isUserInGame(u.getUid())) {
            return gson.toJson(new Error("false", "user is already in a game"), Error.class);
        }
        Match g;
        try {
            g = fetchGame(gid);
        } catch (Exception e) {
            return gson.toJson(new Error("false", e.getMessage()), Error.class);
        }
        if (g.hasPlayerTwo()) {
            return gson.toJson(new Error("false", String.format("game with id %s is full.", gid)), Error.class);
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
        throw new Exception(String.format("gid %s does not exist", gid));
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
}
