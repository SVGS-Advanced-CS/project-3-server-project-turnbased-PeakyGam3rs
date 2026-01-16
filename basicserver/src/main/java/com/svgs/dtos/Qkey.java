/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.svgs.dtos;

import java.util.Objects;


public class Qkey {

    public int show_id;
    public String round_name;
    public String question;
    public String answer;
    public String value;
    public Qkey(int show_id, String round_name, String question, String answer, String value) {
    }

    @Override
    public int hashCode() {
        return Objects.hash(show_id, round_name, question, answer, value);
    }

    @Override
    public boolean equals(Object o) {
        if (o==null) return false;
        if (!(o instanceof Qkey)) return false;
        Qkey q = (Qkey) o;
        return this.hashCode() == q.hashCode();
    }

}
