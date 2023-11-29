package server;

import java.io.*;
import java.net.Socket;
import java.util.List;
import helper.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class LoginHandler implements Runnable {
   
    private List<Client> allClients;
    private ConcurrentLinkedQueue<Socket> loginQueue;

    LoginHandler( ConcurrentLinkedQueue<Socket> loginQueue, List<Client> allClients) {
        this.loginQueue = loginQueue;
        this.allClients = allClients;
    }

    @Override
    public void run() { //could change use of sockets into socket channels but SSL engine with socket channels sucks
        System.out.println("Booting up login thread");
        while(true) {
            Socket newClientSocket = loginQueue.poll();
            if(newClientSocket == null) {
                continue;
            }
            System.out.println("New user arrived");
            
            try {
                newClientSocket.setSoTimeout(30000); //to prevent the login stalling other clients
                DataOutputStream newClientOutputStream = new DataOutputStream(newClientSocket.getOutputStream());
                DataInputStream newClientInputStream = new DataInputStream(newClientSocket.getInputStream());
                newClientOutputStream.writeUTF("Please provider username:");
                newClientOutputStream.flush();
                String username = (String)newClientInputStream.readUTF();
                if(username!=null) {
                    Client newClient = new Client(username, newClientSocket);
                    newClientSocket.setSoTimeout(0); //back to 0 for smooth client operations
                    synchronized(allClients) {
                        allClients.add(newClient);
                    }
                    newClientOutputStream.writeUTF(username + " has logged in");
                }
            } catch (IOException e) {
                e.printStackTrace();
                if(!newClientSocket.isClosed()) {
                    try {
                        DataOutputStream newClientOutputStream = new DataOutputStream(newClientSocket.getOutputStream());
                        newClientOutputStream.writeUTF("Login failed, possibly took too long, and connection closed, please try again.");
                        newClientOutputStream.flush();
                        newClientSocket.close();
                    }catch (IOException e1) {
                        //e1.printStackTrace();
                    }
                }
            }
        }
        
    }
}
