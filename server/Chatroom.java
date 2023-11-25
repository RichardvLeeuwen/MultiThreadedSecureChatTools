import java.io.*;  
import java.net.*;
import java.util.List;  


public class Chatroom implements Runnable {
   
    private String name;
    List<Socket> globalClients;

    Chatroom(String name, List<Socket> globalClients) {
        this.name = name;
        this.globalClients = globalClients;
    }

    @Override
    public void run() {
        System.out.println("Booting up chatroom "+name);
        Socket clientSocket;
        
        while(true){
            synchronized(globalClients) {
            if(!globalClients.isEmpty()) {
                break;
            }
        }
        }
        clientSocket = globalClients.get(0);
        
        
        try {
            DataInputStream serverInputStream;
            serverInputStream = new DataInputStream(clientSocket.getInputStream());
        
            String clientGreeting;
            
            clientGreeting = (String)serverInputStream.readUTF();
            System.out.println(clientGreeting);

            DataOutputStream serverOutputStream = new DataOutputStream(clientSocket.getOutputStream());
            serverOutputStream.writeUTF("Server says hello back");
            serverOutputStream.flush();


            serverOutputStream.flush();
            serverOutputStream.close();
            serverInputStream.close();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        
    }


}
