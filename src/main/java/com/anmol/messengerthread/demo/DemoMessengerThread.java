package com.anmol.messengerthread.demo;

import com.anmol.java.Message;
import com.anmol.java.MessengerThread;

public class DemoMessengerThread extends MessengerThread {

    private MessengerThread threadToSendTo;

    public DemoMessengerThread(final String name) {
        super(name);
    }

    @Override
    protected void onRun() {
        // Option 1 : send Runnable which will be picked up by threadToSendTo and run by it.
        threadToSendTo.post(() -> System.out.println("Sent by " + getName() + ", and Received by: " + Thread.currentThread().getName()));

        // Option 2: Send a message. onMessage of that ConsumerThread will handle the message.
        threadToSendTo.sendMessage(Message.obtain().withData("Sent by " + getName()));
    }

    @Override
    protected void onMessage(final Message msg) {
        System.out.println(msg.data() + ", and Received by: " + Thread.currentThread().getName());
    }

    public void toSendTo(final MessengerThread threadToSendTo) {
        this.threadToSendTo = threadToSendTo;
    }
}
