import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class HttpServer {
    private static class HandlerKey implements Comparable<HandlerKey> {
        private String method;
        private String path;

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public HandlerKey(String method, String path) {
            this.method = method;
            this.path = path;
        }

        @Override
        public int hashCode() {
            return Objects.hash(method, path);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof HandlerKey converted)) {
                return false;
            }

            return compareTo(converted) == 0;
        }

        @Override
        public int compareTo(HandlerKey o) {
            int res = method.compareTo(o.method);

            if (res != 0) {
                return res;
            }

            return path.compareTo(o.path);
        }
    }

    private Thread connectionsAccepting;
    private ExecutorService connectionsServing;
    private boolean runningFlag;
    private ServerSocket listener;
    private int tcpPort;
    private int connectionsServingThreadsNumber;
    private Consumer<Exception> exceptionsHandler;
    private boolean waitOnStop;
    private final Map<HandlerKey, HttpHandler> handlers;
    private HttpHandler defaultHandler;

    public HttpServer() {
        this(80, 64, null);
    }

    public HttpServer(int tcpPort, int connectionsServingThreadsNumber, Consumer<Exception> exceptionsHandler) {
        this.connectionsServingThreadsNumber = connectionsServingThreadsNumber;
        this.exceptionsHandler = exceptionsHandler;
        this.tcpPort = tcpPort;
        runningFlag = false;
        handlers = new HashMap<>();
        defaultHandler = null;
    }

    public void addHandler(String method, String path, HttpHandler handler) {
        if (runningFlag) {
            throw new IllegalStateException("HTTP server settings shouldn't be changed while it is running");
        }

        handlers.put(new HandlerKey(method, path), handler);
    }

    public void addDefaultHandler(HttpHandler handler) {
        if (runningFlag) {
            throw new IllegalStateException("HTTP server settings shouldn't be changed while it is running");
        }

        defaultHandler = handler;
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
        try (socket) {
            var request = new HttpRequest(socket.getInputStream());

            var handler = handlers.getOrDefault(new HandlerKey(request.getMethod(), request.getPath()), null);

            var output = new BufferedOutputStream(socket.getOutputStream());

            try {
                if (handler == null) {
                    if (defaultHandler != null) {
                        defaultHandler.handle(request, output);
                    }
                } else {
                    handler.handle(request, output);
                }
            } finally {
                output.flush();
            }
        } catch (Exception ex) {
            if (exceptionsHandler != null) {
                exceptionsHandler.accept(ex);
            }
        }
    }
}
