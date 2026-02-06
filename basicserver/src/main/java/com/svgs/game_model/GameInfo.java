package com.svgs.game_model;

import java.util.ArrayList;


// a class to store data for a Match, doubling as a dto
public class GameInfo {

    public String game_over; // "true"/"false"
    public int player_1_pts;
    public int player_2_pts;
    public String player_1_name;
    public String player_2_name; // blank if null, and if so, all below is null.
    public String round_type;
    public String current_stage; // "waiting"/"answer"/"select"
    // waiting means no player two
    public String active_player; // active player name, or blank if none
    private GameCategory[] categorys;
    
    public ArrayList<Event> event_log;

    // omit select data for serialization
    public GameInfo report() {
        GameInfo result = new GameInfo();
        
        return result;
    }

    // NOTES:
    // only active question will have a non blank question text.
    private transient long startTime;
    private transient Match game;
    public GameInfo(Match owner, GameCategory[] cats) {
        startTime = System.currentTimeMillis();
        game = owner;
        event_log = new ArrayList<>();
        categorys = cats;
        init();
    }
    private void init() {
        logGenericEvent(String.format("game %s created by %s", game.getGid(), player_1_name));
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
