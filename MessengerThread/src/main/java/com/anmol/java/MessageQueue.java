package com.anmol.java;

/**
 * The Queue of messages associated with every {@link MessengerThread}.
 */
final class MessageQueue {
    private Message mMessages; // All the messages(ts a linked list).
    private boolean mStopping; // If queue is asked to stop.

    /**
     * Read the next message in the Queue. It blocks until the message arrives.
     */
    Message next() {
        while (true) {
            synchronized (this) {
                // Try to retrieve the next message.  Return if found.
                final long now = System.currentTimeMillis();
                final Message msg = mMessages;
                if (msg != null) {
                    if (now >= msg.when()) {
                        // Got a message.
                        mMessages = msg.next;
                        msg.next = null;
                        msg.markInUse();
                        return msg;
                    }
                }

                if (mStopping) {
                    return null;
                }
            }
        }
    }

    /**
     * Stops the Queue, and prevent adding or reading more msgs into/from it.
     */
    void stop(final boolean safe) {
        synchronized (this) {
            if (mStopping) {
                return;
            }
            mStopping = true;

            if (safe) {
                removeAllFutureMessagesLocked();
            } else {
                removeAllMessagesLocked();
            }

            // We can assume mPtr != 0 because mStopping was previously false.
            notifyAll();

            mStopping = false;
        }
    }

    /**
     * Add the message into the queue.
     * <br>
     * It will iterate the queue(sorted on its when), and find the slot to place this into, such that the queue is still sorted.
     *
     * @param msg  Message to add.
     * @param when The time to process it.
     * @return True if it is added, false otherwise like if queue was stopping etc.
     */
    boolean enqueueMessage(final Message msg, final long when) {
        if (msg.isInUse()) {
            throw new IllegalStateException(msg + " This message is already in use.");
        }

        synchronized (this) {
            if (mStopping) {
                msg.recycle();
                return false;
            }

            msg.markInUse();
            msg.setWhen(when);
            Message p = mMessages;
            if (p == null || when == 0 || when < p.when()) {
                msg.next = p; // as it has when before the first msg, add it at the head
                mMessages = msg; // and reset head to this.
            } else {
                // Find the appropriate slot to place this message, based on its when.
                Message prev;
                do {
                    prev = p;
                    p = p.next;
                } while (p != null && when >= p.when());
                msg.next = p;
                prev.next = msg;
            }
            notifyAll();
        }
        return true;
    }

    boolean hasMessages(final Object data) {
        synchronized (this) {
            Message p = mMessages;
            while (p != null) {
                if ((data == null || p.data() == data)) {
                    return true;
                }
                p = p.next;
            }
            return false;
        }
    }

    boolean hasMessages(final Runnable r, final Object data) {
        synchronized (this) {
            Message p = mMessages;
            while (p != null) {
                if (p.callback() == r && (data == null || p.data() == data)) {
                    return true;
                }
                p = p.next;
            }
            return false;
        }
    }

    void removeMessages(final Object dataToRemove) {
        synchronized (this) {
            Message p = mMessages;

            // Remove all messages at front.
            while (p != null && (dataToRemove == null || p.data() == dataToRemove)) {
                Message n = p.next;
                mMessages = n;
                p.recycleUnchecked();
                p = n;
            }

            // Remove all messages after front.
            while (p != null) {
                Message n = p.next;
                if (n != null) {
                    if (n.data() == dataToRemove) {
                        Message nn = n.next;
                        n.recycleUnchecked();
                        p.next = nn;
                        continue;
                    }
                }
                p = n;
            }
        }
    }

    void removeMessages(final Runnable r, final Object data) {
        if (r == null) {
            return;
        }

        synchronized (this) {
            Message p = mMessages;

            // Remove all messages at front.
            while (p != null && p.callback() == r && (data == null || p.data() == data)) {
                Message n = p.next;
                mMessages = n;
                p.recycleUnchecked();
                p = n;
            }

            // Remove all messages after front.
            while (p != null) {
                Message n = p.next;
                if (n != null) {
                    if (n.callback() == r && (data == null || n.data() == data)) {
                        Message nn = n.next;
                        n.recycleUnchecked();
                        p.next = nn;
                        continue;
                    }
                }
                p = n;
            }
        }
    }

    /**
     * Remove all the messages in the queue,  so they wont be read and dispatched by the consumer.
     */
    private void removeAllMessagesLocked() {
        Message p = mMessages; // Iterate over the LinkedList and remove one message at a time.
        while (p != null) {
            final Message n = p.next;
            p.recycleUnchecked();
            p = n;
        }
        mMessages = null;
    }

    /**
     * Remove all messages with their {@link Message#when} after the current time. All messages before this will not be removed.
     */
    private void removeAllFutureMessagesLocked() {
        final long now = System.currentTimeMillis();
        Message p = mMessages;
        if (p != null) {
            if (p.when() > now) {
                // All messages to the next of p are added after this p, so they can be removed.
                removeAllMessagesLocked();
            } else {
                Message n;
                while (true) { // Keep iterating until we find the message to be processed in the future.
                    n = p.next;
                    if (n == null) {
                        // Reached end, no more messages.
                        return;
                    }
                    if (n.when() > now) {
                        break;
                    }
                    p = n;
                }
                p.next = null; // p is the last message which should be processed. So do not remove them.

                // Recycled/remove all messages after n(n being the first message to be processed in the future).
                do {
                    p = n;
                    n = p.next;
                    p.recycleUnchecked();
                } while (n != null);
            }
        }
    }
}