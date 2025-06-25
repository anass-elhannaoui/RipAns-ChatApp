import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class ClientCallbackImpl extends UnicastRemoteObject implements ClientCallback {
    private ChatClient chatClient;
    private ChatClientGUI chatClientGUI;
    private boolean isGUI;

    public ClientCallbackImpl(ChatClient client) throws RemoteException {
        super();
        this.chatClient = client;
        this.chatClientGUI = null;
        this.isGUI = false;
    }

    public ClientCallbackImpl(ChatClientGUI clientGUI) throws RemoteException {
        super();
        this.chatClient = null;
        this.chatClientGUI = clientGUI;
        this.isGUI = true;
    }

    @Override
    public void receiveMessage(String sender, String message, boolean isPrivate) throws RemoteException {
        if (isGUI) {
            chatClientGUI.displayMessage(sender, message, isPrivate);
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
}