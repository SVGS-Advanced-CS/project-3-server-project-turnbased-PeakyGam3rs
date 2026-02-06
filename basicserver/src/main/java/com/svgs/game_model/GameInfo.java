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
    
    
    public ArrayList<Event> event_log;

    // select certain levels of data to return
    public GameInfo report() {
        GameInfo result = new GameInfo();
        result.event_log = this.event_log;
        result.game_over = this.game_over;
        result.player_1_pts = this.player_1_pts;
        result.player_2_pts = this.player_2_pts;
        result.player_1_name = this.player_1_name;
        result.current_stage = this.current_stage;
        if (player_2_name == null) {
            return result;
        }
        result.active_player = this.active_player;
        result.player_2_name = this.player_2_name;
        result.current_stage = this.current_stage;
        result.categorys = this.categorys;
        if (current_stage.equals("select")) {
            return result;
        }
        result.active_question_index = this.active_question_index;
        return result;
    }

    // NOTES:
    // only active question/completed questions will have a non blank question text.
    // GameCategorys are initialized with blank question texts, no need to censor
    // this class only holds data about the game state

    private transient long startTime;
    private transient Match game;
    public GameInfo(Match owner, GameCategory[] cats) {
        startTime = System.currentTimeMillis();
        game = owner;
        event_log = new ArrayList<>();
        categorys = cats;
    }
    // blank profile to copy wanted data over to for dto purposes
    private GameInfo() {}
    // prepares the gameInfo return
    
    // data must be formatted properly, using json for simplicity
    // generic events are ignored for game logic, just included for users
    void logGenericEvent(String message) {
        event_log.add(new genericEvent(formattedTime(), message));
    }

    private String formattedTime() {
        return com.svgs.server.Helper.formatTime(System.currentTimeMillis()-startTime);
    }
}
