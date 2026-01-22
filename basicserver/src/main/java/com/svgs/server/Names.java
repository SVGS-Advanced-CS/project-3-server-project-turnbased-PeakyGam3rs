package com.svgs.server;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;

public class Names {
    private static final Connection conn = Helper.createConnection("names.db");

    public static void loadNames() {
        HashSet<String> names = new HashSet<>();
        try (BufferedReader br = new BufferedReader(new FileReader("./src/main/resources/list.txt"))) {
            br.lines().filter(s -> s.length() >= 3).forEach(names::add);
        } catch (Exception e) {
            System.out.println(e);
        }

        String update = "INSERT INTO names (name) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(update)) {
            conn.setAutoCommit(false);
            for (String name : names) {
                ps.setString(1, name);
                ps.executeUpdate();
            }

            conn.commit();
        } catch (SQLException e) {
            System.out.println(e);
        }
    }

    public static String getRandomName() {
        String query = "SELECT * FROM names ORDER BY RANDOM() LIMIT 1";
        try (Statement state = conn.createStatement()) {
            ResultSet rs = state.executeQuery(query);
            if (rs.next()) {
                return rs.getString("name");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Anthony Tyler";
    }
}
