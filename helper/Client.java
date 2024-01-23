package helper;

import java.io.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.net.ssl.SSLSocket;

public class Client implements Runnable {

    private String name;
    private SSLSocket socket;
    private ConcurrentLinkedQueue<String> sendQueue;
    private DataInputStream inputStream;
    private Boolean printOut; // by default outputs to command line

    public Client(String name, SSLSocket socket) {
        this.name = name;
        this.socket = socket;
        this.printOut = true;
        try {
            this.inputStream = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getName() {
        return this.name;
    }

    public SSLSocket getSocket() {
        return this.socket;
    }

    public void setSendQueue(ConcurrentLinkedQueue<String> sendQueue) { // when set, messages get forwarded to the send
                                                                        // queue
        this.sendQueue = sendQueue;
        this.printOut = false;
    }

    @Override
    public void run() { // implement elegant way of killing the thread
        while (true) {
            if (sendQueue == null && !printOut) {
                break;
            }

            String clientMessage = null;
            try {
                //System.out.println("before");
                clientMessage = (String) inputStream.readUTF();
                //System.out.println("after");
            } catch (IOException e) {
                e.printStackTrace(); // consider logging and catching different exceptions, maybe own class?
                break;
            }
            if (clientMessage != null) {
                String nameAppendedMessage = name + ": " + clientMessage;
                if (printOut) {
                    System.out.println(clientMessage);
                    continue;
                }
                try {
                    sendQueue.offer(nameAppendedMessage);
                } catch (Exception e) {
                    e.printStackTrace(); 
                    // TODO: handle exception
                }
                
            }
        }
    }
}
