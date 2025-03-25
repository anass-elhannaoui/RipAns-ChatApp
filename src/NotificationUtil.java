import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.net.URL;

public class NotificationUtil {
    private static TrayIcon trayIcon;
    private static ChatClientGUI chatGUI;
    private static boolean notificationsEnabled = true;
    private static ActionListener notificationClickListener;

    public static void initTrayIcon(ChatClientGUI chatGUI) {
        if (!SystemTray.isSupported()) {
            System.err.println("System tray not supported");
            return;
        }

        NotificationUtil.chatGUI = chatGUI;

        SystemTray tray = SystemTray.getSystemTray();

        // Try loading icon from resources
        Image image = loadTrayIcon();
        if (image == null) {
            System.err.println("Could not load tray icon");
            return;
        }

        trayIcon = new TrayIcon(image, "RMI Chat");
        trayIcon.setImageAutoSize(true);

        // Create popup menu
        PopupMenu popup = new PopupMenu();
        
        MenuItem openItem = new MenuItem("Open Chat");
        openItem.addActionListener(e -> restoreChatWindow());
        
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(e -> {
            tray.remove(trayIcon);
            chatGUI.dispose();
            System.exit(0);
        });
        
        popup.add(openItem);
        popup.addSeparator();
        popup.add(exitItem);
        trayIcon.setPopupMenu(popup);

        // Create reusable click listener
        notificationClickListener = e -> restoreChatWindow();
        
        // Add click listener to tray icon
        trayIcon.addActionListener(notificationClickListener);

        try {
            tray.add(trayIcon);
        } catch (AWTException ex) {
            System.err.println("TrayIcon could not be added: " + ex.getMessage());
        }
    }

    private static void restoreChatWindow() {
        if (chatGUI != null) {
            SwingUtilities.invokeLater(() -> {
                chatGUI.setVisible(true);
                chatGUI.setExtendedState(JFrame.NORMAL);
                chatGUI.toFront();
                chatGUI.requestFocus();
                
                // Bring the window to the very front
                chatGUI.setAlwaysOnTop(true);
                chatGUI.setAlwaysOnTop(false);
            });
        }
    }

    private static Image loadTrayIcon() {
        try {
            // Try loading from resources first
            URL imageUrl = NotificationUtil.class.getResource("/notification.png");
            if (imageUrl != null) {
                return Toolkit.getDefaultToolkit().getImage(imageUrl);
            }
            
            // Fallback to file system
            return Toolkit.getDefaultToolkit().getImage("notification.png");
        } catch (Exception e) {
            System.err.println("Error loading tray icon: " + e.getMessage());
            return null;
        }
    }

    public static void showNotification(String title, String message) {
        if (!notificationsEnabled || trayIcon == null) return;
        
        // Ensure this runs on the EDT
        SwingUtilities.invokeLater(() -> {
            try {
                // Add click listener before showing notification
                trayIcon.addActionListener(notificationClickListener);
                
                trayIcon.displayMessage(
                    title,
                    message,
                    TrayIcon.MessageType.INFO
                );
                
                // Remove the listener after showing to avoid duplicates
                // We do this in a timer to ensure it runs after the notification is processed
                new Timer(1000, e -> {
                    trayIcon.removeActionListener(notificationClickListener);
                    ((Timer)e.getSource()).stop();
                }).start();
            } catch (Exception e) {
                System.err.println("Failed to show notification: " + e.getMessage());
            }
        });
    }

    public static void setNotificationsEnabled(boolean enabled) {
        notificationsEnabled = enabled;
    }

    public static void removeTrayIcon() {
        if (trayIcon != null) {
            SystemTray.getSystemTray().remove(trayIcon);
        }
    }
}