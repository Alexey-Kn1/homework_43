import java.io.BufferedOutputStream;

@FunctionalInterface
public interface HttpHandler {
    void handle(HttpRequest request, BufferedOutputStream response);
}
