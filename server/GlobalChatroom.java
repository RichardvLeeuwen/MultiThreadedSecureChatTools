package server;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import helper.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class GlobalChatroom extends Chatroom { //global chatroom allows for the creation of new chatrooms and joining chatrooms. 

    private HashMap<String, Thread> chatroomThreads;

    GlobalChatroom(String name, List<Client> allClients) {
        super(name, allClients);
        this.chatroomThreads = new HashMap<String, Thread>();
    }

    private void updateActiveChatroomsList() { //todo update active chatroom list

    }

    @Override
    protected void processCommand(String command) {
        String[] splitCommand = command.split(":",2);
        if(splitCommand[1].startsWith(" /")) { 
            String[] commandParts = splitCommand[1].split(" ", 4); //best stripped but works fine for now
            switch(commandParts[1]) {
                case "/users":
                    executeUsersCommand(splitCommand[0]);
                    return;
                case "/whisper":
                    executeWhisperCommand(commandParts, splitCommand[0], commandParts[2], command);
                    return;
                case "/createChatroom":
                    return;
                case "/joinChatroom":
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
        
        while(true) {
            updateActiveClientList();
            updateActiveChatroomsList();
            String command = commandsQueue.poll(); //queue can have commands or messages to send
            if(command == null) continue;
 
            processCommand(command);
        }
        
    }


}
