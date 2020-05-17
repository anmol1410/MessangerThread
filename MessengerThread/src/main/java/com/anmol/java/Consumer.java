package com.anmol.java;

/**
 * Loops through the messageQueue associated with the {@link MessengerThread}, until it is closed.
 */
final class Consumer {

    private static final ThreadLocal<Consumer> sThreadLocal = new ThreadLocal<>();

    private final MessageQueue mQueue; // Queue to loop on.
    private final Thread mThread; // Thread who's Queue is to read the messages from.

    private Consumer() {
        mQueue = new MessageQueue();
        mThread = Thread.currentThread();
    }

    /**
     * Thread which calls it will become a MessengerThread, which can accept messages from other threads.
     * <br>
     * Currently its Package protected, as you can directly extend the {@link MessengerThread}, and save yourself from all this stuff.
     */
    static void init() {
        if (sThreadLocal.get() != null) {
            throw new RuntimeException("Only one Consumer should be created per thread");
        }
        sThreadLocal.set(new Consumer());
    }

    /**
     * Start consuming the messages in the Queue, and dispatch them to the callbacks, to handle it.
     */
    static void start() {
        final Consumer me = myConsumer();
        if (me == null) {
            throw new RuntimeException("Consumer::init() wasn't called on this thread.");
        }

        while (true) { // Loop infinitely until the consumer is stopped.
            final Message msg = me.mQueue.next(); // Might block
            if (msg == null) {
                // No message indicates that the message queue is quitting.
                return;
            }

            // Reaching here means the message is read.
            if (msg.callback() != null) {
                msg.callback().run();
            } else {
                ((MessengerThread) myConsumer().mThread).onMessage(msg);
            }

            // Message is handled, so it can be recycled now, to make it reusable in the Message Pool.
            msg.recycleUnchecked();
        }
    }

    static Consumer myConsumer() {
        return sThreadLocal.get();
    }

    /**
     * Stop the Consumer from reading any more messages.
     */
    void stop() {
        mQueue.stop(false);
    }

    /**
     * Stop the messages which are not yet read, but process the messages currently being processed.
     */
    void stopSafely() {
        mQueue.stop(true);
    }

    MessageQueue queue() {
        return mQueue;
    }
}
