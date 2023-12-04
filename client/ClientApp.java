package client;

import java.io.*;  
import java.net.*;
import java.util.Scanner;

import helper.*;

public class ClientApp {
    private static final int SERVERPORT = 5000;
    private static final String SERVERADDRESS = "127.0.0.1";

    public static void main(String[] args) throws Exception {
        System.out.println("Booting up client");
        InetAddress serverInetAddress = InetAddress.getByName(SERVERADDRESS);
        Socket clientSocket = new Socket(serverInetAddress, SERVERPORT);

    
        Client client = new Client("Rich", clientSocket);
        Thread newClientThread = new Thread(client);
        newClientThread.start();

        DataOutputStream clientOutputStream = new DataOutputStream(clientSocket.getOutputStream());
        Scanner consoleInputScanner = new Scanner(System.in);

        while(true) {
            String input = consoleInputScanner.nextLine();
            
            clientOutputStream.writeUTF(input);
            clientOutputStream.flush();
            if(input.equals("/leave")) {
                newClientThread.stop();
                break;
            }
        }
        consoleInputScanner.close(); 
        clientOutputStream.flush();
        clientOutputStream.close();
        clientSocket.close();
    }
}
