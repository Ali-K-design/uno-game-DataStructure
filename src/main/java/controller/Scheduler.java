package controller;

/** Lets the controller delay bot moves without knowing about threads or JavaFX. */
public interface Scheduler {
    void later(long delayMillis, Runnable task);

    /** Runs the task right away (console and tests). */
    static Scheduler immediate() { return (delay, task) -> task.run(); }
}
