import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatServiceImpl extends UnicastRemoteObject implements ChatService {
    private Map<String, ClientCallback> clients;
    private List<String> registeredNames; // For tracking all registered names if needed

    public ChatServiceImpl() throws RemoteException {
        super();
        clients = new HashMap<>();
        registeredNames = new ArrayList<>();
        System.out.println("Chat Service started successfully");
    }

    @Override
    public synchronized boolean isUsernameTaken(String username) throws RemoteException {
        // Check both connected clients and all registered names
        return clients.containsKey(username) || registeredNames.contains(username);
    }

    @Override
    public synchronized void registerClient(String name, ClientCallback callback) throws RemoteException {
        if (isUsernameTaken(name)) {
            throw new RemoteException("Name '" + name + "' is already taken. Please choose another name.");
        }
        
        clients.put(name, callback);
        registeredNames.add(name); // Add to permanent registry
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
    public synchronized void changeUsername(String oldName, String newName) throws RemoteException {
        if (isUsernameTaken(newName)) {
            throw new RemoteException("The username '" + newName + "' is already taken.");
        }

        ClientCallback callback = clients.remove(oldName);
        registeredNames.remove(oldName);
        clients.put(newName, callback);
        registeredNames.add(newName);
        
        broadcastMessage("SERVER", oldName + " has changed their username to " + newName);
        updateClientListForAll();
        System.out.println("Username changed from " + oldName + " to " + newName);
    }

    @Override
    public synchronized void broadcastMessage(String sender, String message) throws RemoteException {
        System.out.println(sender + ": " + message);
        List<String> deadClients = new ArrayList<>();

        for (Map.Entry<String, ClientCallback> entry : clients.entrySet()) {
            try {
                entry.getValue().receiveMessage(sender, message, false);
            } catch (RemoteException e) {
                deadClients.add(entry.getKey());
            }
        }

        for (String name : deadClients) {
            clients.remove(name);
            System.out.println("Removed dead client: " + name);
        }

        if (!deadClients.isEmpty()) {
            updateClientListForAll();
        }
    }

    @Override
    public synchronized void sendPrivateMessage(String sender, String recipient, String message)
            throws RemoteException {
        if (!clients.containsKey(recipient)) {
            throw new RemoteException("Recipient " + recipient + " is not online.");
        }

        try {
            // Send to recipient
            clients.get(recipient).receiveMessage("[Private] " + sender, message, true);

            // Send confirmation to sender
            if (clients.containsKey(sender)) {
                clients.get(sender).receiveMessage("[Private] " + sender, "To " + recipient + ": " + message, true);
            }
        } catch (RemoteException e) {
            clients.remove(recipient);
            registeredNames.remove(recipient);
            updateClientListForAll();
            throw new RemoteException("Failed to send private message: " + e.getMessage());
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