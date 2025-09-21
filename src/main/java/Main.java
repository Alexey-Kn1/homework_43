import java.io.BufferedOutputStream;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        HttpServer srv = new HttpServer(4444, 64, null);

        srv.addHandler(
                "GET",
                "/messages",
                (HttpRequest request, BufferedOutputStream responseStream) -> {
                    var responseStr = "/messages response";
                    var responseBytes = responseStr.getBytes();

                    try {

                        responseStream.write(
                                (
                                        "HTTP/1.1 200 OK\r\n" +
                                                "Content-Type: text/plain; charset=UTF-8\r\n" +
                                                "Content-Length: " + responseBytes.length + "\r\n" +
                                                "Connection: close\r\n" +
                                                "\r\n"
                                ).getBytes()
                        );

                        responseStream.write(responseBytes);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
        );

        srv.addHandler(
                "POST",
                "/messages",
                (HttpRequest request, BufferedOutputStream responseStream) -> {
                    var responseStr = "/messages response";
                    var responseBytes = responseStr.getBytes();

                    try {

                        responseStream.write(
                                (
                                        "HTTP/1.1 200 OK\r\n" +
                                                "Content-Type: text/plain; charset=UTF-8\r\n" +
                                                "Content-Length: " + responseBytes.length + "\r\n" +
                                                "Connection: close\r\n" +
                                                "\r\n"
                                ).getBytes()
                        );

                        responseStream.write(responseBytes);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
        );

        srv.start();

        var sc = new Scanner(System.in);

        sc.nextLine();

        srv.stop();
    }
}
