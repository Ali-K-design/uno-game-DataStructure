package network;

import java.io.IOException;

public class ServerMain {
    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : Protocol.DEFAULT_PORT;
        long delay = args.length > 1 ? Long.parseLong(args[1]) : 1100;
        UnoServer server = new UnoServer(port, delay);
        server.setVerbose(true);
        server.start();
        System.out.println("UNO server running on port " + server.getPort() + " (Ctrl+C to stop)");
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            server.stop();
        }
    }
}
