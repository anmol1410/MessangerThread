package com.anmol.java;

final class MessageQueue {

    private Message mMessages;
    private boolean mStopping;

    Message next() {
        for (; ; ) {
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
        }
    }

    boolean enqueueMessage(Message msg, long when) {
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
                msg.next = p;
                mMessages = msg;
            } else {
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

    boolean hasMessages(final Object object) {
        synchronized (this) {
            Message p = mMessages;
            while (p != null) {
                if ((object == null || p.data() == object)) {
                    return true;
                }
                p = p.next;
            }
            return false;
        }
    }

    boolean hasMessages(final Runnable r, final Object object) {
        synchronized (this) {
            Message p = mMessages;
            while (p != null) {
                if (p.callback() == r && (object == null || p.data() == object)) {
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

    void removeMessages(final Runnable r, final Object object) {
        if (r == null) {
            return;
        }

        synchronized (this) {
            Message p = mMessages;

            // Remove all messages at front.
            while (p != null && p.callback() == r && (object == null || p.data() == object)) {
                Message n = p.next;
                mMessages = n;
                p.recycleUnchecked();
                p = n;
            }

            // Remove all messages after front.
            while (p != null) {
                Message n = p.next;
                if (n != null) {
                    if (n.callback() == r && (object == null || n.data() == object)) {
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

    void removeCallbacksAndMessages(final Object object) {
        synchronized (this) {
            Message p = mMessages;

            // Remove all messages at front.
            while (p != null && (object == null || p.data() == object)) {
                final Message n = p.next;
                mMessages = n;
                p.recycleUnchecked();
                p = n;
            }

            // Remove all messages after front.
            while (p != null) {
                final Message n = p.next;
                if (n != null) {
                    if (n.data() == object) {
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

    private void removeAllMessagesLocked() {
        Message p = mMessages;
        while (p != null) {
            final Message n = p.next;
            p.recycleUnchecked();
            p = n;
        }
        mMessages = null;
    }

    private void removeAllFutureMessagesLocked() {
        final long now = System.currentTimeMillis();
        Message p = mMessages;
        if (p != null) {
            if (p.when() > now) {
                removeAllMessagesLocked();
            } else {
                Message n;
                for (; ; ) {
                    n = p.next;
                    if (n == null) {
                        return;
                    }
                    if (n.when() > now) {
                        break;
                    }
                    p = n;
                }
                p.next = null;
                do {
                    p = n;
                    n = p.next;
                    p.recycleUnchecked();
                } while (n != null);
            }
        }
    }
}