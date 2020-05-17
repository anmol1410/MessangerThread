package com.anmol.java;

/**
 * The Messenger thread, which can accept {@link Message messages}/{@link Runnable runnables}.
 * <br>
 * It maintains a {@link MessageQueue}(every {@link MessengerThread} will have a queue each), and other threads can then send messages to {@link MessengerThread}, which will get enqueued to its {@link MessageQueue}.
 * <br>
 * {@link MessageQueue} will store the messages in sorted order, sorting on the messages' <b>when</b>.
 * <br>Along with a queue, {@link MessengerThread} also has a consumer which keeps on reading from the queue(until stopped), and then dispatches the read msg to the callback onMessage/ or runs it if its a {@link Runnable}.
 * <br>2 Types of messages can be written:
 * <ol>
 *     <li> {@link Message}: which upon read by the {@link Consumer}, will be handled in the {@link MessengerThread#onMessage} of the current thread.</li>
 *     <li> {@link Runnable}: which upon read by the {@link Consumer}, will run inside the current Thread.</li>
 * </ol>
 * <p>
 * Messages can we send when the sender thread has no idea how to process such messages,<br> and Runnable can be send to execute this code when its read by the target thread.
 * <br>
 */
public abstract class MessengerThread extends Thread {
    private Consumer mConsumer;
    private MessageQueue mQueue;

    // Thread#run alternate
    protected abstract void onRun();

    /**
     * To handle the messages send onto this {@link MessengerThread}.
     *
     * @param msg Message read.
     */
    protected void onMessage(final Message msg) {
    }

    @Override
    public final void run() {
        Consumer.init(); // Setup the queue with this thread.
        synchronized (this) {
            mConsumer = Consumer.myConsumer();
            mQueue = mConsumer.queue();
            notifyAll(); // Notify that the Consumer is initialized, so that other threads if they need this Consumer can now be unblocked.
        }
        onRun();

        Consumer.start(); // Start polling msgs from the Queue.
    }

    /**
     * Get the Consumer associated with this {@link MessengerThread}.
     */
    private Consumer consumer() {
        if (!isAlive()) {
            // The thread has not yet started.
            return null;
        }

        synchronized (this) {
            while (isAlive() && mConsumer == null) {
                try {
                    wait(); // Thread started but not yet initialized yet.
                } catch (InterruptedException ignored) {
                }
            }
        }
        return mConsumer;
    }

    /**
     * Stop reading msgs from the Queue. After stopping the {@link MessengerThread} will behave as a normal {@link Thread}.
     *
     * @return True if it got stopped successfully, false otherwise.
     */
    public boolean close() {
        final Consumer consumer = consumer();
        if (consumer != null) {
            consumer.stop();
            return true;
        }
        return false;
    }

    /**
     * Close the queue to read msgs which are meant to be processed later(i.e. {@link Message#when} is after current time).
     * But process those which should have been processed by now (i.e. {@link Message#when} is before current time, but did not get the timeframe to process them).
     *
     * @return @return True if it got safe-stopped successfully, false otherwise.
     */
    public boolean closeSafely() {
        Consumer consumer = consumer();
        if (consumer != null) {
            consumer.stopSafely();
            return true;
        }
        return false;
    }

    // Write Operations

    /**
     * Send the Runnable to the {@link MessengerThread}, which should be processed at the current time
     * i.e. if no earlier msg is yet to be processed, it would be processed right away.
     * <br> Please note that the {@link MessengerThread} will call {@link Runnable#run()} method when it reads this msg from its queue.
     */
    public final boolean post(final Runnable runnable) {
        return sendMessageDelayed(Message.obtain().withCallback(runnable), 0);
    }

    /**
     * Send the Runnable to the {@link MessengerThread}, which should be processed at the uptimeMillis time.
     * <br> Please note that the {@link MessengerThread} will call {@link Runnable#run()} method when it reads this msg from its queue.
     */
    public final boolean postAtTime(final Runnable runnable, final long uptimeMillis) {
        return sendMessageAtTime(Message.obtain().withCallback(runnable), uptimeMillis);
    }

    /**
     * Send the Runnable to the {@link MessengerThread}, which should be processed after the delayMillis.
     * <br> Please note that the {@link MessengerThread} will call {@link Runnable#run()} method when it reads this msg from its queue.
     */
    public final boolean postDelayed(final Runnable runnable, final long delayMillis) {
        return sendMessageDelayed(Message.obtain().withCallback(runnable), delayMillis);
    }

    /**
     * It sends the Runnable to the {@link MessengerThread}, which should be processed which should be processed at the earliest.
     * <br> Please note that the {@link MessengerThread} will call {@link Runnable#run()} method when it reads this msg from its queue.
     */
    public final boolean postAtFrontOfQueue(final Runnable runnable) {
        return sendMessageAtFrontOfQueue(Message.obtain().withCallback(runnable));
    }

    /**
     * It sends the {@link Message} to the {@link MessengerThread}, which should be processed ASAP.
     * <br> Please note that the {@link MessengerThread} will send this message in {@link MessengerThread#onMessage(Message)} ()} method when it reads this msg from its queue.
     */
    public final boolean sendMessage(final Message msg) {
        return sendMessageDelayed(msg, 0);
    }

