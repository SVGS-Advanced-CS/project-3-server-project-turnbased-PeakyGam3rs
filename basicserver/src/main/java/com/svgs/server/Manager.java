package com.svgs.server;

import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.svgs.game_model.Match;
import com.svgs.game_model.User;
import com.svgs.req_dtos.AnswerQuestionRequest;
import com.svgs.req_dtos.CreateGameRequest;
import com.svgs.req_dtos.GameInfoRequest;
import com.svgs.req_dtos.JoinGameRequest;
import com.svgs.req_dtos.SelectQuestionRequest;
import com.svgs.resp_model.AnswerQuestionResponse;
import com.svgs.resp_model.CreateGameResponse;
import com.svgs.resp_model.JoinGameResponse;
import com.svgs.resp_model.SelectQuestionResponse;


public class Manager {
    private static final ConcurrentHashMap<String, User> usersById = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Match> gamesById = new ConcurrentHashMap<>();
    private static final Gson gson = new Gson();

    public static AnswerQuestionResponse answerQuestion(AnswerQuestionRequest input) throws Exception {
        User u = fetchUser(input.uid);
        Match g = fetchGame(input.gid);
        int question_index = checkIntegerInput(input.question_index, 0, 25);

        if (!g.userCanAnswer(u)) {
            throw new Conflict("WRONG_USER", "it is not this users turn");
        }

        if (g.questionIsAnswered(question_index)) {
            throw new Conflict("ALREADY_ANSWERED", "this question has already been answered");
        }

        if (input.answer_text == null) {
            throw new BadRequest("MISSING_FIELD", "missing answer_text field");
        }

        String inputRaw = input.answer_text;
        
        boolean isCorrect = g.isCorrect(question_index, inputRaw);
        g.answer(question_index, isCorrect, u, inputRaw);
        int points_change = g.getQuestion(question_index).getValue();
        if (!isCorrect) {
            points_change *= -1;
        }
        return new AnswerQuestionResponse(isCorrect, points_change, g.getQuestion(question_index).getAnswer());
    }
    
    
     /*
     * response (answer_question):
     * 
     * {
     * 
     * 
     * "answer_correct" : "true"/"false" // only true if correct answer fuzzy equals
     * 
     * "points_gained" : <example> // points gained for answer
     * 
     * "answer_text" : ""
     * }
     */

    public static User createUser() throws Exception {
        String uid = newUid();
        String name = newName();
        User result = new User(uid, name);
        usersById.put(uid, result);
        return result;
    }

    public static String fetchGameInfo(GameInfoRequest input) throws Exception {
        Match g = fetchGame(input.gid);
        return gson.toJson(g.getGameInfo());
    }

    public static SelectQuestionResponse selectQuestion(SelectQuestionRequest input) throws Exception{
        User u = fetchUser(input.uid);
        Match g = fetchGame(input.gid);

        int question_index = checkIntegerInput(input.question_index, 0, 25);
        if (question_index < 0 || question_index > 24) {
            throw new BadRequest("INVALID_INPUT", String.format("question index %d out of range [0,24]",question_index));
        }
        
        if (!g.hasPlayerTwo()) {
            throw new Conflict("NO_PLAYER_TWO", "waiting on 2nd player");
        }

        if (g.isGameOver()) {
            throw new Conflict("GAME_OVER", "this game is over");
        }

        if (!g.isPlaying(u.getUid())) {
            throw new Conflict("NOT_IN_GAME", String.format("user not in game"));
        }

        if (!g.canSelect()) {
            throw new Conflict("WRONG_PHASE", "not in selection phase");
        }

        if (!g.isActivePlayer(u)) {
            throw new Conflict("WRONG_TURN", "user is not currently the active player");
        }

        if (g.isAnswered(question_index)) {
            throw new Conflict("ALREADY_ANSWERED", "this question is already answered");
        }

        g.selectQuestion(question_index);
        String question_text = g.getQuestion(question_index).getQuestion();
        return new SelectQuestionResponse(question_text);
    }

    public static JoinGameResponse joinGame(JoinGameRequest input) throws Exception {
        User u = fetchUser(input.uid);
        Match g = fetchGame(input.gid);
        if (isUserInGame(u.getUid())) {
            throw new Conflict("USER_IN_GAME", String.format("user w/ uid %s in other game", u.getUid()));
        }

        if (g.hasPlayerTwo()) {
            throw new Conflict("FULL_GAME", String.format("game w/ gid %s is full", g.getGid()));
        }

        g.setPlayerTwo(u);
        return new JoinGameResponse(true);
    }

    public static Match fetchGame(String gid) throws Exception {
        if (gid == null || gid.length() != 4) {
            throw new BadRequest("INVALID_INPUT", "gid is invalid");
        }
        if (gamesById.containsKey(gid)) {
            return gamesById.get(gid);
        }
        throw new NotFound("NOT_FOUND", String.format("no game with gid %s", gid));
    }
    
    public static CreateGameResponse createGame(CreateGameRequest input) throws Exception {
        if (input.uid == null || input.uid.length() != 8) {
            throw new BadRequest("INVALID_INPUT", "uid input is invalid");
        }
        User u = fetchUser(input.uid);
        if (isUserInGame(input.uid)) {
            throw new Conflict("IN_GAME", String.format("user w/ uid %s in other game", input.uid));
        }

        Match game = Helper.newMatch(u, newGid());
        gamesById.put(game.getGid(), game);

        return new CreateGameResponse(game.getGid());
    }

    static User fetchUser(String uid) throws Exception {
        if (uid == null || uid.length() != 8) {
            throw new BadRequest("INVALID_INPUT", "uid is invalid");
        }
        if (usersById.containsKey(uid)) {
            return usersById.get(uid);
        }
        throw new NotFound("NOT_FOUND", String.format("no user with uid %s",uid));
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

    public static int checkIntegerInput(Integer input, int lower_bounds, int upper_bounds) {
        if (input == null) {
            throw new BadRequest("MISSING_FIELD", "missing field (likely question_index)");
        }
        if (input < lower_bounds || input >= upper_bounds) {
            throw new BadRequest("INPUT_OOB",String.format("integer input (likely question_index) out of bounds [%d, %d)",lower_bounds, upper_bounds));
        }
        return input;
    }

}
