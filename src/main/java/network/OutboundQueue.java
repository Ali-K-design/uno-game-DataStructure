package network;

import dataStructures.CustomQueue;

final class OutboundQueue {
    private static final int MAX_PENDING = 2000;

    private final CustomQueue<String> queue = new CustomQueue<>();
    private boolean closed;

    synchronized boolean put(String line) {
        if (closed) return false;
        if (queue.getSize() >= MAX_PENDING) {
            closed = true;
            notifyAll();
            return false;
        }
        queue.enqueue(line);
        notifyAll();
        return true;
    }

    synchronized String take() throws InterruptedException {
        while (queue.isEmpty() && !closed) wait();
        return queue.isEmpty() ? null : queue.dequeue();
    }

    synchronized void close() {
        closed = true;
        notifyAll();
    }
}
