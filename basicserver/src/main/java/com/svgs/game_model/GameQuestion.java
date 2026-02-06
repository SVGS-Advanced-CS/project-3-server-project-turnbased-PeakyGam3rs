package com.svgs.game_model;

public class GameQuestion {
    public int question_index;
    public int point_value;
    public boolean is_answered;
    public String question_text;
    
    public GameQuestion(int question_index, int point_value, boolean is_answered, String question_text) {
        this.question_index = question_index;
        this.point_value = point_value;
        this.is_answered = is_answered;
        this.question_text = question_text;
    }
}
