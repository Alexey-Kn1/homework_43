import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        HttpServer srv = new HttpServer(4444, 64, null);

        srv.start();

        var sc = new Scanner(System.in);

        sc.nextLine();

        srv.stop();
    }
}
