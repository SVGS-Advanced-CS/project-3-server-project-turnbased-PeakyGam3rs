package com.svgs.dtos;

import java.util.Objects;


public class Show {
    public int show_id;
    public String air_date;

    public Show(int id, String ad) {
        show_id = id;
        air_date = ad;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (!(o instanceof Show))
            return false;
        Show s = (Show) o;
        return this.hashCode() == s.hashCode();
    }

    @Override
    public int hashCode() {
        return Objects.hash(show_id, air_date);
    }

}
