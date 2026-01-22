package com.svgs;

import com.svgs.model.Category;
import com.svgs.server.Names;

import static spark.Spark.before;
import static spark.Spark.options;

public class Main {

    public static void main(String[] args) {
        //disableCORS();
        Category[] cats = com.svgs.api.JAPI.getGame();
        for (Category cat : cats) {
            cat.print();
        }
        Names.loadNames();
        

    }

    public static void disableCORS() {
        before((req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            res.header("Access-Control-Allow-Headers", "Content-Type, Authorization");
        });

        options("/*", (req, res) -> {
            String accessControlRequestHeaders = req.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                res.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }

            String accessControlRequestMethod = req.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                res.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }

            return "OK";
        });
    }
}