package view.console;

import controller.Scheduler;
import controller.UnoController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/** java -cp target/classes view.console.ConsoleMain */
public class ConsoleMain {
    public static void main(String[] args) throws IOException {
        UnoController controller = new UnoController(Scheduler.immediate(), 0);
        new ConsoleApp(controller, new BufferedReader(new InputStreamReader(System.in)), System.out).run();
    }
}
