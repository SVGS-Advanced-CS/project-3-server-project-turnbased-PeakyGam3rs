package com.svgs.game_model;

public class Event {
    public String time;

    public Event(String time) {
        this.time = time;
    }

}
class genericEvent extends Event {
    public String message;

    public genericEvent(String time, String message) {
        super(time);
        this.message = message;
    }
    
}
