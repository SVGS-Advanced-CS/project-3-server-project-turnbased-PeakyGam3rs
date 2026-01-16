package com.svgs.api;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;

import com.svgs.db.JDBReader;
import com.svgs.model.Category;

// a class to make calling stuff easy
public class JAPI {
    private static JDBReader jdbr = new JDBReader();
    private static Connection conn;

    public static Category[] getGame() {
        conn = jdbr.getConnection();
        String query = "SELECT c.* FROM categories c "
        + "JOIN questions q ON q.category_id = c.category_id "
        + "GROUP BY c.category_id "
        + "HAVING COUNT(*) = 5";
        HashSet<Integer> catIds = new HashSet<>();
        try (Statement state = conn.createStatement()) {
            ResultSet rs = state.executeQuery(query);
            while (rs.next()) {
                catIds.add(rs.getInt("category_id"));
            }
        } catch (SQLException e) {
            System.out.println(e);
        }
        System.out.println(catIds.size());
        return new Category[0];
    }
}
