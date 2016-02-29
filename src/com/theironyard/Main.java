package com.theironyard;

import com.sun.org.apache.xpath.internal.operations.Mod;
import org.h2.command.Prepared;
import spark.ModelAndView;
import spark.Session;
import spark.Spark;
import spark.template.mustache.MustacheTemplateEngine;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class Main {

    static HashMap<String, User> users = new HashMap<>();

    static void insertGame(Connection conn, Game game) throws SQLException {
        PreparedStatement stmt2 = conn.prepareStatement("INSERT INTO games VALUES (NULL, ?, ?, ?, ?)");
        stmt2.setString(1, game.name);
        stmt2.setString(2, game.genre);
        stmt2.setString(3, game.platform);
        stmt2.setInt(4, game.releaseYear);
        stmt2.execute();
    }

    static ArrayList<Game> selectGames(Connection conn) throws SQLException {
        ArrayList<Game> results = new ArrayList<>();
        Statement stmt2 = conn.createStatement();
        ResultSet resultSet = stmt2.executeQuery("SELECT * FROM games");
        while (resultSet.next()) {
            String name = resultSet.getString("name");
            String genre = resultSet.getString("genre");
            String platform = resultSet.getString("platform");
            int releaseYear = resultSet.getInt("releaseYear");
            int id = resultSet.getInt("id");
            Game game = new Game(name, genre, platform, releaseYear, id);

            results.add(game);
        }
        return results;
    }

    static void deleteGame(Connection conn, int id) throws SQLException {
        PreparedStatement stmt2 = conn.prepareStatement("DELETE FROM games WHERE id = ?");
        stmt2.setInt(1, id);
        stmt2.execute();
    }

    static void updateGame(Connection conn, Game game) throws SQLException {
        PreparedStatement stmt2 = conn.prepareStatement("UPDATE games SET name = ?, genre = ?, platform = ?, releaseYear = ? WHERE id = ?");
        stmt2.setString(1, game.name);
        stmt2.setString(2, game.genre);
        stmt2.setString(3, game.platform);
        stmt2.setInt(4, game.releaseYear);
        stmt2.setInt(5, game.id);
        stmt2.execute();
    }


    public static void main(String[] args) throws SQLException {

        Connection conn = DriverManager.getConnection("jdbc:h2:./main");

        Statement stmt = conn.createStatement();


        stmt.execute("CREATE TABLE IF NOT EXISTS games(id IDENTITY, name VARCHAR, genre VARCHAR, platform VARCHAR, releaseYear INT)");



        Spark.externalStaticFileLocation("public");
        Spark.init();
        Spark.get(
                "/",
                ((request, response) -> {
                    User user = getUserFromSession(request.session());
                    ArrayList<Game> games = selectGames(conn);

                    HashMap m = new HashMap<>();
                    if (user == null) {
                        return new ModelAndView(m, "login.html");
                    } else if (request.session().attribute("id") != null) {
                        for (int i = 0; i < games.size(); i++) {
                            if (request.session().attribute("id").equals(games.get(i).id)) {
                                request.session().removeAttribute("id");
                                m.put("game", games.get(i));
                            }
                        }
                        return new ModelAndView(m, "edit.html");
                    } else {
                        m.put("games", games);
                        return new ModelAndView(m, "home.html");
                    }
                }),
                new MustacheTemplateEngine()
        );
        Spark.post(
                "/create-user",
                ((request, response) -> {
                    String name = request.queryParams("loginName");
                    if (name == null) {
                        throw new Exception("Login name is null.");
                    }

                    User user = users.get(name);
                    if (user == null) {
                        user = new User(name);
                        users.put(name, user);
                    }

                    Session session = request.session();
                    session.attribute("userName", name);

                    response.redirect("/");
                    return "";
                })
        );
        Spark.post(
                "/create-game",
                ((request, response) -> {
                    User user = getUserFromSession(request.session());
                    if (user == null) {
                        //throw new Exception("User is not logged in");
                        Spark.halt(403);
                    }

                    String gameName = request.queryParams("gameName");
                    String gameGenre = request.queryParams("gameGenre");
                    String gamePlatform = request.queryParams("gamePlatform");
                    int gameYear = Integer.valueOf(request.queryParams("gameYear"));
                    if (gameName == null || gameGenre == null || gamePlatform == null) {
                        throw new Exception("Didn't receive all query parameters.");
                    }
                    Game game = new Game(gameName, gameGenre, gamePlatform, gameYear, 1);

                    insertGame(conn, game);

                    response.redirect("/");
                    return "";
                })
        );
        Spark.post(
                "/delete-entry",
                ((request, response) -> {
                    User user = getUserFromSession(request.session());
                    int id = Integer.valueOf(request.queryParams("id"));
                    deleteGame(conn, id);
                    response.redirect("/");
                    return "";
                })
        );
        Spark.get(
                "/edit-entry",
                ((request, response) -> {
                    HashMap m = new HashMap();
                    int id = Integer.valueOf(request.queryParams("id"));
                    m.put("id", id);
                    return new ModelAndView(m, "edit.html");
                }),
                new MustacheTemplateEngine()
        );
        Spark.post(
                "/logout",
                ((request, response) -> {
                    Session session = request.session();
                    session.invalidate();
                    response.redirect("/");
                    return "";
                })
        );
        Spark.post(
                "/perform-update",
                ((request, response) -> {
                    String gameName = request.queryParams("gameName");
                    String gameGenre = request.queryParams("gameGenre");
                    String gamePlatform = request.queryParams("gamePlatform");
                    int gameYear = Integer.valueOf(request.queryParams("gameYear"));
                    int id = Integer.valueOf(request.queryParams("id"));
                    Game game = new Game(gameName, gameGenre, gamePlatform, gameYear, id);
                    updateGame(conn, game);
                    response.redirect("/");
                    return "";
                })
        );
    }

    static User getUserFromSession(Session session) {
        String name = session.attribute("userName");
        return users.get(name);
    }

}
