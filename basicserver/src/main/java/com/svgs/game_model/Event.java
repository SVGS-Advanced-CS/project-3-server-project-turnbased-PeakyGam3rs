package com.svgs.game_model;

public class Event {
    public transient GameInfo info;
    public String time;
    public String message;

    public Event(GameInfo info, String message) throws Exception {
        this.info = info;
        this.time = info.formatTime(System.currentTimeMillis() - info.startTime);
        this.message = message;
    }

}
class SelectionEvent extends Event {
    public String question_text;
    public SelectionEvent(GameInfo info, String pName, int question_index) throws Exception {
        super(info, String.format("%s selection question with index %d",pName,question_index));
        question_text = info.getQuestion(question_index).getQuestion();
    }
}

