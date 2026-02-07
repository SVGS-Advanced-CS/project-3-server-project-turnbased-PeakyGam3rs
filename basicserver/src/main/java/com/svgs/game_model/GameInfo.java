package com.svgs.game_model;

import java.util.ArrayList;
import java.util.HashMap;

import com.svgs.model.Question;
import com.svgs.server.BadRequest;


// a class to store data for a Match, doubling as a dto
public class GameInfo {
    String gid;
    String game_over; // "true"/"false"
    int player_1_pts;
    int player_2_pts;
    String player_1_name;
    String player_2_name; // blank if null, and if so, all below is null.
    String current_stage; // "waiting"/"answer"/"select"
    int active_question_index;
    // waiting means no player two
    String active_player; // active player name, or blank if none
    GameCategory[] categories;
    transient long startTime;
    
    transient User p1;
    transient User p2;
    transient HashMap<Integer, Question> questionMap;
    
    ArrayList<Event> event_log;

    // select certain levels of data to return
    public GameInfo report() throws Exception {
        GameInfo result = new GameInfo();
        result.p1 = this.p1;
        result.event_log = this.event_log;
        result.game_over = this.game_over;
        result.player_1_pts = this.player_1_pts;
        result.player_2_pts = this.player_2_pts;
        result.player_1_name = this.player_1_name;
        result.current_stage = this.current_stage;
        if (player_2_name == null) {
            return result;
        }
        result.p2 = this.p2;
        result.active_player = this.active_player;
        result.player_2_name = this.player_2_name;
        result.current_stage = this.current_stage;
        result.categories = this.categories;
        result.active_question_index = -1;
        if (current_stage.equals("select")) {
            return result;
        }
        result.active_question_index = this.active_question_index;
        
        return result;
    }

    GameQuestion fetchGameQuestion(int question_index) {
        for (GameCategory c : categories) {
            for (GameQuestion q : c.questions) {
                if (q.question_index == question_index) {
                    return q;
                }
            }
        }
        return null;
    }

    Question getQuestion(int question_index) {
        return questionMap.get(question_index);
    }

    void selectQuestion(int question_index) {
        this.current_stage = "answer";
        this.active_question_index = question_index;
        logSelectionEvent(question_index);
    }

    // NOTES:
    // only active question/completed questions will have a non blank question text.
    // GameCategorys are initialized with blank question texts, no need to censor
    // this class only holds data about the game state

    private transient Match game;
    GameInfo(Match owner, HashMap<Integer, Question> questionMap, GameCategory[] cats, User p1, String gid, long startTime) {
        this.questionMap = questionMap;
        game = owner;
        event_log = new ArrayList<>();
        categories = cats;
        this.p1 = p1;
        this.gid = gid;
        this.startTime = startTime;
    }

    User translateName(String playerName) throws Exception {
        if (p1.getName().equals(playerName)) {
            return p1;
        }
        if (p2.getName().equals(playerName)) {
            return p2;
        }
        throw new BadRequest("INVALID_NAME", String.format("no user with name %s in game", playerName));
    }

    // blank profile to copy wanted data over to for dto purposes
    private GameInfo() {}
    // prepares the gameInfo return
    
    // data must be formatted properly, using json for simplicity
    // generic events are ignored for game logic, just included for users
    void logGenericEvent(String message) throws Exception {
        event_log.add(new Event(timeStamp(), message));
    }
    void logSelectionEvent(int question_index) {
        String pName = this.active_player;
        event_log.add(new SelectionEvent(timeStamp(), pName, question_index, questionMap.get(question_index).getQuestion()));
    }

    public String timeStamp() {
        return formatTime(System.currentTimeMillis()-startTime);
    }

    public String formatTime(long input) {
        long time = input%(1000*60*60*24); // time is toroidal ig, idc if it messes with TWENTY-FOUR HOUR GAMES!
        long hours = time/(1000*60*60);
        time %= (1000*60*60);
        long minutes = time/(1000*60);
        time %= (1000*60);
        long seconds = time/1000;
        time %=1000;
        long hundredths = time/10;
        return String.format("%02d:%02d:%02d.%02ds", hours, minutes, seconds, hundredths);
    }
}
