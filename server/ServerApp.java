import java.net.*;
import java.util.ArrayList;
import java.util.List;  


public class ServerApp {
    private static final int PORT = 5000;
   
    public static void main(String[] args) throws Exception {
        System.out.println("Booting up server");
        ServerSocket serverSocket = new ServerSocket(PORT);

        List<Socket> globalClients =  new ArrayList<Socket> ();
        Thread globalChatroomThread = new Thread(new Chatroom("Global", globalClients));
        globalChatroomThread.start();
        Socket clientSocket = serverSocket.accept();
        synchronized(globalClients) {
            globalClients.add(clientSocket);
        }
        System.out.println("Added new client to chatroom Global");
        
        

        serverSocket.close();
    }
}
