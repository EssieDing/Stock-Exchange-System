package edu.duke.ece568.hw4.client;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Client {
    private final Socket clientSocket;

    public Client(String ip, int port) throws IOException {
        clientSocket = new Socket(ip, port);
    }

    public Socket getClientSocket() {
        return clientSocket;
    }

    public void sendRequest(String request) throws IOException {
        DataOutputStream dataOut = new DataOutputStream(clientSocket.getOutputStream());
        dataOut.writeUTF(request); // send a string to the client
    }

    public String recvResponse() throws IOException {
        DataInputStream dataIn = new DataInputStream(clientSocket.getInputStream());
        return dataIn.readUTF();
    }
}
