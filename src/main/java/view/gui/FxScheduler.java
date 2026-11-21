package view.gui;

import controller.Scheduler;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

public final class FxScheduler implements Scheduler {
    @Override
    public void later(long delayMillis, Runnable task) {
        PauseTransition pause = new PauseTransition(Duration.millis(Math.max(1, delayMillis)));
        pause.setOnFinished(e -> task.run());
        pause.play();
    }
}
