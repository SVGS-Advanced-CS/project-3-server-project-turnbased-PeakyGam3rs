package com.svgs;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.svgs.server.ApiException;
import com.svgs.server.BadRequest;
import com.svgs.server.Manager;

import static spark.Spark.afterAfter;
import static spark.Spark.before;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.options;
import static spark.Spark.port;
import static spark.Spark.post;

public class Main {
    private static Gson gson = new Gson();
    private static final Logger API = LoggerFactory.getLogger("api");

    public static void main(String[] args) {
        API.info("INFO works");
        API.warn("WARN works");
        API.error("ERROR works");
        port(4567);
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

        // code bugs, not bc api input
        exception(Exception.class, (e, req, res) -> {
            String rid = req.attribute("rid");
            API.error("rid={} {} {} -> 500 INTERNAL_ERROR", rid, req.requestMethod(), req.pathInfo(), e);
            res.type("application/json");
            res.status(500);
            res.body(gson.toJson(new ErrorResponse("INTERNAL_ERROR", "ssshhhh, u didnt see this", rid)));
        });

        // api error so common it got its own special case
        exception(JsonSyntaxException.class, (e, req, res) -> {
            String rid = req.attribute("rid");
            API.info("rid={} malformed json", rid);
            res.type("application/json");
            res.status(400);
            res.body(gson.toJson(new ErrorResponse("MALFORMED_JSON", "invalid json", rid)));
        });

        get("/api/initialize_user", "application/json", (req, res) -> {
            return gson.toJson(Manager.createUser());
        });

        post("/api/create_game", "application/json", (req, res) -> {
            res.type("application/json");
            String body = req.body();
            String gid = Manager.createGame(body);
            record gam(String gid) {
            }
            return gson.toJson(new gam(gid), gam.class);
        });

        post("/api/join_game", "application/json", (req, res) -> {
            res.type("application/json");
            return Manager.joinGame(req.body());
        });

        get("/api/game_info", "application/json", (req, res) -> {
            res.type("application/json");
            String gid = req.queryParamOrDefault("gid", null);
            if (gid == null) {
                throw new BadRequest("MISSING_QUERY_PARAM", "missing query parameter gid");
            }
            if (gid.length() != 4) {
                throw new BadRequest("INVALID_GID", "gid does not match schema");
            }
            return Manager.fetchGameInfo(gid);
        });

        post("/api/select_question", "application/json", (req, res) -> {
            return Manager.selectQuestion(req.body());
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