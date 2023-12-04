package server;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import helper.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Chatroom implements Runnable {
   
    protected String name;
    protected List<Client> allClients;
    protected ConcurrentLinkedQueue<String> commandsQueue;
    protected HashMap<String, DataOutputStream> outputStreams;
    protected HashMap<String, Thread> clientThreads;
    protected List<Client> parentClients; //placeholder name, allows a child chatroom to return clients to parents chatroom

    Chatroom(String name, List<Client> allClients) {
        this.name = name;
        this.allClients = allClients;
        this.commandsQueue = new ConcurrentLinkedQueue<String>();
        this.outputStreams = new HashMap<String, DataOutputStream>();
        this.clientThreads = new HashMap<String, Thread>();
        this.parentClients = null;
    }

    Chatroom(String name, List<Client> allClients, List<Client> parentClients) {
        this.name = name;
        this.allClients = allClients;
        this.commandsQueue = new ConcurrentLinkedQueue<String>();
        this.outputStreams = new HashMap<String, DataOutputStream>();
        this.clientThreads = new HashMap<String, Thread>();
        this.parentClients = parentClients;
    }

    public String getUserNames() {
        String allUserNames = String.join(", ", clientThreads.keySet());
        return allUserNames;
    }

    protected Client returnClientFromName(String name) {
        synchronized(allClients) {
            for(Client client : allClients) {
                if(name.equals(client.getName())) {
                    return client;
                }
            }
        }
        return null;
    }

    protected boolean updateActiveClientList() { //returns false if client list empty, true if not
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

    protected void broadcastMessage(String message, String messageOwner) { //broadcast except to message owner
        for (String nameStream : outputStreams.keySet()) {
            try {
                if(messageOwner.equals(nameStream)) {
                    continue;
                }
                DataOutputStream stream = outputStreams.get(nameStream);
                stream.writeUTF(message);
                stream.flush();  
            } catch (IOException e) {
                e.printStackTrace();
            }  
        }
    }

    protected void executeUsersCommand(String senderName) { //send sender a list of all active usernames
        DataOutputStream stream = outputStreams.get(senderName);
        try {
            stream.writeUTF(getUserNames());
            stream.flush();
            return;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    protected void executeWhisperCommand(String[] commandParts, String senderName,  String whisperMessage) {
        if(commandParts.length < 3 ) {
            DataOutputStream stream = outputStreams.get(senderName);
            try {
                stream.writeUTF("Please specify whisper target");
                stream.flush();
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        String targetName = commandParts[2];
        DataOutputStream stream = outputStreams.get(targetName);
        if(stream == null) {
            stream = outputStreams.get(senderName);
            try {
                stream.writeUTF("Invalid target");
                stream.flush();
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            stream.writeUTF(whisperMessage);
            stream.flush();
            return;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void executeLeaveCommand(String userName) {
        Client toBeRemovedClient = returnClientFromName(userName);
        if(toBeRemovedClient  == null) {
            System.out.println("Client " + userName + " does not exist");
            return;
        }
 
        Thread oldThread = clientThreads.get(userName); //remove client old list
        oldThread.stop(); //deprecated, need to find saver way
        clientThreads.remove(userName);
        outputStreams.remove(userName);
        synchronized(allClients) {
            allClients.remove(toBeRemovedClient);
        }
        if(parentClients == null) return;
        synchronized(parentClients) { //return to parent chatroom
            parentClients.add(toBeRemovedClient);
        }

    }
    
    
    protected void processCommand(String command) {
        String[] splitCommand = command.split(":",2);
        if(splitCommand[1].startsWith(" /")) { 
            String[] commandParts = splitCommand[1].split(" ", 4); //best stripped but works fine for now
            switch(commandParts[1]) {
                case "/users":
                    executeUsersCommand(splitCommand[0]);
                    return;
                case "/whisper":
                    executeWhisperCommand(commandParts, splitCommand[0], command);
                    return;
                case "/leave":
                    executeLeaveCommand(splitCommand[0]);
                    return;
                default:
                    DataOutputStream stream = outputStreams.get(splitCommand[0]);
                    try {
                        stream.writeUTF("Invalid command");
                        stream.flush();
                        return;
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }
            }
        }
        else {
            broadcastMessage(command, splitCommand[0]);
        }
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

            String command = commandsQueue.poll(); //queue can have commands or messages to send
            if(command == null) continue;
 
            processCommand(command);
        }
        
    }


}
