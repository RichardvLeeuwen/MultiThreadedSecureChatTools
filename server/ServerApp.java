package server;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import helper.*;

public class ServerApp {
    private static final int PORT = 5000;
   
    public static void main(String[] args) throws Exception {
        System.out.println("Booting up server");
        ServerSocket serverSocket = new ServerSocket(PORT);

        

        List<Client> newClients =  new ArrayList<Client> ();
        Thread globalChatroomThread = new Thread(new GlobalChatroom("Global", newClients));
        globalChatroomThread.start();


        ConcurrentLinkedQueue<Socket> loginQueue = new ConcurrentLinkedQueue<Socket>();
        Thread loginThread = new Thread(new LoginHandler(loginQueue, newClients));
        loginThread.start();



        while(true) {
            Socket clientSocket = serverSocket.accept();
            loginQueue.offer(clientSocket);
            
        }

        //serverSocket.close(); todo, for when properly closing server
    }
}
