package edu.duke.ece568.hw4.server;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

public class ServerWorker implements Runnable {

    private final Socket clientSocket;

    public ServerWorker(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    public void sendResponse(String response) throws IOException {
        DataOutputStream dataOut = new DataOutputStream(clientSocket.getOutputStream());
        dataOut.writeUTF(response);
    }

    public String recvRequest() throws IOException {
        DataInputStream dataIn = new DataInputStream(clientSocket.getInputStream());
        return dataIn.readUTF();
    }

    @Override
    public void run() {
        try {
//            JDBCManager jdbcManager = new JDBCManager("localhost", "5432", "ece568_hw4", "postgres", "postgres");
            JDBCManager jdbcManager = new JDBCManager("db", "5432", "postgres", "postgres", "P@ssw0rd!");
            String request = recvRequest();
            System.out.println("Request is: " + request);
            XMLParser parser = new XMLParser(request, jdbcManager);
            String response = parser.parseRequest();
            sendResponse(response);
            System.out.println("Response is: " + response);
            jdbcManager.getConnection().close();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}