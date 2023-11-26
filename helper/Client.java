package helper;

import java.io.*;  
import java.net.*;
import java.util.concurrent.ConcurrentLinkedQueue;  


public class Client implements Runnable {
   
    private String name;
    private Socket socket;
    private ConcurrentLinkedQueue<String> sendQueue;
    private DataInputStream inputStream;

    public Client(String name, Socket socket) {
        this.name = name;
        this.socket = socket;
        try {
            this.inputStream = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public String getName() {
        return this.name;
    }
    public Socket getSocket() {
        return this.socket;
    }
    public void setSendQueue(ConcurrentLinkedQueue<String> sendQueue) {
        this.sendQueue = sendQueue;
    }

    @Override
    public void run() {
        while(true) {
            if (sendQueue == null) {
                break;
            }
            
            
            String clientMessage = null;
            try {
                clientMessage = (String)inputStream.readUTF();
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
            if(clientMessage != null) {
                sendQueue.offer(clientMessage);
            }
        }
    }

    
        
}
