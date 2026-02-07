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

    public static void loadNames() {
        try (Connection conn = Helper.createConnection("names.db")) {
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
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static String getRandomName() throws Exception {
        String query = "SELECT name FROM names WHERE id = 1 + (ABS(RANDOM()) % (SELECT MAX(id) from names))";
        // retry for super rare missing gaps
        for (int retry = 0; retry < 3; retry++) {
            try (Connection conn = Helper.createConnection("names.db");
                    Statement state = conn.createStatement();
                    ResultSet rs = state.executeQuery(query)) {
                if (rs.next()) {
                    return rs.getString("name");
                }
            }
        }
        // if this exception is thrown, either buy a lottery ticket or fix the broken db
        throw new RuntimeException("failed to get random name");
    }
}
