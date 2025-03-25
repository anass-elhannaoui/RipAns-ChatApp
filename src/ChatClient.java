import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Scanner;

public class ChatClient {
    private String name;
    private ChatService service;
    private ClientCallback callback;
    
    public ChatClient(String name, String serverIP) {
        this.name = name;
        try {
            // Set the security policy but don't set security manager (for newer Java versions)
            System.setProperty("java.security.policy", "./policy/client.policy");
            
            // Get the registry
            Registry registry = LocateRegistry.getRegistry(serverIP, 1099);
            
            // Look up the service
            service = (ChatService) registry.lookup("ChatService");
            
            // Create callback
            callback = new ClientCallbackImpl(this);
            
            // Register with the service
            service.registerClient(name, callback);
            
            System.out.println("Connected to chat server as " + name);
        } catch (RemoteException | NotBoundException e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }
    
    public void sendMessage(String message) {
        try {
            service.broadcastMessage(name, message);
        } catch (RemoteException e) {
            System.err.println("Error sending message: " + e.toString());
        }
    }
    
    public void disconnect() {
        try {
            service.unregisterClient(name);
            System.out.println("Disconnected from chat server");
        } catch (RemoteException e) {
            System.err.println("Error disconnecting: " + e.toString());
        }
    }
    
    public void displayMessage(String sender, String message) {
        System.out.println(sender + ": " + message);
    }
    
    public void updateClientList(List<String> clients) {
        System.out.println("\nActive users: " + String.join(", ", clients));
    }
    
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java ChatClient <username> <server-ip>");
            System.exit(1);
        }
        
        String name = args[0];
        String serverIP = args[1];
        
        ChatClient client = new ChatClient(name, serverIP);
        
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("Type 'exit' to leave the chat");
        
        while (true) {
            String message = scanner.nextLine();
            if ("exit".equalsIgnoreCase(message)) {
                client.disconnect();
                break;
            }
            client.sendMessage(message);
        }
        
        scanner.close();
        System.exit(0);
    }
}