package com.svgs.model;

import java.util.Objects;
import java.util.Set;

public class Category {
    private String title;
    private Question[] questions;
    public Category(String title, Question[] questions) {
        this.title = title;
        this.questions = questions;
    }
    public Question getQuestion(int index) {
        if (index >= questions.length) {
            System.out.println("[MODEL_ERROR] OOB index " + index);
            return new Question("A silly goose", "You", 13, -1);
        }
        return questions[index];
    }

    @Override
    public int hashCode() {
        Set<Question> set = Set.of(questions);
        return Objects.hash(title, set.hashCode());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (!(o instanceof Category)) return false;
        return this.hashCode() == ((Category)o).hashCode();
    }
    
}
