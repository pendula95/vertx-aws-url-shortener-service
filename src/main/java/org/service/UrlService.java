package org.service;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.log4j.Logger;
import org.dto.Url;
import org.util.Message;

import java.time.Instant;

public class UrlService extends AbstractVerticle {

    private static final Logger logger = Logger.getLogger(UrlService.class);
    private SQLClient client;
    private final UrlValidator urlValidator = new UrlValidator();

    @Override
    public void start() {
        final EventBus eventBus = vertx.eventBus();
        final JsonObject jdbcClientConfig = new JsonObject()
                .put("url", System.getenv("db_url"))
                .put("driver_class", System.getenv("db_driver"))
                .put("user", System.getenv("db_username"))
                .put("password", System.getenv("db_password"))
                .put("max_pool_size", 20);
        client = JDBCClient.createShared(vertx, jdbcClientConfig);

        eventBus.consumer("POST/api", message -> {
            Message replay = (Message) message.body();
            logger.debug("URL = " + replay.getUrl().getLongUrl());
            if(replay.getUrl().getLongUrl() == null || replay.getUrl().getLongUrl().isEmpty() || !urlValidator.isValid(replay.getUrl().getLongUrl())) {
                replay.setStatusCode(400);
                replay.setMessage("Longurl was not provided, empty or invalid format, failed to generate shorturl");
                message.reply(replay);
                return;
            }
            add(replay.getUrl(), done -> {
                if(done.succeeded() && done.result().getResults().size() == 1) {
                    replay.getUrl().setId(done.result().getResults().get(0).getLong(0));
                    replay.getUrl().setShortUrl(encode(done.result().getResults().get(0).getLong(0)));
                    replay.setStatusCode(200);
                    message.reply(replay);
                } else {
                    replay.setStatusCode(500);
                    message.reply(replay);
                }
            });
        });

        eventBus.consumer("GET/{shorturl}", message -> {
            Message replay = (Message) message.body();
            replay.getUrl().setShortUrl(replay.getPathParams());
            logger.debug("PP = " + replay.getPathParams());
            resolveShortUrl(replay.getUrl(), done ->{
                if(done.succeeded() && done.result().getResults().size() == 1) {
                    replay.getUrl().setLongUrl((done.result().getResults().get(0).getString(1)));
                    replay.setStatusCode(307);
                    message.reply(replay);
                } else {
                    replay.setStatusCode(404);
                    replay.setMessage("No record found for provided longurl");
                    message.reply(replay);
                }
            });
        });
    }

    private void add(Url url, Handler<AsyncResult<ResultSet>> done) {
        JsonArray params = new JsonArray().add(url.getLongUrl()).add(Instant.now());
        client.getConnection(conn -> {
            query(conn.result(), INSERT_NEW, params, done);
            conn.result().close(doneConn -> {
                if (doneConn.failed()) {
                    throw new RuntimeException(doneConn.cause());
                }
            });
        });
    }

    private void resolveShortUrl(Url url, Handler<AsyncResult<ResultSet>> done) {
        JsonArray params = new JsonArray().add(decode(url.getShortUrl()));
        client.getConnection(conn -> {
            query(conn.result(), RETRIVE_BY_SHORT_URL, params, done);
            conn.result().close(doneConn -> {
                if (doneConn.failed()) {
                    throw new RuntimeException(doneConn.cause());
                }
            });
        });
    }

    private void query(SQLConnection conn, String sql, JsonArray param,  Handler<AsyncResult<ResultSet>> done) {
        conn.queryWithParams(sql, param, res -> {
            if(res.failed()) {
                logger.error("Cause = " + res.cause().getCause() + " Message = " + res.cause().getMessage());
                throw new RuntimeException(res.cause());
            } else {
                done.handle(res);
            }
        });
    }

    private static String encode(long number) {
        long i = number;
        if (i == 0) {
            return Character.toString(CHARSET[0]);
        }

        StringBuilder stringBuilder = new StringBuilder();
        while (i > 0) {
            long remainder = i % ALPHABET_LENGTH;
            i /= ALPHABET_LENGTH;
            stringBuilder.append(CHARSET[(int) remainder]);
        }
        return stringBuilder.reverse().toString();
    }

    private static long decode(String s) {
        long i = 0;
        char[] chars = s.toCharArray();
        for (char c : chars) {
            i = i * ALPHABET_LENGTH + ALPHABET.indexOf(c);
        }
        return i;
    }

    private static final String SCHEMA = "public";
    private static final String TABLE= "url";
    private static final String INSERT_NEW = "INSERT INTO " + SCHEMA + "." + TABLE + " (longurl, created) VALUES (?,?) RETURNING id";
    private static final String RETRIVE_BY_SHORT_URL = "UPDATE " + SCHEMA + "." + TABLE + " SET count = count + 1 WHERE id = ? RETURNING *";

    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int ALPHABET_LENGTH = ALPHABET.length();
    private static final char[] CHARSET = ALPHABET.toCharArray();
}
