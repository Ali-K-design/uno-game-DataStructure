package view.gui;

import controller.AppActions;
import controller.UnoController;
import javafx.application.Application;
import javafx.stage.Stage;
import view.gui.demo.DemoBackend;

/** Run normally for the real game; pass --demo for the fake backend (UI preview only). */
public class UnoApp extends Application {
    @Override
    public void start(Stage stage) {
        boolean demo = getParameters().getRaw().contains("--demo");
        AppActions app = demo ? new DemoBackend() : new UnoController(new FxScheduler(), 1100);
        Router router = new Router(stage, app);
        router.showAuth();
        stage.show();
    }
}
