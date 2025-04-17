import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ChatService extends Remote {
    void registerClient(String name, ClientCallback callback) throws RemoteException;
    void unregisterClient(String name) throws RemoteException;
    void broadcastMessage(String sender, String message) throws RemoteException;
    void sendPrivateMessage(String sender, String recipient, String message) throws RemoteException;
    List<String> getActiveClients() throws RemoteException;
    void changeUsername(String oldName, String newName) throws RemoteException;
}