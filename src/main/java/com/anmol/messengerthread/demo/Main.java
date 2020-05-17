package com.anmol.messengerthread.demo;

import com.anmol.java.Message;

public class Main {

    public static void main(String[] args) {
        final DemoMessengerThread threadOne = new DemoMessengerThread("Thread_1");
        final DemoMessengerThread threadTwo = new DemoMessengerThread("Thread_2");

        threadOne.toSendTo(threadTwo);
        threadTwo.toSendTo(threadOne);

        threadOne.start();
        threadTwo.start();

        // Send another message from normal thread to MessengerThread
        new Thread(() -> {
            System.out.println("Running Inside : " + Thread.currentThread().getName());
            threadOne.post(() -> System.out.println("Sent by workerThread1 , and Received by: " + Thread.currentThread().getName()));
        }, "workerThread1").start();

        // Send another message from normal thread to MessengerThread
        new Thread(() -> {
            System.out.println("Running Inside : " + Thread.currentThread().getName());
            threadOne.sendMessage(Message.obtain().withData("Sent by workerThread2"));
        }, "workerThread2").start();

    }
}
