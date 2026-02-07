package com.svgs.resp_model;

public class AnswerQuestionResponse {
    boolean is_correct;
    int points_gained;
    String answer_text;

    public AnswerQuestionResponse(boolean is_correct, int points_gained, String answer_text) {
        this.is_correct = is_correct;
        this.points_gained = points_gained;
        this.answer_text = answer_text;
    }
}
