package com.svgs.db;
// a class for reading the db for app. use

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.svgs.dtos.Qu;
import com.svgs.model.Question;
import com.svgs.model.Round;

public class JDBReader {
    private Connection conn;

    public JDBReader() {
        String url = "jdbc:sqlite:./basicserver/src/main/resources/JPRDY.db";
        try {
            conn = DriverManager.getConnection(url);
            conn.createStatement().execute("PRAGMA foreign_keys = ON");
        } catch (SQLException e) {
            System.out.println(e);
        }
    }

    public JDBReader(Connection conn) {
        this.conn = conn;
    }

    public Connection getConnection() {
        return conn;
    }

    // TODO: handle images
    public Question[] findQSWithURLS() {
        List<Question> result = new ArrayList<>();
        String query = "SELECT * FROM questions WHERE question_text LIKE '%<a href=%'";
        try (Statement state = conn.createStatement()) {
            ResultSet rs = state.executeQuery(query);
            while (rs.next()) {
                int value = -1;
                String tmp = rs.getString("value").substring(1);
                tmp = tmp.replaceAll("\\$", ""); // just in case
                if (tmp.matches("\\d+")) value = Integer.parseInt(tmp);
                Question q = new Question(
                        rs.getString("question_text"),
                        rs.getString("answer_text"),
                        value,
                        rs.getInt("question_id"));
                result.add(q);
            }
        } catch (SQLException e) {
            System.out.println(e);
        }
        return result.toArray(Question[]::new);
    }

    public void printCatCount() {
        int catCount = getCats().size();
        System.out.printf("[JLOAD_INFO] categories in DB: %d%n", catCount);
    }

    public void printQCount() {
        try (java.sql.Statement st = conn.createStatement();
                java.sql.ResultSet rs = st
                        .executeQuery("SELECT COUNT(*) FROM questions")) {
            if (rs.next())
                System.out.printf("[JLOAD_INFO] questions in DB: %d%n", rs.getInt(1));
        } catch (Exception e) {
            System.out.println("[JREAD_WARN] Failed to print question count: " + e.getMessage());
        }
    }

