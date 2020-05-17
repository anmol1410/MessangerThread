package com.anmol.java;

import java.io.Serializable;

/**
 * Message packets which are send across the Threads.
 * <br>This has a linkedList of messages, <b>sorted</b> according to its <b>when</b>(when should it be processed).
 * <br> It can be created using {@link Message#obtain()} method, which ensures the message is pulled.added to to pool,
 * and unnecessary Message Object creations can be avoided.
 */
public final class Message implements Serializable {

    private static final int FLAG_IN_USE = 1;
    private static final int FLAGS_TO_CLEAR_ON_COPY_FROM = FLAG_IN_USE;
    private int flags;

    /**
     * Time at which this message should be processed.
     * This message will not be dispatched by the consumer before this time.
     */
    long when;

    /**
     * Actual data this message contains.
     */
    private Object data;

    /**
     * Callback who can handle this message after its read by the consumer.
     */
    private Runnable callback;

    /**
     * Linked list to maintain a pool of messages.
     */
    Message next;

    private static final Object sPoolSync = new Object();
    private static Message sPool; // Pool to avoid unnecessary object creations.
    private static int sPoolSize = 0;

    private static final int MAX_POOL_SIZE = 50;

    /**
     * Get the empty message which you can set data/callback on.
     * <br>
     * Private Constructor so that the message is always reused, and pulled from the pool.
     */
    public static Message obtain() {
        synchronized (sPoolSync) {
            if (sPool != null) {
                Message m = sPool;
                sPool = m.next;
                m.next = null;
                m.flags = 0; // clear in-use flag
                sPoolSize--;
                return m;
            }
        }
        return new Message(); // No message in the pool? Create a new one then.
    }

    /**
     * Clones the Message.
     */
    public static Message obtain(final Message orig) {
        final Message m = obtain();
        m.callback = orig.callback;
        if (orig.data != null) {
            m.data = orig.data;
        }
        return m;
    }

    /**
     * Recycle the message to avoid unnecessary object creations.
     */
    public void recycle() {
        if (isInUse()) {
            throw new IllegalStateException("This message cannot be recycled because it is still in use.");
        }
        recycleUnchecked();
    }

    /**
     * Does not check if the message is currently being used. It recycles it anyways.
     * <br>
     * Useful when we want to clear the messages explicitly.
     */
    void recycleUnchecked() {
        flags = FLAG_IN_USE;
        when = 0;
        callback = null;
        data = null;

        synchronized (sPoolSync) {
            if (sPoolSize < MAX_POOL_SIZE)
                next = sPool;
            sPool = this;
            sPoolSize++;
        }
    }

    public void copyFrom(Message o) {
        this.flags = o.flags & ~FLAGS_TO_CLEAR_ON_COPY_FROM;

        if (o.data != null) {
            this.data = o.data;
        } else {
            this.data = null;
        }
    }

    public Object data() {
        return data;
    }

    public Message withData(final Object data) {
        this.data = data;
        return this;
    }

    long when() {
        return when;
    }

    void setWhen(final long when) {
        this.when = when;
    }

    Runnable callback() {
        return callback;
    }

    Message withCallback(final Runnable callback) {
        this.callback = callback;
        return this;
    }

    boolean isInUse() {
        return ((flags & FLAG_IN_USE) == FLAG_IN_USE);
    }

    void markInUse() {
        flags |= FLAG_IN_USE;
    }

    private Message() {
    }
}
