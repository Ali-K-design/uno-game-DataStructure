package view.console;

import network.Protocol;
import network.RemoteApp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class NetConsoleMain {
    public static void main(String[] args) throws IOException {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : Protocol.DEFAULT_PORT;
        RemoteApp app = RemoteApp.connect(host, port, Runnable::run);
        System.out.println("Connected to " + host + ":" + port);
        new ConsoleApp(app, new BufferedReader(new InputStreamReader(System.in)), System.out).run();
        app.getClient().close();
    }
}
