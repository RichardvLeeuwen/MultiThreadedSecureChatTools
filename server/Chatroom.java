package server;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import helper.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Chatroom implements Runnable {
   
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

    public String getUserNames() {
        String allUserNames = String.join(", ", clientThreads.keySet());
        return allUserNames;
    }

    private boolean updateActiveClientList() { //returns false if client list empty, true if not
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
                    return false;
                }
            }
            for(Client client: allClients) { //check for new clients, if so add output streams and initialize client thread
                if (!outputStreams.containsKey(client.getName()) ) {
                    try {
                        String serverArrivalAnnouncement = "New user " + client.getName() + " has entered chatroom " + this.name;
                        System.out.println(serverArrivalAnnouncement);
                        outputStreams.put(client.getName(), new DataOutputStream(client.getSocket().getOutputStream()));
                        client.setSendQueue(commandsQueue);
                        Thread newClientThread = new Thread(client);
                        newClientThread.start();
                        clientThreads.put(client.getName(), newClientThread);

                        for (String nameStream : outputStreams.keySet()) { //announce to all users
                            DataOutputStream stream = outputStreams.get(nameStream);
                            stream.writeUTF(serverArrivalAnnouncement);
                            stream.flush();  
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return true;
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
            if (!updateActiveClientList()) break;
            
            String message = commandsQueue.poll(); //queue can have commands or messages to send
            if(message == null) {
                continue;
            }
            String[] messageParts = message.split(":",2);
            if(messageParts[1].startsWith(" /")) { 
                String[] commandString = messageParts[1].split(" ", 4); //best stripped but works fine for now
                if(commandString[1].equals("/users")) { //considering a switch with functions for later
                    DataOutputStream stream = outputStreams.get(messageParts[0]);
                    try {
                        stream.writeUTF(getUserNames());
                        stream.flush();
                         message=null;
                         continue;
                    } catch (IOException e) {
                        e.printStackTrace();
                    } 
                }
                if(commandString[1].equals("/whisper")) { //TODO: clean up error checking and modulise into functions
                    if(commandString.length < 3) {
                        DataOutputStream stream = outputStreams.get(messageParts[0]);
                        try {
                            stream.writeUTF("Please specify whisper target");
                            stream.flush();
                            message=null;
                            continue;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    DataOutputStream stream = outputStreams.get(commandString[2]);
                    if(stream == null) {
                        stream = outputStreams.get(messageParts[0]);
                        try {
                            stream.writeUTF("Invalid target");
                            stream.flush();
                            message=null;
                            continue;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        stream.writeUTF(message);
                        stream.flush();
                        message=null;
                         continue;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    }
            }

            for (String nameStream : outputStreams.keySet()) {
                try {
                    if(messageParts[0].equals(nameStream)) {
                        continue;
                    }
                    DataOutputStream stream = outputStreams.get(nameStream);
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
