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

import com.svgs.model.Category;
import com.svgs.model.Question;

public class Helper {
    private static final String charset = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final Connection jpconn = createConnection("JPRDY.db");

    static String generateId(int length) {
        String result = "";
        for (int i = 0; i < length; i++) {
            Double dub = ThreadLocalRandom.current().nextDouble();
            result += charset.charAt((int) (dub * charset.length()));
        }
        return result;
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

    static Category[] initializeGame() {
        String query = "SELECT c.* FROM categories c "
                + "JOIN questions q ON q.category_id = c.category_id "
                + "GROUP BY c.category_id "
                + "HAVING COUNT(*) = 5";
        HashMap<Integer, String> cats = new HashMap<>();
        try (Statement state = jpconn.createStatement()) {
            ResultSet rs = state.executeQuery(query);
            while (rs.next()) {
                cats.put(rs.getInt("category_id"), rs.getString("category_name"));
            }
            rs.close();
        } catch (SQLException e) {
            System.out.println(e);
        }
        Category[] results = new Category[5];
        query = "SELECT * FROM questions WHERE category_id=?";
        List<Integer> catIds = new ArrayList<>();
        catIds.addAll(cats.keySet());
        try (PreparedStatement ps = jpconn.prepareStatement(query)) {
            for (int i = 0; i < 5; i++) {
                int id = catIds.remove((int) (Math.random() * catIds.size()));
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
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
                rs.close();
                try {
                    for (Question q : qs) {
                        if (q.getValue() == -1) {
                            throw new Exception("nah");
                        }
                    }
                } catch (Exception e) {
                    i--;
                    continue;
                }
                results[i] = new Category(cats.get(id), qs.toArray(Question[]::new));
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return results;
    }
}
