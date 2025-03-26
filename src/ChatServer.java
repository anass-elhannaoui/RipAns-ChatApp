import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ChatServer {
    public static void main(String[] args) {
        try {
            // Set the security policy but don't set security manager (for newer Java versions)
            System.setProperty("java.security.policy", "server.policy");
            
            // Set the java.rmi.server.hostname property to the server's IP
            System.setProperty("java.rmi.server.hostname", "192.168.1.2");
            
            // Create and export the service
            ChatServiceImpl service = new ChatServiceImpl();
            
            // Create or get the registry
            Registry registry = LocateRegistry.createRegistry(1099);
            
            // Bind the service
            registry.rebind("ChatService", service);
            
            System.out.println("Chat Server is running...");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}