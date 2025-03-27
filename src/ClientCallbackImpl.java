import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class ClientCallbackImpl extends UnicastRemoteObject implements ClientCallback {
    private ChatClient chatClient;
    private ChatClientGUI chatClientGUI;
    private boolean isGUI;

    // Constructor for console client
    public ClientCallbackImpl(ChatClient client) throws RemoteException {
        super();
        this.chatClient = client;
        this.chatClientGUI = null;
        this.isGUI = false;
    }

    // Constructor for GUI client
    public ClientCallbackImpl(ChatClientGUI clientGUI) throws RemoteException {
        super();
        this.chatClient = null;
        this.chatClientGUI = clientGUI;
        this.isGUI = true;
    }

    @Override
    public void receiveMessage(String sender, String message) throws RemoteException {
        if (isGUI) {
            chatClientGUI.displayMessage(sender, message);
        } else {
            chatClient.displayMessage(sender, message);
        }
    }

    @Override
    public void updateClientList(List<String> clients) throws RemoteException {
        if (isGUI) {
            chatClientGUI.updateClientList(clients);
        } else {
            chatClient.updateClientList(clients);
        }
    }

    @Override
    public void ping() throws RemoteException {
        // This method is intentionally empty
        // It exists solely to check if the RMI connection is still alive
    }
}