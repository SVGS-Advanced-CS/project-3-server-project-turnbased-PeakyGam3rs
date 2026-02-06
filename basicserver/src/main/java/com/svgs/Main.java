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
    private static Gson gson = new Gson();
    public static void main(String[] args) {
        port(4567);
        before((req, res) -> res.type("application/json"));
        get("api/initialize_user", "application/json", (req, res) -> gson.toJson(Manager.createUser()));
        post("api/create_game", "application/json", (req, res) -> {
            res.type("application/json");
            String body = req.body();
            String gid = Manager.createGame(body);
            record gam(String gid){}
            return gson.toJson(new gam(gid), gam.class);
        });
        post("/api/join_game", "application/json", (req,res) -> {
            res.type("application/json");
            return Manager.joinGame(req.body());
        });
        get("/api/game_info", "application/json", (req, res) -> {
            res.type("application/json");
            String gid = req.queryParamOrDefault("gid", null);
            if (gid == null) {
                res.status(400);
                return genericError(String.format("missing query parameter \"%s\"",gid));
            }
            return Manager.fetchGameInfo(gid);
        });
        post("/api/select_question", "application/json", (req,res) -> {
            record Input(String uid, String gid, int question_index) {}
            Input input;
            try {
                input = gson.fromJson(req.body(), Input.class);
            } catch (Exception e) {
                res.status(400);
                return genericError("failed to parse input json");
            }

            String gid = input.gid;
            String uid = input.uid;

            int question_index = input.question_index;
            if (gid.equals("")) {
                res.status(400);
                return genericError("input gid is empty");
            }
            if (uid.equals("")) {
                res.status(400);
                return genericError("input uid is empty");
            }
            if (question_index < 0 || question_index > 24) {
                res.status(400);
                return genericError(String.format("question index %d is out of range [0, 24]", question_index));
            }
            // no status code here for errors, srry
            return Manager.selectQuestion(uid, gid, question_index);
        });

        User user = Manager.createUser();
        System.out.println(Manager.createGame(gson.toJson(user, User.class)));


    }

    static String genericError(String message) {
        record Error(String message) {}
        return gson.toJson(new Error(message));
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