package ru.nsu;

public class Main {
    public static void main(String[] args) throws Throwable
    {
        var serverThread = new Thread(new Proxy(1080));
        serverThread.start();
    }
}
