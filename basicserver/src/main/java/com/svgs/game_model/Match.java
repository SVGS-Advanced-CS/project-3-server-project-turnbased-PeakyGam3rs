package com.svgs.game_model;

import java.util.HashMap;

import org.apache.commons.text.similarity.JaroWinklerSimilarity;

import com.svgs.model.Category;
import com.svgs.model.Question;
import com.svgs.server.BadRequest;

// more abstracted class for internal management
public class Match {
    private final GameInfo info; // basically a ledger
    private static final JaroWinklerSimilarity JWS = new JaroWinklerSimilarity();
    private long startTime;


    public Match(User one, Category[] cats, String gid_in) throws Exception {
        startTime = System.currentTimeMillis();
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
        info = new GameInfo(this, questionMap, kittens, one, gid, startTime);
        info.game_over = "false";
        info.player_1_pts = 0;
        info.player_2_pts = 0;
        info.player_1_name = one.getName();
        info.current_stage = "waiting";
        info.logGenericEvent(String.format("%s created game %s", info.p1.getName(), gid));
    }
    
    // request is already completely vetted, now update game_state
    // question with active index is loaded properly when calling report(), no need to set it.
    public void selectQuestion(int question_index) {
        info.selectQuestion(question_index); // seems jank, but i only want to deal with ONE object in Manager
    }

    public void answer(int question_index, boolean is_correct, User nerd, String inputRaw) {
        Question q = info.questionMap.get(question_index);
        int points_change = q.getValue();
        if (!is_correct) {
            points_change *= -1;
        }
        boolean isPlayerOne = info.player_1_name.equals(nerd.getName());

        // update gamestate. phase -> select, player -> not who just answered, update player points
        info.current_stage = "select";
        if (isPlayerOne) {
            info.player_1_pts += points_change;
            info.active_player = info.player_2_name;
        } else {
            info.player_2_pts += points_change;
            info.active_player = info.player_1_name;
        }

        // log answer
        info.event_log.add(new AnswerEvent(timeStamp(), question_index, inputRaw, nerd, q, is_correct));
    }

    public boolean isCorrect(int question_index, String inputRaw) {
        String answerRaw = info.questionMap.get(question_index).getAnswer();
        String a1 = normalizeAnswer(inputRaw);
        String a2 = normalizeAnswer(answerRaw);

        if (a2.length() <= 5) {
            return a1.equals(a2);
        }

        // found this on google, and i already had apache commmons text in my pom lol
        double score = JWS.apply(a1, a2);

        return score >= 0.90;
    }

    static String normalizeAnswer(String input) {
        String result = input;
        result = result.toLowerCase();
        result = result.trim();
        result = result.replaceAll("[^a-z0-9\\s]", "");
        result = result.replaceAll("^(what is|who is|where is|where are|what are|who are)\\s+", "");
        result = result.replaceAll("\\s+", " ");
        result = result.replaceAll("^(the|a|an)\\s+", "");
        return result;
    }

    public boolean userCanAnswer(User u) {
        return info.current_stage.equals("answer")&&info.active_player.equals(u.getName());
    }

    public boolean questionIsAnswered(int question_index) {
        return getGameQuestion(question_index).is_answered;
    }
    public GameQuestion getGameQuestion(int question_index) {
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

    public boolean isActivePlayer(User u) {
        return this.info.active_player.equals(u.getName());
    }

    public boolean canSelect() {
        return info.current_stage.equals("select");
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

    private String timeStamp() {
        return formatTime(System.currentTimeMillis() - startTime);
    }
    private String formatTime(long ms) {
        long time = ms%(1000*60*60*24);
        long hours = ms/(1000*60*60);
        time %= (1000*60*60);
        long minutes = time/(1000*60);
        time %= (1000*60);
        long seconds = time/(1000);
        time %= 1000;
        long hundredths = time/10;
        return String.format("%02d:%02d:%02d.%02ds", hours, minutes, seconds, hundredths);
    }
}