    public Question getRandomQuestion() {
        String query = "SELECT * FROM questions ORDER BY RANDOM() LIMIT 1";
        try (Statement state = conn.createStatement()) {
            ResultSet rs = state.executeQuery(query);
            if (rs.next()) {
                return new Question(rs.getString("question_text"),
                        rs.getString("answer_text"),
                        Integer.parseInt(rs.getString("value").substring(1)),
                        rs.getInt("question_id"));
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return dummyQuestion();
    }

    public Question getQuestion(int question_id) {
        String query = "SELECT * FROM questions WHERE question_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, question_id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String question_text = rs.getString("question_text");
                String answer_text = rs.getString("answer_text");
                int value = Integer.parseInt(rs.getString("value").substring(1));
                return new Question(question_text, answer_text, value, question_id);
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return dummyQuestion();
    }

    public void markCategories() {
        Set<Integer> allCIDS = getCats().keySet();
        Set<Integer> completeCIDS = getCompleteCIDS();
        String update = "UPDATE categories SET is_complete = ? WHERE category_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(update)) {
            for (int cid : allCIDS) {
                String complete = "false";
                if (completeCIDS.contains(cid)) {
                    complete = "true";
                }
                ps.setString(1, complete);
                ps.setInt(2, cid);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println(e);
        }
    }

    Set<Integer> getCompleteCIDS() {
        HashMap<Integer, String> map = getCats();

        Set<Integer> result = new HashSet<>();
        // Use aggregation to count questions per category in a single pass
        result.clear(); // Clear previous results
        String newQuery = "SELECT category_id, COUNT(*) as count FROM questions GROUP BY category_id";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(newQuery)) {
            while (rs.next()) {
                int cid = rs.getInt("category_id");
                int cnt = rs.getInt("count");
                result.add(cid);
            }
        } catch (SQLException e) {
            System.out.println(e);
        }
        return result;
    }

    Set<Integer> getSIDS() {
        Set<Integer> result = new HashSet<>();
        String query = "SELECT show_id FROM shows";
        try (Statement state = conn.createStatement()) {
            ResultSet rs = state.executeQuery(query);
            while (rs.next()) {
                result.add((Integer) rs.getInt(1));
            }
        } catch (SQLException e) {
            System.out.println(e);
        }
        return result;
    }

    List<Round> getRounds() {
        ArrayList<Round> result = new ArrayList<>();
        try {
            String query = "SELECT * FROM rounds";
            Statement state = conn.createStatement();
            ResultSet rs = state.executeQuery(query);
            while (rs.next()) {
                int show_id = rs.getInt("show_id");
                String round_name = rs.getString("round_name");
                int round_id = rs.getInt("round_id");
                result.add(new Round(round_id, show_id, round_name));
            }
        } catch (SQLException e) {
            System.out.println("[JDBW_FATAL] SQLException in JDBWriter.getRounds:");
            System.out.println("Exception: " + e.getMessage());
            System.exit(-1);
        }

        return result;

    }

    HashMap<String, Integer> getCNTIDMap() {
        HashMap<String, Integer> result = new HashMap<>();
        String query = "SELECT * FROM categories";
        try (Statement state = conn.createStatement()) {
            ResultSet rs = state.executeQuery(query);
            while (rs.next()) {
                result.put(rs.getString("category_name"), (Integer) rs.getInt("category_id"));
            }
        } catch (Exception e) {
            System.out.println(e);
            return new HashMap<>();
        }
        return result;
    }

    private HashMap<Integer, Integer> ridsidMap;

    HashMap<Integer, HashMap<String, Integer>> getRoundMap() throws SQLException {
        HashMap<Integer, HashMap<String, Integer>> map = new HashMap<>();
        try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery("SELECT show_id, round_name, round_id FROM rounds")) {
            while (rs.next()) {
                int sid = rs.getInt("show_id");
                String rn = rs.getString("round_name");
                int rid = rs.getInt("round_id");
                map.computeIfAbsent(sid, k -> new HashMap<>()).put(rn, rid);
            }
        }
        return map;
    }

    List<Qu> getQuestions() {
        List<Qu> result = new ArrayList<>();
        HashMap<String, Integer> cntidMap = getCNTIDMap();
        String query = "SELECT * FROM questions";
        try (Statement state = conn.createStatement()) {
            ResultSet rs = state.executeQuery(query);
            while (rs.next()) {
                Qu q = new Qu(
                        rs.getString("question_text"),
                        rs.getString("answer_text"),
                        rs.getString("value"),
                        rs.getInt("round_id"),
                        rs.getInt("question_id"));
                Integer cid = cntidMap.getOrDefault(q.category, -1);
                q.setCID(cid);
                result.add(q);

            }
        } catch (SQLException e) {
            System.out.println("[JDBR_FATAL] Error when at JDBReader.getQuestions:");
            System.out.println("\tException - " + e.getMessage());
            System.exit(-1);
        }
        return result;
    }

    // returns a hashmap with category_id as key and category_name as value
    HashMap<Integer, String> getCats() {
        HashMap<Integer, String> map = new HashMap<>();
        String query = "SELECT * FROM categories";
        int attempts = 0;
        while (attempts < 5) {
            try (Statement state = conn.createStatement(); ResultSet rs = state.executeQuery(query)) {
                while (rs.next()) {
                    int id = rs.getInt("category_id");
                    String cn = rs.getString("category_name");
                    map.put(id, cn);
                }
                return map;
            } catch (SQLException e) {
                attempts++;
                if (e.getMessage() != null && e.getMessage().toLowerCase().contains("locked") && attempts < 5) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    continue;
                }
                System.out.println(String.format("[JDBW_FATAL] Failed call in JrpdyCTRL.getCats:\n\tException - " + e));
                System.exit(-1);
            }
        }
        return map;
    }

    private Question dummyQuestion() {
        return new Question("Method without a good result?", "This", 666, -1);
    }

}