package com.svgs.game_model;

import java.util.HashMap;

import com.svgs.model.Category;
import com.svgs.model.Question;

// more abstracted class for internal management
public class Match {
    private String gid;
    private User p1;
    private User p2;
    private GameInfo info; // basically a ledger
    private HashMap<Integer, Question> questionMap; // so gson doesn't serialize answers in GameInfo

    public Match(User one, Category[] cats, String gid) {
        questionMap = new HashMap<>();
        this.p1 = one;
        this.gid = gid;
        GameCategory[] kittens = new GameCategory[5];
        for (int i = 0; i < 5; i++) {
            Category source = cats[i];
            Question[] ogQs = source.getQuestions();
            GameQuestion[] qs = new GameQuestion[5];
            for (int j = 0; j < 5; j++) {
                Question ogQ = ogQs[j];
                int index = (i*5) + j;
                int value = ogQ.getValue();
                questionMap.put(index, ogQ);
                qs[j] = new GameQuestion(index, value, false, "");
            }
            while (!pointsAscending(qs)) {
                // ik its slow but im laaaazy and arr is small
                for (int j = 1; j < qs.length; j++) {
                    if (qs[j].point_value < qs[j-1].point_value) {
                        GameQuestion tmp = qs[j-1];
                        qs[j-1] = qs[j];
                        qs[j] = tmp;
                    }
                }
            }
            String title = source.getTitle();
            kittens[i] = new GameCategory(title, i, qs);
        }

        // data is parsed, now prepare GameInfo
        info = new GameInfo(this, kittens, p1);
        info.game_over = "false";
        info.player_1_pts = 0;
        info.player_2_pts = 0;
        info.player_1_name = p1.getName();
        info.current_stage = "waiting";
        info.logGenericEvent(String.format("%s created game %s", p1.getName(), gid));
    }
    
    public boolean hasPlayerTwo() {
        return info.p2 != null;
    }
    public boolean isGameOver() {
        return info.game_over.equals("true");
    }
    public void setPlayerTwo(User u) {
        info.p2 = u;
        info.player_2_name = p2.getName();
        info.player_2_pts = 0;
        info.active_player = ((int) (Math.random()) == 1 ? p1.getName() : p2.getName());
        info.logGenericEvent(String.format("%s joined the game",u.getName()));
    }
    public String getGid() {
        return gid;
    }

    public boolean isAnswered(int question_index) {
        return getQuestion(question_index).is_answered;
    }

    public GameQuestion getQuestion(int question_index) {
        return info.fetchQuestion(question_index);
    }

    public boolean isPlaying(String uid) {
        if (info.p2 == null) return info.p1.getUid().equals(uid);
        return info.p1.getUid().equals(uid)||info.p2.getUid().equals(uid);
    }

    public User getActivePlayer() {
        return info.translateName(info.active_player);
    }

    public String getStage() {
        return info.current_stage;
    }

    public GameInfo getGameInfo() {
        return info.report(); // available data logic handled in GameInfo
    }

    private boolean pointsAscending(GameQuestion[] qs) {
        for (int i = 1; i < qs.length; i++) {
            if (qs[i].point_value < qs[i - 1].point_value) {
                return false;
            }
        }
        return true;
    }
}