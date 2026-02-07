package com.svgs.game_model;

import java.util.HashMap;

import com.google.gson.Gson;
import com.svgs.model.Category;
import com.svgs.model.Question;
import com.svgs.server.BadRequest;

// more abstracted class for internal management
public class Match {
    private GameInfo info; // basically a ledger
    private Gson gson = new Gson();

    public Match(User one, Category[] cats, String gid_in) throws Exception {
        HashMap<Integer, Question> questionMap = new HashMap<>();
        String gid = gid_in;
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
        info = new GameInfo(this, questionMap, kittens, one, gid);
        info.game_over = "false";
        info.player_1_pts = 0;
        info.player_2_pts = 0;
        info.player_1_name = one.getName();
        info.current_stage = "waiting";
        info.logGenericEvent(String.format("%s created game %s", info.p1.getName(), gid));
    }
    
    // request is already completely vetted, now update game_state
    // question with active index is loaded properly when calling report(), no need to set it.
    public String selectQuestion(int question_index) throws Exception {
        info.selectQuestion(question_index); // seems jank, but i only want to deal with ONE object as management
        record Result(String question_text) {}
        return gson.toJson(new Result(info.getQuestion(question_index).getQuestion()));
    }

    public GameQuestion getGameQuestion(int question_index) throws Exception {
        return info.fetchGameQuestion(question_index);
    }

    public Question getQuestion(int question_index) throws Exception {
        return info.getQuestion(question_index);
    }
    public boolean hasPlayerTwo() {
        return info.p2 != null;
    }
    public boolean isGameOver() {
        return info.game_over.equals("true");
    }
    public void setPlayerTwo(User u) throws Exception {
        info.p2 = u;
        if (u == null) {
            throw new BadRequest("MISSING_USER", "uid is required");
        }
        info.current_stage = "select";
        info.player_2_name = info.p2.getName();
        info.player_2_pts = 0;
        info.active_player = ((int) (Math.random()*2) == 1 ? info.p1.getName() : info.p2.getName());
        info.logGenericEvent(String.format("%s joined the game",info.p2.getName()));
    }
    public String getGid() {
        return info.gid;
    }

    public boolean isAnswered(int question_index) throws Exception {
        return getGameQuestion(question_index).is_answered;
    }

    public boolean isPlaying(String uid) {
        if (info.current_stage.equals("waiting")) return info.p1.getUid().equals(uid);
        return info.p1.getUid().equals(uid)||info.p2.getUid().equals(uid);
    }

    public User getActivePlayer() throws Exception {
        return info.translateName(info.active_player);
    }

    public String getStage() {
        return info.current_stage;
    }

    public GameInfo getGameInfo() throws Exception {
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