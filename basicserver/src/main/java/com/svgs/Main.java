package com.svgs;

import com.google.gson.Gson;
import com.svgs.game_model.User;
import com.svgs.server.Manager;

import static spark.Spark.before;
import static spark.Spark.get;
import static spark.Spark.options;
import static spark.Spark.port;
import static spark.Spark.post;

public class Main {

    public static void main(String[] args) {
        Gson gson = new Gson();
        port(4567);
        before((req, res) -> res.type("application/json"));
        get("api/initialize_user", "application/json", (req, res) -> gson.toJson(Manager.createUser()));
        //post("api/create_game", "application/json", (req, res) -> Manager.createGame(req.body())); 
        post("api/create_game", "application/json", (req, res) -> {
            res.type("application/json");
            String body = req.body();
            String gid = Manager.createGame(body);
            System.out.println(" gid: " + gid);
            record gam(String gid){}
            return gson.toJson(new gam(gid), gam.class);
        });
        post("/api/join_game", "application/json", (req,res) -> {
            record Tmp(String uid, String gid) {};
            Tmp tmp = gson.fromJson(req.body(), Tmp.class);
            return Manager.joinGame(tmp.uid, tmp.gid);
        });


        User user = Manager.createUser();
        System.out.println(Manager.createGame(gson.toJson(user, User.class)));

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