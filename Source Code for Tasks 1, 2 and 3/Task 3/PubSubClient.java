import java.io.*;
import java.net.*;
import java.util.Scanner;

public class PubSubClient {
    private String host;
    private int port;
    private String clientType;
    private String topic;
    private Socket socket;
    private volatile boolean running = true;
    
    public PubSubClient(String host, int port, String clientType, String topic) {
        this.host = host;
        this.port = port;
        this.clientType = clientType.toUpperCase();
        this.topic = topic;
    }
    
    // Thread class to receive messages from server (for subscribers)
    private class MessageReceiver extends Thread {
        private BufferedReader in;
        
        public MessageReceiver(BufferedReader in) {
            this.in = in;
            this.setDaemon(true);
        }
        
        @Override
        public void run() {
            while (running) {
                try {
                    String message = in.readLine();
                    if (message == null) {
                        break;
                    }
                    
                    message = message.trim();
                    System.out.println("\n" + message);
                    System.out.print("> ");
                    System.out.flush();
                    
                } catch (IOException e) {
                    if (running) {
                        System.out.println("\nServer disconnected");
                    }
                    break;
                }
            }
        }
    }
    
    public void connectAndCommunicate() {
        try {
            socket = new Socket(host, port);
            
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            Scanner scanner = new Scanner(System.in);
            
            // Connect to server
            System.out.println("Connecting to server " + host + ":" + port + " as " + clientType + " for topic '" + topic + "'...");
            System.out.println("Connected to server!");
            
            // Send client type and topic to server (format: "PUBLISHER:TOPIC_A")
            String clientInfo = clientType + ":" + topic;
            out.println(clientInfo);
            
            // Start receiving thread for subscribers
            if (clientType.equals("SUBSCRIBER")) {
                MessageReceiver receiver = new MessageReceiver(in);
                receiver.start();
                System.out.println("Listening for messages on topic '" + topic + "'...");
            }
            
            if (clientType.equals("PUBLISHER")) {
                System.out.println("Ready to publish messages on topic '" + topic + "'");
            }
            
            System.out.println("Type your messages (type 'terminate' to quit):");
            
            while (running) {
                try {
                    // Get user input
                    String prompt = clientType.equals("PUBLISHER") ? "[" + topic + "] > " : "> ";
                    System.out.print(prompt);
                    
                    String message = scanner.nextLine();
                    
                    // Send message to server
                    out.println(message);
                    
                    // Check if user wants to terminate
                    if (message.toLowerCase().equals("terminate")) {
                        System.out.println("Terminating connection...");
                        running = false;
                        break;
                    }
                    
                } catch (Exception e) {
                    System.out.println("Error sending message: " + e.getMessage());
                    break;
                }
            }
            
            scanner.close();
            
        } catch (ConnectException e) {
            System.out.println("Error: Could not connect to server " + host + ":" + port);
            System.out.println("Make sure the server is running and the address is correct.");
        } catch (IOException e) {
            System.out.println("Socket error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Unexpected error: " + e.getMessage());
        } finally {
            // Close socket
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }
    
    public static void main(String[] args) {
        // Check if Server IP, PORT, CLIENT_TYPE, and TOPIC arguments are provided
        if (args.length != 4) {
            System.out.println("Usage: java PubSubClient <SERVER_IP> <SERVER_PORT> <CLIENT_TYPE> <TOPIC>");
            System.out.println("Example: java PubSubClient 192.168.10.2 5000 PUBLISHER TOPIC_A");
            System.out.println("Example: java PubSubClient 192.168.10.2 5000 SUBSCRIBER TOPIC_A");
            System.out.println("Example: java PubSubClient 127.0.0.1 5000 PUBLISHER SPORTS");
            System.out.println("Example: java PubSubClient 127.0.0.1 5000 SUBSCRIBER SPORTS");
            System.exit(1);
        }
        
        String host = args[0];
        String clientType = args[2];
        String topic = args[3];
        
        int port;
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Error: PORT must be a valid integer");
            System.exit(1);
            return;
        }
        
        if (!clientType.toUpperCase().equals("PUBLISHER") && !clientType.toUpperCase().equals("SUBSCRIBER")) {
            System.out.println("Error: CLIENT_TYPE must be either 'PUBLISHER' or 'SUBSCRIBER'");
            System.exit(1);
        }
        
        if (topic.trim().isEmpty()) {
            System.out.println("Error: TOPIC cannot be empty");
            System.exit(1);
        }
        
        PubSubClient client = new PubSubClient(host, port, clientType, topic);
        client.connectAndCommunicate();
        
        System.out.println("Client terminated.");
    }
}