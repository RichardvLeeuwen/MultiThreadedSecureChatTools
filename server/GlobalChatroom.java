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
        String[] messageParts = command.split(":",2);
        if(messageParts[1].startsWith(" /")) { 
            String[] commandString = messageParts[1].split(" ", 4); //best stripped but works fine for now
            if(commandString[1].equals("/users")) { //considering a switch with functions for later
                DataOutputStream stream = outputStreams.get(messageParts[0]);
                try {
                    stream.writeUTF(getUserNames());
                    stream.flush();
                    return;
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
                        return;
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
                        return;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    stream.writeUTF(command);
                    stream.flush();
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        broadcastMessage(command, messageParts[0]);
    }


}
