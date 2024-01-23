package server;


import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;


import javax.net.ssl.SSLSocket;


import helper.*;

public class ServerApp {
    private static final int PORT = 5000;
    private static final String KEYSTOREPATH = "C:\\Users\\Richard\\Desktop\\MultiThreadedSecureChatTools\\RichChatServerKeyStore.jks";
    private static final char[] PASSWORD = "password".toCharArray();
    public static void main(String[] args) throws Exception {

        System.out.println("Booting up server");
        ServerSocket serverSocket = TLSWrapper.returnTLSServerSocket(KEYSTOREPATH, PASSWORD, PORT);
        if (serverSocket == null) return; 
        List<Client> newClients =  new ArrayList<Client> ();
        Thread globalChatroomThread = new Thread(new GlobalChatroom("Global", newClients));
        globalChatroomThread.start();

        ConcurrentLinkedQueue<SSLSocket> loginQueue = new ConcurrentLinkedQueue<SSLSocket>();
        Thread loginThread = new Thread(new LoginHandler(loginQueue, newClients));
        loginThread.start();


        while(true) {
            SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
            loginQueue.offer(clientSocket);
            
        }

        //serverSocket.close(); todo, for when properly closing server
    }
}
