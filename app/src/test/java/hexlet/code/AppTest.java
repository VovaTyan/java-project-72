package hexlet.code;

import hexlet.code.domain.Url;
import hexlet.code.domain.query.QUrl;
import io.ebean.DB;
import io.ebean.Database;
import io.javalin.Javalin;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;

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

    // Тесты не зависят друг от друга
    // Но хорошей практикой будет возвращать базу данных между тестами в исходное состояние
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
        void testChecks() throws IOException {
            MockWebServer server = new MockWebServer();

            MockResponse mockResponse = new MockResponse()
                    .addHeader("Content-Type", "text/html; charset=utf-8")
                    .addHeader("title", "Title")
                    .setBody("hello");
            server.enqueue(mockResponse);
            server.start(5000);

            assertThat(server.url("/").toString()).isEqualTo("http://localhost:5000/");
            assertThat(mockResponse.getStatus()).contains("200");
            assertThat(mockResponse.getHeaders().get("title")).isEqualTo("Title");
            assertThat(mockResponse.getBody().readUtf8()).isEqualTo("hello");

            server.shutdown();
        }
    }
}
