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
    protected HashMap<String, Thread> userThreads;
    protected GlobalChatroom parentChatroom; // allows a child chatroom to return clients to parent
                                          // chatroom

    Chatroom(String name, List<Client> allClients) {
        this.name = name;
        this.allClients = allClients;
        this.commandsQueue = new ConcurrentLinkedQueue<String>();
        this.outputStreams = new HashMap<String, DataOutputStream>();
        this.userThreads = new HashMap<String, Thread>();
        this.parentChatroom = null;
    }

    Chatroom(String name, List<Client> allClients, GlobalChatroom parentChatroom) {
        this.name = name;
        this.allClients = allClients;
        this.commandsQueue = new ConcurrentLinkedQueue<String>();
        this.outputStreams = new HashMap<String, DataOutputStream>();
        this.userThreads = new HashMap<String, Thread>();
        this.parentChatroom = parentChatroom;
    }

    public String getUserNames() {
        String allUserNames;
        synchronized(userThreads) {
            allUserNames = String.join(", ", userThreads.keySet());
        }
        return allUserNames;
    }

    protected Client returnClientFromName(String name) {
        synchronized (allClients) {
            for (Client client : allClients) {
                if (name.equals(client.getName())) {
                    return client;
                }
            }
        }
        return null;
    }

    protected boolean updateActiveClientList() { // returns false if client list empty, true if not
        synchronized (allClients) { // expensive potentially, will look into alternatives
            List<String> toBeRemovedNames = new ArrayList<String>();
            List<Client> toBeRemovedClient = new ArrayList<Client>(); // painfully awkward
            synchronized(userThreads) {
                for (String name : userThreads.keySet()) {
                    Thread thread = userThreads.get(name);
                    if (!thread.isAlive()) {
                        toBeRemovedNames.add(name);
                        for (Client client : allClients) {
                            if (client.getName() == name) {
                                toBeRemovedClient.add(client);
                            }
                        }
                    }
                }
            }

            for (String name : toBeRemovedNames) { // removal
                userThreads.remove(name);
                outputStreams.remove(name);
            }
            for (Client client : toBeRemovedClient) {
                allClients.remove(client);
            }
            if (allClients.isEmpty()) { // close chatroom once empty except if global
                if (name != "Global") {
                    System.out.println("Chatroom is empty and killed");
                    return false;
                }
            }
            for (Client client : allClients) { // check for new uninitialised clients without output stream/client receive thread, if so add output streams and initialize client
                                               // thread
                if (!outputStreams.containsKey(client.getName())) {
                    try {
                        String serverArrivalAnnouncement = "New user " + client.getName() + " has entered chatroom "
                                + this.name;
                        System.out.println(serverArrivalAnnouncement);

                        outputStreams.put(client.getName(), new DataOutputStream(client.getSocket().getOutputStream()));
                        client.setSendQueue(commandsQueue);
                        Thread newClientThread = new Thread(client);
                        newClientThread.start();
                        userThreads.put(client.getName(), newClientThread);

                        for (String nameStream : outputStreams.keySet()) { // announce to all users
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

    protected void broadcastMessage(String message, String messageOwner) { // broadcast except to message owner
        for (String nameStream : outputStreams.keySet()) {
            try {
                if (messageOwner.equals(nameStream)) {
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

    protected void addInitialisedClient(String clientName, Client client, DataOutputStream clientOutputStream, Thread clientThread) {
        synchronized(this.outputStreams) {
            if(this.outputStreams.containsKey(clientName)) return;
            this.outputStreams.put(clientName, clientOutputStream);
        }
        synchronized(this.userThreads) {
            this.userThreads.put(clientName, clientThread);
        }
        synchronized(this.allClients) {
            this.allClients.add(client);
        }
        broadcastMessage(clientName+" has entered chatroom "+this.name, "");
    }

    protected void executeUsersCommand(String senderName) { // send sender a list of all active usernames
        DataOutputStream stream = outputStreams.get(senderName);
        try {
            stream.writeUTF(getUserNames());
            stream.flush();
            return;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void executeWhisperCommand(String[] commandParts, String senderName, String whisperMessage) {
        if (commandParts.length < 3) {
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
        if (stream == null) {
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

    protected void executeLeaveCommand(String userName) { //TODO: rewrite whole function as add and removing client mechanism is different
        Client toBeRemovedClient = returnClientFromName(userName);
        if (toBeRemovedClient == null) {
            System.out.println("Client " + userName + " does not exist");
            return;
        }

        broadcastMessage(userName + " has left chatroom " + this.name, "");

        if (parentChatroom != null){
            toBeRemovedClient.setSendQueue(parentChatroom.commandsQueue);
            parentChatroom.addInitialisedClient(userName, toBeRemovedClient, outputStreams.get(userName), userThreads.get(userName) );
        }

        synchronized(userThreads) {
            userThreads.remove(userName);
        }
        synchronized(outputStreams) {
            outputStreams.remove(userName);
        }
        synchronized (allClients) {
            allClients.remove(toBeRemovedClient);
        }


        


    }

    protected void processCommand(String command) {
        String[] splitCommand = command.split(":", 2);
        if (splitCommand[1].startsWith(" /")) {
            String[] commandParts = splitCommand[1].split(" ", 4); // best stripped but works fine for now
            switch (commandParts[1]) {
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
        } else {
            broadcastMessage(command, splitCommand[0]);
        }
    }

    @Override
    public void run() { // could change use of sockets into socket channels but SSL engine with socket
                        // channels sucks
        System.out.println("Booting up chatroom " + name);

        while (true) { // wait for initial client
            synchronized (allClients) { // expensive potentially, will look into alternatives such as queue
                if (!allClients.isEmpty()) {
                    break;
                }
            }
        }

        while (true) {
            if (!updateActiveClientList())
                break;
            String command = null;
            try {
                command = commandsQueue.poll(); // queue can have commands or messages to send
            } catch (Exception e) {
                e.printStackTrace();
                // TODO: handle exception
            }
            
            if (command == null)
                continue;

            processCommand(command);
        }

    }

}
