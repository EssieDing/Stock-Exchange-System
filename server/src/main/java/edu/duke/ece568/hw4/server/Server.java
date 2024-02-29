package edu.duke.ece568.hw4.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Server {
    private final int port;
    private ServerSocket serverSocket;

    public Server(int port) throws IOException {
        this.port = port;
        this.serverSocket = new ServerSocket(port);
    }

    public Socket acceptConnection() throws IOException {
        return serverSocket.accept();
    }

    public void closeConnection() throws IOException {
        serverSocket.close();
    }
}
