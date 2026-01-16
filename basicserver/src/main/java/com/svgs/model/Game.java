package com.svgs.model;

import java.util.Objects;
import java.util.Set;

public class Game {
    private String title;
    private Category[] categories;
    public Game(String title, Category[] categories) {
        if (categories == null) return;
        if (categories.length < 5) return;
        if (title == null) return;
        this.title = title;
        this.categories = categories;
    }

    public String getTitle() {
        return title;
    }

    public Category getCategory(int index) {
        if (index >= 5) return null;
        return categories[index];
    }

    @Override
    public int hashCode() {
        Set<Category> set = Set.of(categories);
        return Objects.hash(title, set.hashCode());
    }

    @Override
    public boolean equals(Object o) {
        if (o==null) return false;
        if (!(o instanceof Game)) return false;
        return this.hashCode() == ((Game)o).hashCode();
    }
}
