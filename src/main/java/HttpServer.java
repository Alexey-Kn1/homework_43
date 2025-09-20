import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class HttpServer {
    private static final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");

    private Thread connectionsAccepting;
    private ExecutorService connectionsServing;
    private boolean runningFlag;
    private ServerSocket listener;
    private int tcpPort;
    private int connectionsServingThreadsNumber;
    private Consumer<Exception> exceptionsHandler;
    private boolean waitOnStop;

    public HttpServer() {
        this(80, 64, null);
    }

    public HttpServer(int tcpPort, int connectionsServingThreadsNumber, Consumer<Exception> exceptionsHandler) {
        this.connectionsServingThreadsNumber = connectionsServingThreadsNumber;
        this.exceptionsHandler = exceptionsHandler;
        this.tcpPort = tcpPort;
        runningFlag = false;
    }

    public int getTcpPort() {
        return tcpPort;
    }

    public void setTcpPort(int tcpPort) {
        this.tcpPort = tcpPort;
    }

    public int getConnectionsServingThreadsNumber() throws IllegalStateException {
        return connectionsServingThreadsNumber;
    }

    public void setConnectionsServingThreadsNumber(int connectionsServingThreadsNumber) {
        if (isRunning()) {
            throw new IllegalStateException("HTTP server settings shouldn't be changed while it is running");
        }

        this.connectionsServingThreadsNumber = connectionsServingThreadsNumber;
    }

    public Consumer<Exception> getExceptionsHandler() {
        if (isRunning()) {
            throw new IllegalStateException("HTTP server settings shouldn't be changed while it is running");
        }

        return exceptionsHandler;
    }

    public void setExceptionsHandler(Consumer<Exception> exceptionsHandler) {
        if (runningFlag) {
            throw new IllegalStateException("HTTP server settings shouldn't be changed while it is running");
        }

        this.exceptionsHandler = exceptionsHandler;
    }

    public void run() throws IOException {
        if (runningFlag) {
            return;
        }

        listener = new ServerSocket(tcpPort);

        connectionsServing = Executors.newFixedThreadPool(connectionsServingThreadsNumber);

        waitOnStop = false;

        runningFlag = true;

        acceptConnections();

        if (!listener.isClosed()) {
            listener.close();
        }

        connectionsServing.close();

        runningFlag = false;
    }

    public void start() throws IOException {
        if (runningFlag) {
            return;
        }

        listener = new ServerSocket(tcpPort);

        connectionsAccepting = new Thread(this::acceptConnections);

        connectionsAccepting.start();

        connectionsServing = Executors.newFixedThreadPool(connectionsServingThreadsNumber);

        waitOnStop = true;

        runningFlag = true;
    }

    public void stop() throws IOException, InterruptedException {
        if (!runningFlag) {
            return;
        }

        if (!listener.isClosed()) {
            listener.close();
        }

        if (waitOnStop) {
            connectionsAccepting.join();

            connectionsServing.close();

            runningFlag = false;
        }
    }

    public boolean isRunning() {
        return runningFlag;
    }

    private void acceptConnections() {
        while (true) {
            Socket connection;

            try {
                connection = listener.accept();
            } catch (SocketException ex) {
                if (ex.getMessage().equals("Socket closed")) {
                    break;
                }

                if (exceptionsHandler != null) {
                    exceptionsHandler.accept(ex);
                }

                continue;
            } catch (Exception ex) {
                if (exceptionsHandler != null) {
                    exceptionsHandler.accept(ex);
                }

                continue;
            }

            connectionsServing.execute(
                    () -> serveConnection(connection)
            );
        }
    }

    private void serveConnection(Socket socket) {
        try (
                socket;
                final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                final var out = new BufferedOutputStream(socket.getOutputStream());
        ) {
            // read only request line for simplicity
            // must be in form GET /path HTTP/1.1
            final var requestLine = in.readLine();
            final var parts = requestLine.split(" ");

            if (parts.length != 3) {
                // just close socket
                return;
            }

            final var path = parts[1];
            if (!validPaths.contains(path)) {
                out.write((
                        "HTTP/1.1 404 Not Found\r\n" +
                                "Content-Length: 0\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.flush();
                return;
            }

            final var filePath = Path.of(".", "public", path);
            final var mimeType = Files.probeContentType(filePath);

            // special case for classic
            if (path.equals("/classic.html")) {
                final var template = Files.readString(filePath);
                final var content = template.replace(
                        "{time}",
                        LocalDateTime.now().toString()
                ).getBytes();
                out.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: " + mimeType + "\r\n" +
                                "Content-Length: " + content.length + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.write(content);
                out.flush();
                return;
            }

            final var length = Files.size(filePath);
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            Files.copy(filePath, out);
            out.flush();
        } catch (IOException ex) {
            if (exceptionsHandler != null) {
                exceptionsHandler.accept(ex);
            }
        }
    }
}
