package com.svgs.dtos;

import java.util.Objects;

public class Qu {
    public String question;
    public String answer;
    public String value;
    public int show_id;
    public String round_name;
    public String category;
    public int category_id;

    public transient Integer round_id;
    public transient int question_id;

    public Qu(String q, String a, String v, int rid, int qid) {
        question = q;
        answer = a;
        value = v;
        round_id = rid;
        question_id = qid;
    }
    
    public void setCID(int cid) {
        category_id = cid;
    }

    public void setShowId(int sid) {
        show_id = sid;
    }

    public void setRoundId(int rid) {
        round_id = rid;
    }
    
    public Qu(String q, String a, String v, int sid) {
        question = q;
        answer = a;
        value = v;
        show_id = sid;
    }

    public Qu(Qu q, int rid, int qid) {
        this.question = q.question;
        this.answer = q.answer;
        this.show_id = q.show_id;
        this.value = q.value;
        this.round_name = q.round_name;
        this.round_id = rid;
        this.question_id = qid;
    }

    @Override
    public int hashCode() {
        return Objects.hash(question, answer, value, show_id); // dont hash round_name or any cat stuff
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (!(o instanceof Qu)) {
            return false;
        }
        Qu q = (Qu) o;
        return this.hashCode() == q.hashCode();
    }
}
