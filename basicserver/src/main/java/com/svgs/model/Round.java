package com.svgs.model;

import java.util.Objects;

public class Round {
    public int show_id;
    public String round_name;
    public int round_id;

    public Round(int rid, int sid, String rn) {
        round_id = rid;
        show_id = sid;
        round_name = rn;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (!(o instanceof Round))
            return false;
        Round s = (Round) o;
        return this.hashCode() == s.hashCode();
    }

    @Override
    public int hashCode() {
        return Objects.hash(show_id, round_name);
    }
}
