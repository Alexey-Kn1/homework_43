import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

public class RequestParsingTest {
    @Test
    public void parseHTTPRequest() throws Exception {
        final String requestText =
                "GET /index.html?login=1&password=2 HTTP/1.1\r\n" +
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

        Assertions.assertEquals("1", args.get("login"));

        Assertions.assertEquals("2", args.get("password"));

        var headers = r.getHeaders();

        Assertions.assertEquals(4, headers.size());

        Assertions.assertEquals("text/html; charset=UTF-8", headers.get("Content-Type"));
        Assertions.assertEquals("Fri, 21 Jun 2024 14:18:33 GMT", headers.get("Date"));
        Assertions.assertEquals("Thu, 17 Oct 2019 07:18:26 GMT", headers.get("Last-Modified"));
        Assertions.assertEquals("1234", headers.get("Content-Length"));

        Assertions.assertEquals("CONTENT", r.parseBody());
    }
}