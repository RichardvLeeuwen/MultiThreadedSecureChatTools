package server;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import helper.*;

public class GlobalChatroom extends Chatroom { // global chatroom allows for the creation of new chatrooms and joining
                                               // chatrooms.

    private HashMap<String, Thread> childChatroomThreads;
    private HashMap<String, List<Client>> childChatroomClientLists;
    private HashMap<String, Chatroom> childChatrooms;

    GlobalChatroom(String name, List<Client> allClients) {
        super(name, allClients);
        this.childChatroomThreads = new HashMap<String, Thread>();
        this.childChatroomClientLists = new HashMap<String, List<Client>>();
        this.childChatrooms = new HashMap<String, Chatroom>();
    }

    public String getChatroomNames() {
        String allChatroomNames = String.join(", ", childChatroomThreads.keySet());
        return allChatroomNames;
    }

    private void updateActiveChatroomsList() {
        List<String> toBeRemovedNames = new ArrayList<String>();
        for (String name : childChatroomThreads.keySet()) {
            Thread thread = childChatroomThreads.get(name);
            if (!thread.isAlive()) {
                toBeRemovedNames.add(name);
            }
        }

        for (String name : toBeRemovedNames) { // removal
            childChatroomThreads.remove(name);
            childChatroomClientLists.remove(name);
        }
    }

    private void executeChatroomsCommand(String senderName) { // send sender a list of all active usernames
        DataOutputStream stream = outputStreams.get(senderName);
        try {
            stream.writeUTF(getChatroomNames());
            stream.flush();
            return;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void executeCreateChatroomCommand(String[] commandParts, String senderName) {
        if (commandParts.length < 3) { // todo check for duplicates
            DataOutputStream stream = outputStreams.get(senderName);
            try {
                stream.writeUTF("Please specify chatroom name");
                stream.flush();
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        String chatroomName = commandParts[2];
        if (chatroomName == "Global") { // todo check for duplicates
            DataOutputStream stream = outputStreams.get(senderName);
            try {
                stream.writeUTF("Global is not available");
                stream.flush();
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        List<Client> chatroomClients = new ArrayList<Client>(); // create the chatroom
        childChatroomClientLists.put(chatroomName, chatroomClients);
        Chatroom newChatroom = new Chatroom(chatroomName, chatroomClients, this); //put this in its own hashmap for later use
        this.childChatrooms.put(chatroomName, newChatroom);
        Thread newChatroomThread = new Thread(newChatroom);
        newChatroomThread.start();

        childChatroomThreads.put(chatroomName, newChatroomThread);

        Client movingClient = returnClientFromName(senderName); // find the asking client and move it to the new
                                                                // chatroom

        if (movingClient == null) {
            System.out.println("Client " + senderName + " does not exist");
            return;
        }
        broadcastMessage(senderName + " has left chatroom " + this.name, senderName);

        movingClient.setSendQueue(newChatroom.commandsQueue);
        newChatroom.addInitialisedClient(senderName, movingClient ,outputStreams.get(senderName), userThreads.get(senderName));
        userThreads.remove(senderName);
        outputStreams.remove(senderName);
        synchronized (allClients) {
            allClients.remove(movingClient);
        }
    }


    private void executeJoinChatroomCommand(String[] commandParts, String senderName) {
        if (commandParts.length < 3) {
            DataOutputStream stream = outputStreams.get(senderName);
            try {
                stream.writeUTF("Please specify chatroom name");
                stream.flush();
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        String chatroomName = commandParts[2];
        Chatroom newChatroom = this.childChatrooms.get(chatroomName);
        
        Client movingClient = returnClientFromName(senderName); // find the asking client and move it to the new
                                                                // chatroom

        if (movingClient == null) {
            System.out.println("Client " + senderName + " does not exist");
            return;
        }
        broadcastMessage(senderName + " has left chatroom " + this.name, senderName);

        movingClient.setSendQueue(newChatroom.commandsQueue);
        newChatroom.addInitialisedClient(senderName, movingClient ,outputStreams.get(senderName), userThreads.get(senderName));
        userThreads.remove(senderName);
        outputStreams.remove(senderName);
        synchronized (allClients) {
            allClients.remove(movingClient);
        }
    }

    @Override
    protected void processCommand(String command) {
        String[] splitCommand = command.split(":", 2);
        if (splitCommand[1].startsWith(" /")) {
            String[] commandParts = splitCommand[1].split(" ", 4); // best stripped but works fine for now
            switch (commandParts[1]) {
                case "/users":
                    executeUsersCommand(splitCommand[0]);
                    return;
                case "/chatrooms":
                    executeChatroomsCommand(splitCommand[0]);
                    return;
                case "/whisper":
                    executeWhisperCommand(commandParts, splitCommand[0], command);
                    return;
                case "/create":
                    executeCreateChatroomCommand(commandParts, splitCommand[0]);
                    return;
                case "/join":
                    executeJoinChatroomCommand(commandParts, splitCommand[0]);
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

        while (true) {
            updateActiveClientList();
            updateActiveChatroomsList();
            String command = commandsQueue.poll(); // queue can have commands or messages to send
            if (command == null)
                continue;

            processCommand(command);
        }

    }

}
