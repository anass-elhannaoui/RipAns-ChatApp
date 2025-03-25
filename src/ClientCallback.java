import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List; 

public interface ClientCallback extends Remote {
    void receiveMessage(String sender, String message) throws RemoteException;
    void updateClientList(List<String> clients) throws RemoteException;
}
