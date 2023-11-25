import java.io.*;  
import java.net.*;  


public class ServerApp {
    private static final int PORT = 5000;
   
    public static void main(String[] args) throws Exception {
        System.out.println("Booting up server");
        ServerSocket serverSocket = new ServerSocket(PORT);
        Socket clientSocket = serverSocket.accept();

        DataInputStream serverInputStream = new DataInputStream(clientSocket.getInputStream());  
        String clientGreeting = (String)serverInputStream.readUTF();  
        System.out.println(clientGreeting);

        DataOutputStream serverOutputStream = new DataOutputStream(clientSocket.getOutputStream());
        serverOutputStream.writeUTF("Server says hello back");
        serverOutputStream.flush();


        serverOutputStream.flush();
        serverOutputStream.close();
        serverInputStream.close();
        clientSocket.close();
        serverSocket.close();

    }
}
