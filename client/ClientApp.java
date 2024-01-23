package client;

import java.io.*;
import java.util.Scanner;
import javax.net.ssl.SSLSocket;


import helper.*;

public class ClientApp {
    private static final int SERVERPORT = 5000;
    private static final String SERVERADDRESS = "127.0.0.1";
    private static final String CERTIFICATEPATH = "C:\\Users\\Richard\\Desktop\\MultiThreadedSecureChatTools\\client\\RichChatServerCertificate.cer";

    public static void main(String[] args) throws Exception {

        System.out.println("Booting up client");
        
        SSLSocket clientSocket = TLSWrapper.returnTLSSocket(CERTIFICATEPATH, SERVERADDRESS, SERVERPORT); //new Socket(serverInetAddress, SERVERPORT);
        if (clientSocket == null) return;
        Client client = new Client("Rich", clientSocket);
        Thread newClientThread = new Thread(client);
        newClientThread.start();

        DataOutputStream clientOutputStream = new DataOutputStream(clientSocket.getOutputStream());
        Scanner consoleInputScanner = new Scanner(System.in);

        while (true) {
            String input = consoleInputScanner.nextLine();

            clientOutputStream.writeUTF(input);
            clientOutputStream.flush();
            if (input.equals("/exit")) {
                newClientThread.interrupt();
                break;
            }
        }
        consoleInputScanner.close();
        //clientOutputStream.close(); unnecessary?
        clientSocket.close();
    }
}
