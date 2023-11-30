package server;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import helper.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class GlobalChatroom extends Chatroom {


    GlobalChatroom(String name, List<Client> allClients) {
        super(name, allClients);
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


}
