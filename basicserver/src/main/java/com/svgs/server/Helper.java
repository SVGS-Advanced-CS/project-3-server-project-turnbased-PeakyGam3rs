package com.svgs.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ThreadLocalRandom;

public class Helper {
    private static final String charset = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    static String generateId(int length) {
        String result = "";
        for (int i = 0; i < length; i++) {
            Double dub = ThreadLocalRandom.current().nextDouble();
            result += charset.charAt((int)(dub*charset.length()));
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
}
