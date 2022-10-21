package hexlet.code.controllers;

import io.javalin.http.Handler;
import io.javalin.http.NotFoundResponse;
import io.ebean.PagedList;

import java.net.MalformedURLException;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Collectors;
import java.net.URL;

import hexlet.code.domain.Url;
import hexlet.code.domain.query.QUrl;

public final class UrlController {

    public static Handler listUrls = ctx -> {
        String term = ctx.queryParamAsClass("term", String.class).getOrDefault("");
        int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1) - 1;
        int rowsPerPage = 10;

        PagedList<Url> pagedUrls = new QUrl()
                .name.icontains(term)
                .setFirstRow(page * rowsPerPage)
                .setMaxRows(rowsPerPage)
                .orderBy()
                .id.asc()
                .findPagedList();

        List<Url> urls = pagedUrls.getList();

        int lastPage = pagedUrls.getTotalPageCount() + 1;
        int currentPage = pagedUrls.getPageIndex() + 1;
        List<Integer> pages = IntStream
                .range(1, lastPage)
                .boxed()
                .collect(Collectors.toList());

        ctx.attribute("urls", urls);
        ctx.attribute("term", term);
        ctx.attribute("pages", pages);
        ctx.attribute("currentPage", currentPage);
        ctx.render("urls/index.html");
    };

    public static Handler createUrl = ctx -> {
        String name = ctx.formParam("name");
        try {
            URL urlHttp = new URL(name);
            String urlName = urlHttp.getProtocol() + "://" + urlHttp.getHost();
            if (urlHttp.getPort() != -1) {
                urlName = urlName + ":" + urlHttp.getPort();
            }
            List<Url> urls = new QUrl().findList();
            for (Url u: urls) {
                if (u.getName().equals(urlName)) {
                    ctx.sessionAttribute("flash", "Страница уже существует");
                    ctx.sessionAttribute("flash-type", "danger");
                    ctx.redirect("/");
                    return;
                }
            }
            Url url = new Url(urlName);
            url.save();
        } catch (MalformedURLException E) {
            ctx.sessionAttribute("flash", "Некорректный URL");
            ctx.sessionAttribute("flash-type", "danger");
            ctx.redirect("/");
            return;
        }
        ctx.sessionAttribute("flash", "Страница успешно добавлена");
        ctx.sessionAttribute("flash-type", "success");
        ctx.redirect("/urls");
    };

    public static Handler showUrl = ctx -> {
        int id = ctx.pathParamAsClass("id", Integer.class).getOrDefault(null);

        Url url = new QUrl()
                .id.equalTo(id)
                .findOne();

        if (url == null) {
            throw new NotFoundResponse();
        }

        ctx.attribute("url", url);
        ctx.render("urls/show.html");
    };
}