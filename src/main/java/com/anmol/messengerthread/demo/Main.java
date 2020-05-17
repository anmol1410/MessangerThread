package com.anmol.messengerthread.demo;

import com.anmol.java.Message;

public class Main {

    public static void main(String[] args) {
        // Create 2 MessengerThread to send/receive msgs to/from each other.
        final DemoMessengerThread messengerThread1 = new DemoMessengerThread("messengerThread1");
        final DemoMessengerThread messengerThread2 = new DemoMessengerThread("messengerThread2");

        messengerThread1.toSendTo(messengerThread2);
        messengerThread2.toSendTo(messengerThread1);

        messengerThread1.start();
        messengerThread2.start();

        // Send another message from normal thread to MessengerThread
        new Thread(() -> {
            System.out.println("Running Inside : " + Thread.currentThread().getName());
            messengerThread1.post(() -> System.out.println("Sent by workerThread1 , and Received by: " + Thread.currentThread().getName()));
        }, "workerThread1").start();

        // Send another message from normal thread to MessengerThread
        new Thread(() -> {
            System.out.println("Running Inside : " + Thread.currentThread().getName());
            messengerThread1.sendMessage(Message.obtain().withData("Sent by workerThread2"));
        }, "workerThread2").start();


    }
}
