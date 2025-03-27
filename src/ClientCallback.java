import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ClientCallback extends Remote {
    void receiveMessage(String sender, String message, boolean isPrivate) throws RemoteException;

    void updateClientList(List<String> clients) throws RemoteException;

    // For backward compatibility
    default void receiveMessage(String sender, String message) throws RemoteException {
        receiveMessage(sender, message, false);
    }
}