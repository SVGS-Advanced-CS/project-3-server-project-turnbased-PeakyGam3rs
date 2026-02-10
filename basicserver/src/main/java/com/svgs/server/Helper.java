package com.svgs.server;

// big ugly class, no logic here
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import com.google.gson.Gson;
import com.svgs.game_model.Match;
import com.svgs.game_model.User;
import com.svgs.model.Category;
import com.svgs.model.Question;

public class Helper {
    private static final String charset = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final Connection jpconn = createConnection("JPRDY.db");
    private static final Gson gson = new Gson();
    private static final HashMap<Integer, String> cats = loadCats();
    private static int[] catIds;
    private static final HashMap<Integer, Integer> roundIdToCatIds = new HashMap<>();
    private static HashMap<Integer, String> loadCats() {
        String query = "SELECT c.* FROM categories c "
                + "JOIN questions q ON q.category_id = c.category_id "
                + "GROUP BY c.category_id "
                + "HAVING COUNT(*) = 5";
        HashMap<Integer, String> result = new HashMap<>();
        try (Statement state = jpconn.createStatement()) {
            ResultSet rs = state.executeQuery(query);
            while (rs.next()) {
                result.put(rs.getInt("category_id"), rs.getString("category_name"));
            }
        } catch (SQLException e) {
            System.out.println(e);
        }
        catIds = new int[result.size()];
        int i = 0;
        for (int id : result.keySet()) {
            catIds[i++] = id;
        }
        return result;
    }

    static Match newMatch(User one, String gid) throws Exception {
        Category[] cats = genCats();
        Match result = new Match(one, cats, gid);
        return result;
    }

    static String generateId(int length) {
        String result = "";
        for (int i = 0; i < length; i++) {
            Double dub = ThreadLocalRandom.current().nextDouble();
            result += charset.charAt((int) (dub * charset.length()));
        }
        return result;
    }

    // works for millis
    public static String formatTime(long unformatted) {
        long time = unformatted % (1000 * 60 * 60 * 24);
        long hours = time / (1000 * 60 * 60);
        time %= (1000 * 60 * 60);
        long minutes = time / (1000 * 60);
        time %= (1000 * 60);
        long seconds = time / 1000;
        time %= 1000;
        long hundredths = time / 10;
        return String.format("%02d:%02d:%02d.%02ds", hours, minutes, seconds, hundredths);
    }

    static Connection createConnection(String db) {
        Connection conn;
        String url = "jdbc:sqlite:./src/main/resources/" + db;
        try {
            conn = DriverManager.getConnection(url);
        } catch (SQLException e) {
            System.out.println(e);
            return null;
        }
        return conn;
    }

    static Category[] genCats() throws Exception {
        Category[] results = new Category[5];
        int[] ids = new int[5];
        int x = 0;
        while (x < 5) {
            int y = ThreadLocalRandom.current().nextInt(catIds.length);
            boolean isDupe = false;
            for (int z = 0; z < x; z++) {
                if (catIds[y] == ids[z]) {
                    isDupe = true;
                }
            }
            if (!isDupe) {
                ids[x] = catIds[y];
                x++;
            }
        }
        String query = "SELECT * FROM questions WHERE category_id=?";
        try (PreparedStatement ps = jpconn.prepareStatement(query)) {
            for (int i = 0; i < 5; i++) {
                int id = ids[i];
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Question> qs = new ArrayList<>();
                    while (rs.next()) {
                        String q = "";
                        String a = "";
                        int qid = -1;
                        q = rs.getString("question_text");
                        a = rs.getString("answer_text");
                        int value = -1;
                        qid = rs.getInt("question_id");
                        try {
                            value = Integer.parseInt(rs.getString("value"));
                        } catch (Exception e) {
                        }
                        Question ques = new Question(q, a, value, qid);
                        qs.add(ques);
                    }

                    for (Question q : qs) {
                        if (q.getValue() <= 0) {
                            return genCats();
                        }

                        results[i] = new Category(cats.get(id), qs.toArray(Question[]::new));
                    }
                }
            }
            return results;
        }
    }
}
