import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

public class RequestParsingTest {
    @Test
    public void parseHTTPRequest() throws Exception {
        final String complicatedParams = "login=%D0%9C%D0%BE%D0%B9+%D0%B2%D0%BE%D1%81%D1%85%D0%B8%D1%82%D0%B8%D1%82%D0%B5%D0%BB%D1%8C%D0%BD%D1%8B%D0%B9+%D0%BB%D0%BE%D0%B3%D0%B8%D0%BD&password=%D0%9C%D0%BE%D0%B9+%D0%BD%D0%B5%D0%BF%D0%BE%D0%B2%D1%82%D0%BE%D1%80%D0%B8%D0%BC%D1%8B%D0%B9+%D0%BF%D0%B0%D1%80%D0%BE%D0%BB%D1%8C+%D1%81+%D0%BD%D0%B5%D0%BE%D0%B1%D1%8B%D1%87%D0%BD%D1%8B%D0%BC%D0%B8+%D1%81%D0%B8%D0%BC%D0%B2%D0%BE%D0%BB%D0%B0%D0%BC%D0%B8%3A+!%22%E2%84%96%3B%25%3A%3F*()_%2B{}[]%2F\\|%22%3B%3C%3E_%3D%23%40%24";

        final String requestText =
                "GET /index.html?" + complicatedParams + " HTTP/1.1\r\n" +
                        "Content-Type: text/html; charset=UTF-8\r\n" +
                        "Date:Fri, 21 Jun 2024 14:18:33 GMT\r\n" +
                        "Last-Modified: Thu, 17 Oct 2019 07:18:26 GMT\r\n" +
                        "Content-Length: 1234\r\n" +
                        "\r\n" +
                        "CONTENT";

        var r = new HttpRequest(new ByteArrayInputStream(requestText.getBytes()));

        Assertions.assertEquals("GET", r.getMethod());

        Assertions.assertEquals("/index.html", r.getPath());

        var args = r.getArguments();

        Assertions.assertEquals(2, args.size());

        var loginArgumentValues = args.get("login");

        Assertions.assertEquals(1, loginArgumentValues.size());

        Assertions.assertEquals("Мой восхитительный логин", loginArgumentValues.getFirst());

        var passwordArgumentValues = args.get("password");

        Assertions.assertEquals(1, passwordArgumentValues.size());

        Assertions.assertEquals("Мой неповторимый пароль с необычными символами: !\"№;%:?*()_+{}[]/\\|\";<>_=#@$", passwordArgumentValues.getFirst());

        var headers = r.getHeaders();

        Assertions.assertEquals(4, headers.size());

        Assertions.assertEquals("text/html; charset=UTF-8", headers.get("Content-Type"));
        Assertions.assertEquals("Fri, 21 Jun 2024 14:18:33 GMT", headers.get("Date"));
        Assertions.assertEquals("Thu, 17 Oct 2019 07:18:26 GMT", headers.get("Last-Modified"));
        Assertions.assertEquals("1234", headers.get("Content-Length"));

        Assertions.assertEquals("CONTENT", r.parseBody());
    }
}