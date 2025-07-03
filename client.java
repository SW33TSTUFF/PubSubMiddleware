import java.net.*;
import java.io.*;


public class Client {

    private static Socket clientSocket;
    private static PrintWriter out;
    private static BufferedReader input;

    public static void main(String[] args) {
        System.out.println("Hello client");
    

    if(args.length != 2) {
        System.out.println("You have to enter the ip of the server and the port number as arguments");
        System.exit(1);
    }

    try {
        String ip = args[0];
        int portNumber = Integer.parseInt(args[1]);

        clientSocket = new Socket(ip, portNumber);
        System.out.println("Connected to the server!");
    } catch(Exception e) {
        System.out.println(e.getMessage());
    }

}
}