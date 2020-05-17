package com.anmol.java;

public abstract class MessengerThread extends Thread {
    private Consumer mConsumer;
    private MessageQueue mQueue;

    protected abstract void onRun();

    protected void onMessage(final Message msg) {
    }

    @Override
    public final void run() {
        Consumer.init();
        synchronized (this) {
            mConsumer = Consumer.myConsumer();
            mQueue = mConsumer.queue();
            notifyAll();
        }
        onRun();
        Consumer.start();
    }

    private Consumer consumer() {
        if (!isAlive()) {
            return null;
        }

        synchronized (this) {
            while (isAlive() && mConsumer == null) {
                try {
                    wait();
                } catch (InterruptedException ignored) {
                }
            }
        }
        return mConsumer;
    }

    public boolean close() {
        final Consumer consumer = consumer();
        if (consumer != null) {
            consumer.stop();
            return true;
        }
        return false;
    }

    public boolean closeSafely() {
        Consumer consumer = consumer();
        if (consumer != null) {
            consumer.stopSafely();
            return true;
        }
        return false;
    }

    // Write Operations

    public final boolean post(final Runnable runnable) {
        return sendMessageDelayed(Message.obtain().withCallback(runnable), 0);
    }

    public final boolean postAtTime(final Runnable runnable, final long uptimeMillis) {
        return sendMessageAtTime(Message.obtain().withCallback(runnable), uptimeMillis);
    }

    public final boolean postDelayed(final Runnable runnable, final long delayMillis) {
        return sendMessageDelayed(Message.obtain().withCallback(runnable), delayMillis);
    }

    public final boolean postAtFrontOfQueue(final Runnable runnable) {
        return sendMessageAtFrontOfQueue(Message.obtain().withCallback(runnable));
    }

    public final void removeCallbacks(final Runnable runnable) {
        mQueue.removeMessages(runnable, null);
    }

    public final void removeCallbacks(final Runnable runnable, final Object token) {
        mQueue.removeMessages(runnable, token);
    }

    public final boolean sendMessage(final Message msg) {
        return sendMessageDelayed(msg, 0);
    }

    public final boolean sendEmptyMessage() {
        return sendEmptyMessageDelayed(0);
    }

    public final boolean sendEmptyMessageDelayed(final long delayMillis) {
        return sendMessageDelayed(Message.obtain(), delayMillis);
    }

    public final boolean sendEmptyMessageAtTime(final long uptimeMillis) {
        return sendMessageAtTime(Message.obtain(), uptimeMillis);
    }

    public final boolean sendMessageDelayed(final Message msg, long delayMillis) {
        if (delayMillis < 0) {
            delayMillis = 0;
        }
        return sendMessageAtTime(msg, System.currentTimeMillis() + delayMillis);
    }

    public boolean sendMessageAtTime(final Message msg, final long uptimeMillis) {
        if (mQueue == null) {
            return false;
        }
        return enqueueMessage(mQueue, msg, uptimeMillis);
    }

    public final boolean sendMessageAtFrontOfQueue(final Message msg) {
        if (mQueue == null) {
            return false;
        }
        return enqueueMessage(mQueue, msg, 0);
    }

    private boolean enqueueMessage(final MessageQueue queue, final Message msg, final long uptimeMillis) {
        return queue.enqueueMessage(msg, uptimeMillis);
    }

    public final void removeMessages() {
        mQueue.removeMessages(null);
    }

    public final void removeMessages(final Object dataValue) {
        mQueue.removeMessages(dataValue);
    }

    public final void removeCallbacksAndMessages(final Object data) {
        mQueue.removeCallbacksAndMessages(data);
    }

    public final boolean hasMessages() {
        return mQueue.hasMessages(null);
    }

    public final boolean hasMessages(final Object object) {
        return mQueue.hasMessages(object);
    }

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