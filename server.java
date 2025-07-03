public class Server {
    public static void main(String[] args) {
        System.out.println("Middleware assignment");
        if(args.length < 1) {
            System.out.println("Please use the proper format (java Server port_number)");
            
        }

        int portNumber = Integer.parseInt(args[0]);
        System.out.println("Entered port number: " + portNumber);
    }
}