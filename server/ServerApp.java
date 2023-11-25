package server;
import java.net.*;
import java.util.ArrayList;
import java.util.List;  
import helper.*;

public class ServerApp {
    private static final int PORT = 5000;
   
    public static void main(String[] args) throws Exception {
        System.out.println("Booting up server");
        ServerSocket serverSocket = new ServerSocket(PORT);

        List<Client> globalClients =  new ArrayList<Client> ();
        Thread globalChatroomThread = new Thread(new Chatroom("Global", globalClients));
        globalChatroomThread.start();
        int counter = 0;
        while(true) {
            Socket clientSocket = serverSocket.accept();
            Client clientOne = new Client(String.valueOf(counter), clientSocket);
            synchronized(globalClients) {
                globalClients.add(clientOne);
            }
            System.out.println("Added new client "+ String.valueOf(counter) +" to chatroom Global");
            counter++;
            
            // clientSocket = serverSocket.accept();
            // Client clientTwo = new Client("two", clientSocket);
            // synchronized(globalClients) {
            //     globalClients.add(clientTwo);
            // }
            // System.out.println("Added new client to chatroom Global");
        }

        //serverSocket.close();
    }
}
