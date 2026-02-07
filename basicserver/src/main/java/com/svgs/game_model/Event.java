package com.svgs.game_model;

import com.svgs.model.Question;

public class Event {
    public String timeStamp;
    public String message;

    public Event(String timeStamp, String message) {
        this.timeStamp = timeStamp;
        this.message = message;
    }

}
class SelectionEvent extends Event {
    public String question_text;
    public SelectionEvent(String timeStamp, String pName, int question_index, String question_text) {
        super(timeStamp, String.format("%s selected question with index %d",pName,question_index));
        this.question_text = question_text;
    }
}
class AnswerEvent extends Event {
    public String question_text;
    public String answer_text;
    public int question_index;
    public String player_name;
    public int points_change;
    public String user_answer;
    public boolean is_correct;

    public transient User u;
    public transient Question q;

    public AnswerEvent(String timeStamp, int question_index, String user_answer, User u, Question q, boolean is_correct) {  
        super(timeStamp, String.format("%s answered %s for question with index %d (%scorrect) (%s%d points)\nthe answer is %s",
            u.getName(),
            user_answer,
            question_index,
            (is_correct ? "" : "in"),
            (is_correct ? "+":"-"),
            q.getValue(),
            q.getAnswer()
        ));
        this.q = q;
        this.u = u;
        this.question_text = q.getQuestion();
        this.answer_text = q.getAnswer();
        this.user_answer = user_answer;
        this.question_index = question_index;
        this.player_name = u.getName();
        this.points_change = (is_correct ? 1 : -1) * q.getValue();
        this.is_correct = is_correct;
    }
}


