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
        info = new GameInfo(this, kittens);
        info.game_over = "false";
        info.player_1_pts = 0;
        info.player_2_pts = 0;
        info.player_1_name = p1.getName();
        info.current_stage = "waiting";
        info.active_player = ((int)(Math.random())==1?p1.getName():p2.getName());
        
    }
    
    public boolean hasPlayerTwo() {
        return p2 != null;
    }
    public void setPlayerTwo(User u) {
        p2 = u;
        info.player_2_name = p2.getName();
        info.player_2_pts = 0;
    }
    public String getGid() {
        return gid;
    }
    public boolean isPlaying(String uid) {
        if (p2 == null) return p1.getUid().equals(uid);
        return p1.getUid().equals(uid)||p2.getUid().equals(uid);
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