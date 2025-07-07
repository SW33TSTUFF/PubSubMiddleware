import java.net.*;
import java.io.*;
import java.util.*;

public class Server {

    private ServerSocket serverSocket;
    private List<SubscriberHandler> subscribersList = Collections.synchronizedList(new ArrayList<>());
    // private static final String PUB = "PUBLISHER";
    // private static final String SUB = "SUBSCRIBER";

    public static void main(String[] args) {
        System.out.println("Middleware assignment");
        if (args.length < 1) {
            System.out.println("Usage: java Server port_number");
            System.exit(1);
        }

        int portNumber = Integer.parseInt(args[0]);
        Server server = new Server();
        server.start(portNumber);
    }

    public void start(int port) {
        System.out.println("Pub/Sub Server starting on port " + port);

        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server is running and waiting for connections...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                // System.out.println("New client connected: " + clientSocket);
                
                // determine client type (pub or sub)
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String clientType = in.readLine();
                
                if (clientType != null && clientType.equalsIgnoreCase("PUBLISHER")) {
                    new PublisherHandler(clientSocket, this).start();
                } else if (clientType != null && clientType.equalsIgnoreCase("SUBSCRIBER")) {
                    SubscriberHandler handler = new SubscriberHandler(clientSocket, this);
                    subscribersList.add(handler);
                    handler.start();
                } else {
                    System.out.println("Invalid client type, closing connection");
                    clientSocket.close();
                }
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }

    public synchronized void broadcastMessage(String message) {
        System.out.println("Broadcasting message from publisher: " + message);
        for (SubscriberHandler subscriber : subscribersList) {
            subscriber.sendMessage(message);
        }
    }

    public synchronized void removeSubscriber(SubscriberHandler subscriber) {
        subscribersList.remove(subscriber);
    }

    private class PublisherHandler extends Thread {
        private Socket clientSocket;
        private BufferedReader in;
        private Server server;

        public PublisherHandler(Socket socket, Server server) {
            this.clientSocket = socket;
            this.server = server;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                System.out.println("Publisher connected: " + clientSocket);

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    // System.out.println("Received from publisher: " + inputLine);
                    server.broadcastMessage(inputLine);
                }
            } catch (IOException e) {
                System.out.println("Publisher disconnected: " + e.getMessage());
            } finally {
                try {
                    in.close();
                    clientSocket.close();
                } catch (IOException e) {
                    System.out.println("Error closing publisher connection: " + e.getMessage());
                }
            }
        }
    }

    private class SubscriberHandler extends Thread {
        private Socket clientSocket;
        private PrintWriter out;
        private Server server;

        public SubscriberHandler(Socket socket, Server server) {
            this.clientSocket = socket;
            this.server = server;
        }

        public void run() {
            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                System.out.println("Subscriber connected: " + clientSocket);

                // just to keep the connection open
                while (!clientSocket.isClosed()) {
                    Thread.sleep(1000); // Prevent busy waiting
                }
            } catch (Exception e) {
                System.out.println("Subscriber disconnected: " + e.getMessage());
            } finally {
                server.subscribersList.remove(this);
                // server.removeSubscriber(this);
                try {
                    out.close();
                    clientSocket.close();
                } catch (IOException e) {
                    System.out.println("Error closing subscriber connection: " + e.getMessage());
                }
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }
    }
}

// REFERENCES
// https://www.baeldung.com/a-guide-to-java-sockets - for java sockets