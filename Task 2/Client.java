import java.net.*;
import java.io.*;
import java.util.Scanner;

public class Client {
    
    // private static final String PUB = "PUBLISHER";
    // private static final String SUB = "SUBSCRIBER";
    // private static final String HELP_CMD = "HELP";
    private static final String TERMINATE_CMD = "TERMINATE";

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java Client server_ip port_number [PUBLISHER|SUBSCRIBER]");
            System.exit(1);
        }

        String ip = args[0];
        int portNumber = Integer.parseInt(args[1]);
        String clientType = args[2].toUpperCase();

        if (!clientType.equals("PUBLISHER") && !clientType.equals("SUBSCRIBER")) {
            System.out.println("Invalid client type. Must be PUBLISHER or SUBSCRIBER");
            System.exit(1);
        }

        System.out.println("Starting " + clientType + " client...");
        System.out.println("Connecting to server at " + ip + ":" + portNumber);

        try {
            Socket clientSocket = new Socket(ip, portNumber);
            
            // to send we are using printwriter
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            out.println(clientType);

            if (clientType.equals("PUBLISHER")) {
                handlePublisher(clientSocket);
            } else {
                handleSubscriber(clientSocket);
            }
        } catch (IOException e) {
            System.out.println("Client error: " + e.getMessage());
        }
    }

    private static void handlePublisher(Socket clientSocket) throws IOException {
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
        // BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        Scanner scanner = new Scanner(System.in);

        // printPublisherHelp();
        System.out.println("\nPublisher Commands:");
        System.out.println("Type any message to publish to all subscribers");
        System.out.println("TERMINATE - Disconnect from server\n");

        while (true) {
            System.out.print("PUBLISHER> ");
            String userInput = scanner.nextLine().trim();

            if (userInput.equalsIgnoreCase(TERMINATE_CMD)) {
                break;
            } 
            // else if (userInput.equalsIgnoreCase(HELP_CMD)) {
            //     printPublisherHelp();
            //     continue;
            // }

            out.println(userInput);
        }

        scanner.close();
        clientSocket.close();
        System.out.println("Publisher disconnected successfully!");
    }

    private static void handleSubscriber(Socket clientSocket) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        System.out.println("Subscriber ready. Waiting for messages...");

        // printSubscriberHelp();
        System.out.println("\nSubscriber Commands:");
        System.out.println("TERMINATE - Disconnect from server");
        System.out.println("All other input is ignored");
        System.out.println("Waiting for messages from publishers...\n");

        // Thread for user commands
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String cmd = scanner.nextLine().trim();
                if (cmd.equalsIgnoreCase(TERMINATE_CMD)) {
                    try {
                        clientSocket.close();
                    } catch (IOException e) {
                        System.out.println("Error closing connection: " + e.getMessage());
                    }
                    break;
                // } else if (cmd.equalsIgnoreCase(HELP_CMD)) {
                //     printSubscriberHelp();
                } else {
                    System.out.println("Unknown command.");
                }
            }
            scanner.close();
        }).start();

        // Main thread for receiving messages
        try {
            String serverResponse;
            while ((serverResponse = in.readLine()) != null) {
                System.out.println("Received: " + serverResponse);
            }
        } catch (IOException e) {
            System.out.println("Disconnected from server");
        }
    }

    // private static void printPublisherHelp() {
    //     System.out.println("\nPublisher Commands:");
    //     System.out.println("Type any message to publish to all subscribers");
    //     System.out.println("TERMINATE - Disconnect from server");
    //     // System.out.println("HELP - Show this help message\n");
    // }

    // private static void printSubscriberHelp() {
    //     System.out.println("\nSubscriber Commands:");
    //     System.out.println("TERMINATE - Disconnect from server");
    //     // System.out.println("HELP - Show this help message");
    //     System.out.println("All other input is ignored");
    //     System.out.println("Waiting for messages from publishers...\n");
    // }
}