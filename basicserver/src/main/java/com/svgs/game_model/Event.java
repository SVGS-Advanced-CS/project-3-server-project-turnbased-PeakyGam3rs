package com.svgs.game_model;

public class Event {
    public transient Match owner;
    public transient GameInfo info;
    public String time;
    public String message;

    public Event(Match owner, String message) {
        this.owner = owner;
        info = owner.getGameInfo();
        this.time = info.formatTime(System.currentTimeMillis() - info.startTime);
        this.message = message;
    }

}
class SelectionEvent extends Event {
    public String question_text;
    public SelectionEvent(Match owner, String pName, int question_index) {
        super(owner, String.format("%s selection question with index %d",pName,question_index));
        question_text = owner.getQuestion(question_index).getQuestion();
    }
}

