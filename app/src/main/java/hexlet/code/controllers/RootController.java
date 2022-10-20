package hexlet.code.controllers;

import io.javalin.http.Handler;

import java.io.PrintWriter;

public class RootController {
    public static Handler welcome = ctx -> {
        ctx.render("index.html");
    };

    public static Handler about = ctx -> {
        ctx.render("about.html");
    };
}
