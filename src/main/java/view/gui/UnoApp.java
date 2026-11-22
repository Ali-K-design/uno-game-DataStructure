package view.gui;

import controller.AppActions;
import controller.UnoController;
import javafx.application.Application;
import javafx.stage.Stage;
import view.gui.demo.DemoBackend;


public class UnoApp extends Application {
    @Override
    public void start(Stage stage) {
        boolean demo = getParameters().getRaw().contains("--demo");
        boolean offline = demo || getParameters().getRaw().contains("--offline");
        AppActions app = demo ? new DemoBackend() : new UnoController(new FxScheduler(), 1100);
        Router router = new Router(stage, app);
        if (offline) router.showAuth();
        else router.showConnect(null);
        stage.show();
    }
}
