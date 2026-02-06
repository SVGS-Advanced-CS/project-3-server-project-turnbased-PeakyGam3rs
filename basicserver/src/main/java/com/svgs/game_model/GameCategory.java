package com.svgs.game_model;

class GameCategory {
        public String category_title;
        public int category_index;
        public GameQuestion[] questions;

        public GameCategory(String category_title, int category_index, GameQuestion[] questions) {
            this.category_title = category_title;
            this.category_index = category_index;
            this.questions = questions;
        }
}
