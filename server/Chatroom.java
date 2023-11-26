package server;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import helper.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Chatroom implements Runnable { //with adition of client threads, chatroom thread still needed?
   
    private String name;
    private List<Client> allClients;
    private ConcurrentLinkedQueue<String> commandsQueue;
    private HashMap<String, DataOutputStream> outputStreams;
    private HashMap<String, Thread> clientThreads;

    Chatroom(String name, List<Client> allClients) {
        this.name = name;
        this.allClients = allClients;
        this.commandsQueue = new ConcurrentLinkedQueue<String>();
        this.outputStreams = new HashMap<String, DataOutputStream>();
        this.clientThreads = new HashMap<String, Thread>();
    }

    @Override
    public void run() { //could change use of sockets into socket channels but SSL engine with socket channels sucks
        System.out.println("Booting up chatroom "+name);
        
        while(true){ //wait for initial client
            synchronized(allClients) { //expensive potentially, will look into alternatives such as queue
                if(!allClients.isEmpty()) {
                    break;
                }
            }
        }

        while(true) {
            synchronized(allClients) { //expensive potentially, will look into alternatives
                List<String> toBeRemovedName = new ArrayList<String>();
                List<Client> toBeRemovedClient = new ArrayList<Client>(); //painfully awkward
                for (String name : clientThreads.keySet()) {
                    Thread thread = clientThreads.get(name);
                    if(!thread.isAlive()) {
                        toBeRemovedName.add(name);
                        for (Client client : allClients) {
                            if(client.getName() == name) {
                                toBeRemovedClient.add(client);
                            }
                        }
                    }
                }
                
                for(String name : toBeRemovedName) { //removal
                    clientThreads.remove(name);
                    outputStreams.remove(name);
                }
                for (Client client : toBeRemovedClient) {
                    allClients.remove(client);
                }
                if(allClients.isEmpty()) { //close chatroom once empty except if global
                    if(name != "Global") {
                        System.out.println("Chatroom is empty and killed");
                        break;
                    }
                    else {
                        //System.out.println("Global chatroom is empty");
                    }
                }
                for(Client client: allClients) { //check for new clients, if so add output streams and initialize client thread
                    if (!outputStreams.containsKey(client.getName()) ) {
                        try {
                            System.out.println("New user " + client.getName() + " has entered chatroom" + this.name);
                            outputStreams.put(client.getName(), new DataOutputStream(client.getSocket().getOutputStream()));
                            client.setSendQueue(commandsQueue);
                            Thread newClientThread = new Thread(client);
                            newClientThread.start();
                            clientThreads.put(client.getName(), newClientThread);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            String message = commandsQueue.poll(); //queue can have commands or messages to send
            if(message == null) {
                continue;
            }
            for (DataOutputStream stream : outputStreams.values()) {
                try {
                    stream.writeUTF(message);
                    stream.flush();  
                } catch (IOException e) {
                    e.printStackTrace();
                }  
            }
            message=null;
        }
        
    }


}
