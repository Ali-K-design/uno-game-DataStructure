package controller;

public interface Scheduler {
    void later(long delayMillis, Runnable task);

    static Scheduler immediate() { return (delay, task) -> task.run(); }
}
