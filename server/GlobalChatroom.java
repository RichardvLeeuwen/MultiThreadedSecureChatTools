package server;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import helper.*;

public class GlobalChatroom extends Chatroom { // global chatroom allows for the creation of new chatrooms and joining
                                               // chatrooms.

    private HashMap<String, Thread> chatroomThreads;
    private HashMap<String, List<Client>> chatroomClientLists;

    GlobalChatroom(String name, List<Client> allClients) {
        super(name, allClients);
        this.chatroomThreads = new HashMap<String, Thread>();
        this.chatroomClientLists = new HashMap<String, List<Client>>();
    }

    public String getChatroomNames() {
        String allChatroomNames = String.join(", ", chatroomThreads.keySet());
        return allChatroomNames;
    }

    private void updateActiveChatroomsList() {
        List<String> toBeRemovedNames = new ArrayList<String>();
        for (String name : chatroomThreads.keySet()) {
            Thread thread = chatroomThreads.get(name);
            if (!thread.isAlive()) {
                toBeRemovedNames.add(name);
            }
        }

        for (String name : toBeRemovedNames) { // removal
            chatroomThreads.remove(name);
            chatroomClientLists.remove(name);
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
        List<Client> chatroomClients = new ArrayList<Client>(); // create the chatroom
        chatroomClientLists.put(chatroomName, chatroomClients);
        Thread newChatroomThread = new Thread(new Chatroom(chatroomName, chatroomClients, allClients));
        newChatroomThread.start();

        chatroomThreads.put(chatroomName, newChatroomThread);

        Client movingClient = returnClientFromName(senderName); // find the asking client and move it to the new
                                                                // chatroom

        if (movingClient == null) {
            System.out.println("Client " + senderName + " does not exist");
            return;
        }
        broadcastMessage(senderName + " has left chatroom " + this.name, senderName);
        Thread oldThread = userThreads.get(senderName); // remove client old list
        oldThread.interrupt(); 
        userThreads.remove(senderName);
        outputStreams.remove(senderName);
        synchronized (allClients) {
            allClients.remove(movingClient);
        }
        synchronized (chatroomClients) {
            chatroomClients.add(movingClient);
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
                case "/createchatroom":
                    executeCreateChatroomCommand(commandParts, splitCommand[0]);
                    return;
                case "/joinchatroom":
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
