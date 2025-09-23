import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        HttpServer srv = new HttpServer(4444, 64, null);

        srv.addDefaultHandler(
                (HttpRequest request, BufferedOutputStream responseStream) -> {
                    var path = "./public" + request.getPath();
                    var responseFile = new File(path);

                    try (
                            var fileContent = new FileInputStream(responseFile)
                    ) {

                        responseStream.write(
                                (
                                        "HTTP/1.1 200 OK\r\n" +
                                                "Content-Type: " + Files.probeContentType(Path.of(path)) + "\r\n" +
                                                "Content-Length: " + responseFile.length() + "\r\n" +
                                                "Connection: close\r\n" +
                                                "\r\n"
                                ).getBytes()
                        );

                        fileContent.transferTo(responseStream);
                    } catch (FileNotFoundException e) {
                        try {
                            responseStream.write(
                                    (
                                            "HTTP/1.1 404 Not Found\r\n" +
                                                    "Content-Length: 0\r\n" +
                                                    "Connection: close\r\n" +
                                                    "\r\n"
                                    ).getBytes()
                            );
                        } catch (Exception ignored) {

                        }
                    } catch (IOException e) {
                        try {
                            responseStream.write(
                                    (
                                            "HTTP/1.1 500 Internal Server Error\r\n" +
                                                    "Content-Length: 0\r\n" +
                                                    "Connection: close\r\n" +
                                                    "\r\n"
                                    ).getBytes()
                            );
                        } catch (Exception ignored) {

                        }
                    }
                }
        );

        srv.addHandler(
                "GET",
                "/arguments",
                (HttpRequest request, BufferedOutputStream responseStream) -> {
                    var arguments = request.getArguments();

                    var responseStrBuilder = new StringBuilder();

                    responseStrBuilder.append("Received URL arguments:\n");

                    for (var argument : arguments.entrySet()) {
                        for (var argValue : argument.getValue()) {
                            responseStrBuilder.append(
                                    String.format("\"%s\" = \"%s\"\n", argument.getKey(), argValue)
                            );
                        }
                    }

                    var responseBytes = responseStrBuilder.toString().getBytes();

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
