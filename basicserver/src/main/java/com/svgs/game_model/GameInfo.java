package com.svgs.game_model;

import java.util.ArrayList;


// a class to store data for a Match, doubling as a dto
public class GameInfo {

    public String game_over; // "true"/"false"
    public int player_1_pts;
    public int player_2_pts;
    public String player_1_name;
    public String player_2_name; // blank if null, and if so, all below is null.
    public String current_stage; // "waiting"/"answer"/"select"
    public int active_question_index;
    // waiting means no player two
    public String active_player; // active player name, or blank if none
    public GameCategory[] categorys;
    
    public transient User p1;
    public transient User p2;
    
    public ArrayList<Event> event_log;

    public transient long startTime;

    // select certain levels of data to return
    public GameInfo report() {
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
        result.categorys = this.categorys;
        result.active_question_index = -1;
        if (current_stage.equals("select")) {
            return result;
        }
        result.active_question_index = this.active_question_index;
        
        return result;
    }

    public GameQuestion fetchQuestion(int question_index) {
        for (GameCategory c : categorys) {
            for (GameQuestion q : c.questions) {
                if (q.question_index == question_index) {
                    return q;
                }
            }
        }
        return new GameQuestion(-1, -1, false, "");
    }

    public void selectQuestion(int question_index) {
        this.current_stage = "answer";
        this.active_question_index = question_index;
        logSelectionEvent(question_index);
    }

    // NOTES:
    // only active question/completed questions will have a non blank question text.
    // GameCategorys are initialized with blank question texts, no need to censor
    // this class only holds data about the game state

    private transient Match game;
    public GameInfo(Match owner, GameCategory[] cats, User p1) {
        startTime = System.currentTimeMillis();
        game = owner;
        event_log = new ArrayList<>();
        categorys = cats;
        this.p1 = p1;
    }

    public User translateName(String playerName) {
        if (p1.getName().equals(playerName)) {
            return p1;
        }
        if (p2.getName().equals(playerName)) {
            return p2;
        }
        return new User();
    }

    // blank profile to copy wanted data over to for dto purposes
    private GameInfo() {}
    // prepares the gameInfo return
    
    // data must be formatted properly, using json for simplicity
    // generic events are ignored for game logic, just included for users
    void logGenericEvent(String message) {
        event_log.add(new Event(game, message));
    }
    void logSelectionEvent(int question_index) {
        String pName = this.active_player;
        event_log.add(new SelectionEvent(game, pName, question_index));
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
