package com.svgs;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.svgs.req_dtos.AnswerQuestionRequest;
import com.svgs.req_dtos.CreateGameRequest;
import com.svgs.req_dtos.GameInfoRequest;
import com.svgs.req_dtos.JoinGameRequest;
import com.svgs.req_dtos.SelectQuestionRequest;
import com.svgs.server.ApiException;
import com.svgs.server.BadRequest;
import com.svgs.server.Manager;

import spark.Request;
import spark.Response;
import spark.Route;
import static spark.Spark.afterAfter;
import static spark.Spark.before;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.options;
import static spark.Spark.port;
import static spark.Spark.post;

// handles inbound and outbound serialization
public class Main {
    private static Gson gson = new Gson();
    private static final Logger API = LoggerFactory.getLogger("api");

    public static void main(String[] args) {
        API.info("INFO works");
        API.warn("WARN works");
        API.error("ERROR works");
        port(4567);
        disableCORS();
        before((req, res) -> res.type("application/json"));

        record ErrorResponse(String code, String message, String requestId) {
        }

        before((req, res) -> req.attribute("t0", System.nanoTime()));

        afterAfter((req, res) -> {
            Long t0 = req.attribute("t0");
            if (t0 != null) {
                long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
                res.header("X-Server-Time-ms", Long.toString(ms));
            }
        });

        // wanted to stamp em, found this online as nice solution
        before((req, res) -> {
            String rid = java.util.UUID.randomUUID().toString();
            req.attribute("rid", rid);
            res.header("X-Request-Id", rid);
        });

        // realized inheritance was crazy fire, and how to get spark to not just void
        // exceptions
        exception(ApiException.class, (e, req, res) -> {
            String rid = req.attribute("rid");
            API.info("rid={} {} {} -> {} {}", rid, req.requestMethod(), req.pathInfo(), e.status, e.code);
            res.type("application/json");
            res.status(e.status);
            res.body(gson.toJson(new ErrorResponse(e.code, e.safeMessage, rid)));
        });

        // api error so common it got its own special case
        exception(JsonSyntaxException.class, (e, req, res) -> {
            String rid = req.attribute("rid");
            API.info("rid={} malformed json", rid);
            res.type("application/json");
            res.status(400);
            res.body(gson.toJson(new ErrorResponse("MALFORMED_JSON", "invalid json", rid)));
        });

        // code bugs, not bc api input
        exception(Exception.class, (e, req, res) -> {
            String rid = req.attribute("rid");
            API.error("rid={} {} {} -> 500 INTERNAL_ERROR", rid, req.requestMethod(), req.pathInfo(), e);
            res.type("application/json");
            res.status(500);
            res.body(gson.toJson(new ErrorResponse("INTERNAL_ERROR", "ssshhhh, u didnt see this", rid)));
        });

        get("/api/initialize_user", "application/json", (req, res) -> {
            return gson.toJson(Manager.createUser());
        });

        post("/api/create_game", "application/json", new Route() {
            @Override
            public Object handle(Request req, Response res) throws Exception {
                String body = req.body();
                CreateGameRequest crg = gson.fromJson(body, CreateGameRequest.class);
                
                return gson.toJson(Manager.createGame(crg));
            }
        });

        post("/api/join_game", "application/json", (req, res) -> {
            String body = req.body();
            JoinGameRequest jgr = gson.fromJson(body, JoinGameRequest.class);
            return gson.toJson(Manager.joinGame(jgr));
        });

        get("/api/game_info", "application/json", (req, res) -> {
            res.type("application/json");
            String gid = req.queryParamOrDefault("gid", null);
            GameInfoRequest gir = new GameInfoRequest(gid);
            return Manager.fetchGameInfo(gir);
        });

        post("/api/select_question", "application/json", (req, res) -> {
            String body = req.body();
            SelectQuestionRequest sqr = gson.fromJson(body, SelectQuestionRequest.class);
            return gson.toJson(Manager.selectQuestion(sqr));
        });

        post("/api/answer_question", "application/json", (req,res) -> {
            String body = req.body();
            API.info("body: {}", body);
            AnswerQuestionRequest aqr = gson.fromJson(body, AnswerQuestionRequest.class);
            return gson.toJson(Manager.answerQuestion(aqr));
        });

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