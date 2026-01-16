package com.svgs.model;

import java.util.Objects;

public class Question {
    private String question;
    private String answer;
    private int value;
    private int db_id;

    public Question(String question, String answer, int value, int db_id) {
        if (question == null || answer == null) {
            System.out.println("[MODEL_ERROR] null init. vals");
            return;
        }
        this.question = question;
        this.answer = answer;
        this.value = value;
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }

    public int getValue() {
        return value;
    }

    public int getDBID() {
        return db_id;
    }

    public void print() {
        System.out.println("Q: " + question);
        System.out.println("A: " + answer);
        System.out.println("For $" + value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(question, answer, value, db_id);
    }

    @Override
    public boolean equals(Object o) {
        if (o==null) return false;
        if (!(o instanceof Question)) return false;
        return this.hashCode() == ((Question)o).hashCode();
    }

}
