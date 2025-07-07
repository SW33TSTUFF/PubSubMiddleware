import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class PubSubServer {
    private String host;
    private int port;
    private Map<String, List<Socket>> publishers;
    private Map<String, List<Socket>> subscribers;
    private Map<Socket, ClientInfo> clients;
    private final Object clientsLock = new Object();
    private volatile boolean running = true;
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    
    // Inner class to store client information
    private static class ClientInfo {
        String type;
        String topic;
        String addr;
        
        ClientInfo(String type, String topic, String addr) {
            this.type = type;
            this.topic = topic;
            this.addr = addr;
        }
    }
    
    public PubSubServer(String host, int port) {
        this.host = host;
        this.port = port;
        this.publishers = new ConcurrentHashMap<>();
        this.subscribers = new ConcurrentHashMap<>();
        this.clients = new ConcurrentHashMap<>();
        this.threadPool = Executors.newCachedThreadPool();
    }
    
    private void handleClient(Socket conn) {
        String clientAddr = conn.getRemoteSocketAddress().toString();
        System.out.println("Connected by " + clientAddr);
        
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
             PrintWriter out = new PrintWriter(conn.getOutputStream(), true)) {
            
            // First, receive client type and topic (format: "PUBLISHER:TOPIC_A" or "SUBSCRIBER:TOPIC_A")
            String clientInfo = in.readLine();
            if (clientInfo == null) {
                return;
            }
            
            clientInfo = clientInfo.trim();
            
            // Parse client type and topic
            String[] parts = clientInfo.split(":", 2);
            if (parts.length != 2) {
                System.out.println("Invalid client info format from " + clientAddr + ": " + clientInfo);
                return;
            }
            
            String clientType = parts[0].toUpperCase();
            String topic = parts[1].trim();
            
            if (!clientType.equals("PUBLISHER") && !clientType.equals("SUBSCRIBER")) {
                System.out.println("Invalid client type from " + clientAddr + ": " + clientType);
                return;
            }
            
            if (topic.isEmpty()) {
                System.out.println("Empty topic from " + clientAddr);
                return;
            }
            
            // Add client to appropriate topic lists
            synchronized (clientsLock) {
                // Store client info
                clients.put(conn, new ClientInfo(clientType, topic, clientAddr));
                
                if (clientType.equals("PUBLISHER")) {
                    publishers.computeIfAbsent(topic, k -> new ArrayList<>()).add(conn);
                    System.out.println("Publisher connected from " + clientAddr + " for topic '" + topic + "'");
                } else { // SUBSCRIBER
                    subscribers.computeIfAbsent(topic, k -> new ArrayList<>()).add(conn);
                    System.out.println("Subscriber connected from " + clientAddr + " for topic '" + topic + "'");
                }
                
                // Display current topic status
                displayTopicStatus();
            }
            
            // Handle messages
            String message;
            while (running && (message = in.readLine()) != null) {
                message = message.trim();
                
                if (message.toLowerCase().equals("terminate")) {
                    System.out.println("Client " + clientAddr + " sent terminate command");
                    break;
                }
                
                // Display message on server
                System.out.println("Received from " + clientType + " " + clientAddr + " on topic '" + topic + "': " + message);
                
                // If it's a publisher, forward to subscribers of the same topic
                if (clientType.equals("PUBLISHER")) {
                    forwardToTopicSubscribers(message, topic, clientAddr);
                }
            }
            
        } catch (IOException e) {
            System.out.println("Client " + clientAddr + " disconnected unexpectedly");
        } finally {
            // Remove client from lists
            synchronized (clientsLock) {
                ClientInfo clientInfo = clients.get(conn);
                if (clientInfo != null) {
                    String clientType = clientInfo.type;
                    String topic = clientInfo.topic;
                    
                    // Remove from appropriate topic list
                    if (clientType.equals("PUBLISHER") && publishers.containsKey(topic)) {
                        publishers.get(topic).remove(conn);
                        if (publishers.get(topic).isEmpty()) {
                            publishers.remove(topic);
                        }
                    } else if (clientType.equals("SUBSCRIBER") && subscribers.containsKey(topic)) {
                        subscribers.get(topic).remove(conn);
                        if (subscribers.get(topic).isEmpty()) {
                            subscribers.remove(topic);
                        }
                    }
                    
                    // Remove from clients map
                    clients.remove(conn);
                    
                    System.out.println(clientType + " " + clientAddr + " disconnected from topic '" + topic + "'");
                    displayTopicStatus();
                }
            }
            
            try {
                conn.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }
    
    private void forwardToTopicSubscribers(String message, String topic, String senderAddr) {
        synchronized (clientsLock) {
            if (!subscribers.containsKey(topic)) {
                System.out.println("No subscribers for topic '" + topic + "'");
                return;
            }
            
            List<Socket> disconnectedSubscribers = new ArrayList<>();
            List<Socket> topicSubscribers = subscribers.get(topic);
            int subscribersCount = topicSubscribers.size();
            
            for (Socket subscriber : topicSubscribers) {
                try {
                    PrintWriter out = new PrintWriter(subscriber.getOutputStream(), true);
                    String formattedMessage = "[" + topic + "] Publisher " + senderAddr + ": " + message;
                    out.println(formattedMessage);
                } catch (IOException e) {
                    // Mark for removal if send fails
                    disconnectedSubscribers.add(subscriber);
                }
            }
            
            // Remove disconnected subscribers
            for (Socket subscriber : disconnectedSubscribers) {
                topicSubscribers.remove(subscriber);
                clients.remove(subscriber);
            }
            
            // Remove topic if no more subscribers
            if (topicSubscribers.isEmpty()) {
                subscribers.remove(topic);
            }
            
            int deliveredCount = subscribersCount - disconnectedSubscribers.size();
            System.out.println("Message delivered to " + deliveredCount + " subscriber(s) on topic '" + topic + "'");
        }
    }
    
    private void displayTopicStatus() {
        System.out.println("\n=== Current Topic Status ===");
        
        // Display publishers
        System.out.println("Publishers:");
        if (publishers.isEmpty()) {
            System.out.println("  None");
        } else {
            for (Map.Entry<String, List<Socket>> entry : publishers.entrySet()) {
                System.out.println("  Topic '" + entry.getKey() + "': " + entry.getValue().size() + " publisher(s)");
            }
        }
        
        // Display subscribers
        System.out.println("Subscribers:");
        if (subscribers.isEmpty()) {
            System.out.println("  None");
        } else {
            for (Map.Entry<String, List<Socket>> entry : subscribers.entrySet()) {
                System.out.println("  Topic '" + entry.getKey() + "': " + entry.getValue().size() + " subscriber(s)");
            }
        }
        
        System.out.println("============================\n");
    }
    
    private void shutdownServer() {
        System.out.println("Initiating server shutdown...");
        running = false;
        
        // Close all client connections
        synchronized (clientsLock) {
            for (Socket client : clients.keySet()) {
                try {
                    client.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
            clients.clear();
            publishers.clear();
            subscribers.clear();
        }
        
        // Close server socket
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
        
        // Shutdown thread pool
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
        }
    }
    
    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server starting on " + host + ":" + port);
            System.out.println("Server listening on port " + port + "...");
            System.out.println("Waiting for client connections...");
            System.out.println("Press Ctrl+C to shutdown server");
            
            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownServer));
            
            while (running) {
                try {
                    Socket conn = serverSocket.accept();
                    if (running) {
                        // Submit client handling to thread pool
                        threadPool.submit(() -> handleClient(conn));
                    }
                } catch (IOException e) {
                    if (running) {
                        System.out.println("Socket error: " + e.getMessage());
                    }
                    break;
                }
            }
            
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }
    
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java PubSubServer <PORT>");
            System.out.println("Example: java PubSubServer 5000");
            System.exit(1);
        }
        
        int port;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.out.println("Error: PORT must be a valid integer");
            System.exit(1);
            return;
        }
        
        String host = "127.0.0.1";
        
        PubSubServer server = new PubSubServer(host, port);
        server.start();
        
        System.out.println("Server terminated.");
    }
}