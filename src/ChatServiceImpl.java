import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatServiceImpl extends UnicastRemoteObject implements ChatService {
    private Map<String, ClientCallback> clients;
    
    public ChatServiceImpl() throws RemoteException {
        super();
        clients = new HashMap<>();
        System.out.println("Chat Service started successfully");
    }
    @Override
    public synchronized void changeUsername(String oldName, String newName) throws RemoteException {
        if (clients.containsKey(newName)) {
            throw new RemoteException("The username '" + newName + "' is already taken.");
        }
        
        // Get the callback object for the old name
        ClientCallback callback = clients.remove(oldName);
        
        // Update the username in the clients map
        clients.put(newName, callback);
        
        // Broadcast the username change to all clients
        broadcastMessage("SERVER", oldName + " has changed their username to " + newName);
        
        // Update the client list for all users
        updateClientListForAll();
        
        System.out.println("Username changed from " + oldName + " to " + newName);
    }

    @Override
    public synchronized void registerClient(String name, ClientCallback callback) throws RemoteException {
        // Clean up inactive clients
        List<String> inactiveClients = new ArrayList<>();
        for (Map.Entry<String, ClientCallback> entry : clients.entrySet()) {
            try {
                // Try to ping the existing client to check if it's still active
                entry.getValue().ping();
            } catch (RemoteException e) {
                // If an exception is thrown, this client is no longer active
                inactiveClients.add(entry.getKey());
            }
        }

        // Remove inactive clients
        for (String inactiveName : inactiveClients) {
            clients.remove(inactiveName);
        }

        // Now check if the name is still in the map
        if (clients.containsKey(name)) {
            throw new RemoteException("Name already taken. Please choose another name.");
        }

        // If the name is not in use, register the new client
        clients.put(name, callback);
        System.out.println("New client registered: " + name);
        broadcastMessage("SERVER", name + " has joined the chat.");
        updateClientListForAll();
    }
    
    @Override
    public synchronized void unregisterClient(String name) throws RemoteException {
        clients.remove(name);
        System.out.println("Client unregistered: " + name);
        broadcastMessage("SERVER", name + " has left the chat.");
        updateClientListForAll();
    }
    
    @Override
    public synchronized void broadcastMessage(String sender, String message) throws RemoteException {
        System.out.println(sender + ": " + message);
        List<String> deadClients = new ArrayList<>();
        
        for (Map.Entry<String, ClientCallback> entry : clients.entrySet()) {
            try {
                entry.getValue().receiveMessage(sender, message);
            } catch (RemoteException e) {
                deadClients.add(entry.getKey());
            }
        }
        
        // Remove any clients that couldn't receive the message
        for (String name : deadClients) {
            clients.remove(name);
            System.out.println("Removed dead client: " + name);
        }
        
        if (!deadClients.isEmpty()) {
            updateClientListForAll();
        }
    }
    
    @Override
    public synchronized List<String> getActiveClients() throws RemoteException {
        return new ArrayList<>(clients.keySet());
    }
    
    private void updateClientListForAll() throws RemoteException {
        List<String> clientNames = getActiveClients();
        for (Map.Entry<String, ClientCallback> entry : clients.entrySet()) {
            try {
                entry.getValue().updateClientList(clientNames);
            } catch (RemoteException e) {
                // Will be cleaned up on next broadcast
            }
        }
    }
}