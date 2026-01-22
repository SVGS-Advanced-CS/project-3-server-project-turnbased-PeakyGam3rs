package com.svgs;

import com.google.gson.Gson;
import com.svgs.model.Category;
import com.svgs.server.Helper;
import com.svgs.server.Names;

import static spark.Spark.before;
import static spark.Spark.options;

public class Main {

    public static void main(String[] args) {
        Gson gson = new Gson();
        port(4567);
        get("api/initialize_user", "application/json", (req, res) -> gson.toJson(Manager.createUser()));
        

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