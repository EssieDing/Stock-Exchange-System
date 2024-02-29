package edu.duke.ece568.hw4.client;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientWorker implements Runnable {
    private Socket clientSocket;

    public ClientWorker(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    private void sendRequest(String request) throws IOException {
        DataOutputStream dataOut = new DataOutputStream(clientSocket.getOutputStream());
        dataOut.writeUTF(request);
    }

    private String recvResponse() throws IOException {
        DataInputStream dataIn = new DataInputStream(clientSocket.getInputStream());
        return dataIn.readUTF();
    }

    @Override
    public void run() {
        for (int i = 0; i < 1; i++) {
            try {
                String request = "123<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><create><account id=\"12714\" balance=\"98349213\"/></create>";
                sendRequest(request);
                String response = recvResponse();
                System.out.println(response);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
