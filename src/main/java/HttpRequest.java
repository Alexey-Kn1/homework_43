import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class HttpRequest {
    private final BufferedInputStream is;
    private final Reader reader;
    private final String method;
    private final String path;
    private final Map<String, List<String>> arguments;
    private final Map<String, String> headers;

    public HttpRequest(InputStream connection) throws Exception {
        this.is = new BufferedInputStream(connection);

        reader = new InputStreamReader(this.is);

        String header;

        {
            var headerBuilder = new StringBuilder();
            char[] lastSymbols = new char[4];
            int current;
            int parsed = 0;

            do {
                current = reader.read();

                if (current == -1) {
                    throw new HttpParsingException("failed to parse HTTP request");
                }

                if (parsed < lastSymbols.length) {
                    lastSymbols[parsed] = (char) current;

                    parsed++;
                } else {
                    int i = 0;

                    while (i < lastSymbols.length - 1) {
                        int nextI = i + 1;

                        lastSymbols[i] = lastSymbols[nextI];

                        i = nextI;
                    }

                    lastSymbols[i] = (char) current;
                }

                headerBuilder.append((char) current);
            } while (
                    parsed < 4
                            || !(lastSymbols[0] == '\r'
                            && lastSymbols[1] == '\n'
                            && lastSymbols[2] == '\r'
                            && lastSymbols[3] == '\n')
            );

            header = headerBuilder.toString();
        }

        var sc = new Scanner(header);

        method = sc.next();

        var pathWithArguments = sc.next();

        var protocol = sc.next();

        if (!protocol.equals("HTTP/1.1")) {
            throw new HttpParsingException("failed to parse HTTP request");
        }

        var splitPathWithArguments = pathWithArguments.split("\\?");

        if (splitPathWithArguments.length > 2) {
            throw new HttpParsingException("failed to parse HTTP request");
        }

        path = splitPathWithArguments[0];

        arguments = new HashMap<>();

        if (splitPathWithArguments.length > 1) {
            var decoded = URLDecoder.decode(splitPathWithArguments[1], StandardCharsets.UTF_8);

            var argumentsStr = decoded.split("&");

            for (var arg : argumentsStr) {
                var indexOfEquality = arg.indexOf("=");

                var key = arg.substring(0, indexOfEquality);

                var value = arg.substring(indexOfEquality + 1);

                var alreadyAdded = arguments.getOrDefault(key, null);

                if (alreadyAdded == null) {
                    var newList = new ArrayList<String>();

                    newList.add(value);

                    arguments.put(key, newList);
                } else {
                    alreadyAdded.add(value);
                }
            }
        }

        var empty = sc.nextLine();

        if (!empty.isBlank()) {
            throw new HttpParsingException("failed to parse HTTP request");
        }

        headers = new HashMap<>();

        while (true) {
            var lineWithHeader = sc.nextLine();

            if (lineWithHeader.isBlank()) {
                break;
            }

            var indexOfSemicolon = lineWithHeader.indexOf(": ");
            var endOfSemicolon = indexOfSemicolon + 2;

            if (indexOfSemicolon == -1) {
                indexOfSemicolon = lineWithHeader.indexOf(':');
                endOfSemicolon = indexOfSemicolon + 1;

                if (indexOfSemicolon == -1) {
                    throw new HttpParsingException("failed to parse HTTP request");
                }
            }

            headers.put(lineWithHeader.substring(0, indexOfSemicolon), lineWithHeader.substring(endOfSemicolon));
        }
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public Map<String, List<String>> getArguments() {
        return arguments;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public InputStream getBody() {
        return is;
    }

    public String parseBody() throws IOException {
        var builder = new StringBuilder();

        while (true) {
            int ch = reader.read();

            if (ch == -1) {
                break;
            }

            builder.append((char) ch);
        }

        return builder.toString();
    }
}