    /**
     * It sends the {@link Message} to the {@link MessengerThread}, which should be processed ASAP.
     * <br> This message has no Data, and acts like a heartbeat type message.
     * <br> Please note that the {@link MessengerThread} will send this message in {@link MessengerThread#onMessage(Message)} ()} method when it reads this msg from its queue.
     */
    public final boolean sendEmptyMessage() {
        return sendEmptyMessageDelayed(0);
    }

    /**
     * It sends the {@link Message empty message without any data} to the {@link MessengerThread}, which should be processed after the delayMillis.
     * <br> See {@link MessengerThread#sendEmptyMessage()}.
     * <br> Please note that the {@link MessengerThread} will send this message in {@link MessengerThread#onMessage(Message)} ()} method when it reads this msg from its queue.
     */
    public final boolean sendEmptyMessageDelayed(final long delayMillis) {
        return sendMessageDelayed(Message.obtain(), delayMillis);
    }

    /**
     * It sends the  {@link Message empty message without any data} to the {@link MessengerThread}, which should be processed at uptimeMillis.
     * <br> See {@link MessengerThread#sendEmptyMessage()}.
     * <br> Please note that the {@link MessengerThread} will send this message in {@link MessengerThread#onMessage(Message)} ()} method when it reads this msg from its queue.
     */
    public final boolean sendEmptyMessageAtTime(final long uptimeMillis) {
        return sendMessageAtTime(Message.obtain(), uptimeMillis);
    }

    /**
     * It sends the {@link Message} to the {@link MessengerThread}, which should be processed after the delay of delayMillis.
     * <br> Please note that the {@link MessengerThread} will send this message in {@link MessengerThread#onMessage(Message)} ()} method when it reads this msg from its queue.
     */
    public final boolean sendMessageDelayed(final Message msg, long delayMillis) {
        if (delayMillis < 0) {
            delayMillis = 0;
        }
        return sendMessageAtTime(msg, System.currentTimeMillis() + delayMillis);
    }

    /**
     * It sends the  {@link Message} to the {@link MessengerThread}, which should be processed at uptimeMillis.
     * <br> Please note that the {@link MessengerThread} will send this message in {@link MessengerThread#onMessage(Message)} ()} method when it reads this msg from its queue.
     */
    public boolean sendMessageAtTime(final Message msg, final long uptimeMillis) {
        if (mQueue == null) {
            return false;
        }
        return enqueueMessage(mQueue, msg, uptimeMillis);
    }

    /**
     * It sends the  {@link Message} to the {@link MessengerThread}, which should be processed at first..
     * <br> Please note that the {@link MessengerThread} will send this message in {@link MessengerThread#onMessage(Message)} ()} method when it reads this msg from its queue.
     */
    public final boolean sendMessageAtFrontOfQueue(final Message msg) {
        if (mQueue == null) {
            return false;
        }
        return enqueueMessage(mQueue, msg, 0);
    }

    /**
     * Adds the message into the queue.
     */
    private boolean enqueueMessage(final MessageQueue queue, final Message msg, final long uptimeMillis) {
        return queue.enqueueMessage(msg, uptimeMillis);
    }

    /**
     * Removes all the messages in the queue.
     */
    public final void removeMessages() {
        mQueue.removeMessages(null);
    }

    /**
     * Removes the messages with Data=dataValue.
     */
    public final void removeMessages(final Object dataValue) {
        mQueue.removeMessages(dataValue);
    }

    /**
     * Removes the messages with the provided Runnable.
     */
    public final void removeMessages(final Runnable runnable) {
        mQueue.removeMessages(runnable, null);
    }

    /**
     * Removes the messages with the matching Runnable and data.
     */
    public final void removeMessages(final Runnable runnable, final Object data) {
        mQueue.removeMessages(runnable, data);
    }

    /**
     * Tells if there is any {@link Message} or {@link Runnable} in the queue yet to be processed.
     */
    public final boolean hasMessages() {
        return mQueue.hasMessages(null);
    }

    /**
     * Tells if there is any {@link Message} or {@link Runnable} in the queue with data=dataValue yet to be processed.
     */
    public final boolean hasMessages(final Object object) {
        return mQueue.hasMessages(object);
    }

    /**
     * Tells if there is any {@link Runnable} in the queue which yet to be processed.
     */
    public final boolean hasCallbacks(final Runnable runnable) {
        return mQueue.hasMessages(runnable, null);
    }

    // Normal Thread constructors
    public MessengerThread() {
    }

    public MessengerThread(final Runnable target) {
        super(target);
    }

    public MessengerThread(final ThreadGroup group, final Runnable target) {
        super(group, target);
    }

    public MessengerThread(final String name) {
        super(name);
    }

    public MessengerThread(final ThreadGroup group, final String name) {
        super(group, name);
    }

    public MessengerThread(final Runnable target, final String name) {
        super(target, name);
    }

    public MessengerThread(final ThreadGroup group, final Runnable target, final String name) {
        super(group, target, name);
    }

    public MessengerThread(final ThreadGroup group, final Runnable target, final String name, final long stackSize) {
        super(group, target, name, stackSize);
    }
}