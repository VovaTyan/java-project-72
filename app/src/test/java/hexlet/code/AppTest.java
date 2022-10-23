package hexlet.code;

import hexlet.code.domain.Url;
import hexlet.code.domain.UrlCheck;
import hexlet.code.domain.query.QUrl;
import io.ebean.DB;
import io.ebean.Database;
import io.javalin.Javalin;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
class AppTest {

    @Test
    void testInit() {
        assertThat(true).isEqualTo(true);
    }

    private static Javalin app;
    private static String baseUrl;
    private static Database database;

    @BeforeAll
    public static void beforeAll() {
        app = App.getApp();
        app.start(0);
        int port = app.port();
        baseUrl = "http://localhost:" + port;
        database = DB.getDefault();
    }

    @AfterAll
    public static void afterAll() {
        app.stop();
    }

    @BeforeEach
    void beforeEach() {
       // database.script().run("/truncate.sql");
        database.script().run("/seed-test-db.sql");
    }

    @Nested
    class RootTest {

        @Test
        void testIndex() {
            HttpResponse<String> response = Unirest.get(baseUrl).asString();
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getBody()).contains("Анализатор страниц");
        }

        @Test
        void testAbout() {
            HttpResponse<String> response = Unirest.get(baseUrl + "/about").asString();
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getBody()).contains("Приложения для SEO анализа сайтов");
        }
    }

    @Nested
    class UrlTest {

        @Test
        void testIndex() {
            HttpResponse<String> response = Unirest
                .get(baseUrl + "/urls")
                .asString();
            String body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(body).contains("https://www.example1.com");
            assertThat(body).contains("https://www.example2.com");
        }

        @Test
        void testShow() {
            HttpResponse<String> response = Unirest
                .get(baseUrl + "/urls/1")
                .asString();
            String body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(body).contains("https://www.example1.com");
        }
        @Test
        void testCreate() {
            String inputName = "https://www.example.com";
            HttpResponse<String> responsePost = Unirest
                .post(baseUrl + "/urls")
                .field("name", inputName)
                .asEmpty();

            assertThat(responsePost.getStatus()).isEqualTo(302);
            assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/urls");

            HttpResponse<String> response = Unirest
                .get(baseUrl + "/urls")
                .asString();
            String body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(body).contains(inputName);
            assertThat(body).contains("Страница успешно добавлена");

            Url actualUrl = new QUrl()
                .name.equalTo(inputName)
                .findOne();

            assertThat(actualUrl).isNotNull();
            assertThat(actualUrl.getName()).isEqualTo(inputName);
        }
        @Test
        void testCreateNotCorrect() {
            String inputName = "www.rex";
            HttpResponse<String> responsePost = Unirest
                    .post(baseUrl + "/urls")
                    .field("name", inputName)
                    .asEmpty();

            assertThat(responsePost.getStatus()).isEqualTo(302);
            assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/");

            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/")
                    .asString();
            String body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(body).contains("Некорректный URL");

            Url actualUrl = new QUrl()
                    .name.equalTo(inputName)
                    .findOne();

            assertThat(actualUrl).isNull();
        }

        @Test
        void testRepeatCreate() {
            String inputName = "https://www.example3.com";
            Url url = new Url(inputName);
            url.save();
            HttpResponse<String> responsePost = Unirest
                    .post(baseUrl + "/urls")
                    .field("name", inputName)
                    .asEmpty();

            assertThat(responsePost.getStatus()).isEqualTo(302);
            assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/urls");

            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/")
                    .asString();
            String body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(body).contains("Страница уже существует");

            Url actualUrl = new QUrl()
                    .name.equalTo(inputName)
                    .findOne();

            assertThat(actualUrl).isNotNull();
            assertThat(actualUrl.getName()).isEqualTo(inputName);
        }

        @Test
        void testChecks() throws IOException {
            MockWebServer server = new MockWebServer();
            MockResponse mockResponse = new MockResponse()
                    .addHeader("Content-Type", "text/html; charset=utf-8")
                    .addHeader("title", "Title")
                    .setBody("hello");
            server.enqueue(mockResponse);
            server.start(6000);


            String nameUrl = server.url("/").toString();
            Url url = new Url(nameUrl);
            url.save();

            UrlCheck urlCheck = new UrlCheck(200, "Title", "", "hello", url);
            urlCheck.save();

            Url urlTest = new QUrl()
                    .name.equalTo(nameUrl)
                    .findOne();

            String numberUrl = Long.toString(urlTest.getId());

            HttpResponse<String> response = Unirest
                    .post(baseUrl + "/urls/" + numberUrl)
                    .asString();
            String body = response.getBody();

            assertThat(body).contains(mockResponse.getHeaders().get("title"));
            assertThat(body).contains(mockResponse.getBody().readUtf8());

            server.shutdown();
        }
    }
}
