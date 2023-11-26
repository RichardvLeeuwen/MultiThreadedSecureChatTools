package client;

import java.io.*;  
import java.net.*;  
import helper.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ClientApp {
    private static final int SERVERPORT = 5000;
    private static final String SERVERADDRESS = "127.0.0.1";
    public static void main(String[] args) throws Exception {
        System.out.println("Booting up client");
        InetAddress serverInetAddress = InetAddress.getByName(SERVERADDRESS);
        Socket clientSocket = new Socket(serverInetAddress, SERVERPORT);

        Client client = new Client("Rich", clientSocket);
        
        DataOutputStream clientOutputStream = new DataOutputStream(clientSocket.getOutputStream());
        clientOutputStream.writeUTF("Client says hello");
        clientOutputStream.flush();

        DataInputStream clientInputStream = new DataInputStream(clientSocket.getInputStream());  
        String serverGreeting = (String)clientInputStream.readUTF();  
        System.out.println(serverGreeting);
        serverGreeting = (String)clientInputStream.readUTF();  
        System.out.println(serverGreeting);


        clientOutputStream.flush();
        clientOutputStream.close();
        clientSocket.close();
    }
}
