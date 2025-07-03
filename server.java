import java.net.*;
import java.io.*;

public class Server {

    private static ServerSocket serverSocket;
    private static Socket clientSocket;
    private static BufferedReader in;
    // private static String text = "";

    public static void main(String[] args) {
        String text = "";
        System.out.println("Middleware assignment");
        if(args.length < 1) {
            System.out.println("Please use the proper format (java Server port_number)");
            System.exit(1);
        }

        int portNumber = Integer.parseInt(args[0]);
        System.out.println("Entered port number: " + portNumber);

        try {
            serverSocket = new ServerSocket(portNumber);
            if(serverSocket != null) {
                System.out.println("Server is running in port " + portNumber);
            }
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
            System.exit(1);
        }

        try {
            clientSocket = serverSocket.accept();
            if(clientSocket != null) {
                System.out.println("Client connected");
            }
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
            System.exit(1);
        }


        // now we need to read the data from client to server
        // by using input stream we get the raw byte data from the client side
        // then we need to convert that byte stream to text, so we have the char
        // buffer reader then gives us the option to read line by line using readLine method

        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
            System.exit(1);
        }

        try {
            while((text = in.readLine()) != null) {
                System.out.println("Client: " + text);
            }
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }


        try {
            clientSocket.close();
            System.out.println("Connection is now closed!");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        
    }
}




// REFERENCES
// https://www.baeldung.com/a-guide-to-java-sockets - for java sockets