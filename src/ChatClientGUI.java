import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.swing.border.*;
import javax.swing.text.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.File;
import java.net.URL;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.net.URI;
public class ChatClientGUI extends JFrame {
    // Performance tuning fields
    private boolean needsFullUIUpdate = false;
    private static final boolean DEBUG_MODE = false;
    
    // UI Components
    private String name;
    private ChatService service;
    private ClientCallbackImpl callback;
    private JTextPane chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private JButton emojiButton;
    private JButton settingsButton;
    private JButton changeNameButton;
    private JButton okButton;
    private JList<UserListItem> userList;
    private DefaultListModel<UserListItem> userListModel;
    private JLabel statusLabel;
    private JLabel userPanelHeader;
    private JPanel emojiPanel;
    private JPanel statusPanel;
    private JPanel messagePanel;
    private boolean emojiPanelVisible = false;
    private boolean notificationsEnabled = true;
    
    // Modern theme settings with vibrant colors and gradients
    private Color THEME_COLOR = new Color(100, 149, 237);
    private Color THEME_SECONDARY = new Color(65, 105, 225);
    private Color BACKGROUND_COLOR = new Color(248, 249, 252);
    private Color MESSAGE_PANEL_COLOR = new Color(240, 242, 245);
    private Font DEFAULT_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private GradientPaint currentGradient;

    // Twemoji settings
    private static final String TWEMOJI_CDN = "https://twemoji.maxcdn.com/v/latest/72x72/";
    private static final String TWEMOJI_EXT = ".png";
    private static final Map<String, ImageIcon> emojiCache = new HashMap<>();
    private static final Pattern EMOJI_PATTERN = Pattern.compile("[\uD83C-\uDBFF\uDC00-\uDFFF]+");

    public ChatClientGUI(String name, String serverIP) {
        this.name = name;
        initializeUI();
        setAppIcon();
        connectToServer(serverIP);
        NotificationUtil.initTrayIcon(this);
    }

    private void setAppIcon() {
        try {
            Image icon = ImageIO.read(getClass().getResource("/assets/logo.png"));
            setIconImage(icon);
            
            if (Taskbar.isTaskbarSupported()) {
                Taskbar taskbar = Taskbar.getTaskbar();
                if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                    taskbar.setIconImage(icon);
                }
            }
        } catch (Exception e) {
            try {
                Image icon = ImageIO.read(new File("logo.png"));
                setIconImage(icon);
            } catch (IOException ex) {
                if (DEBUG_MODE) {
                    System.err.println("Could not load application icon: " + ex.getMessage());
                }
            }
        }
    }

    private void initializeUI() {
        setTitle("RMI Chat - " + name);
        setSize(950, 720);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            UIManager.put("Button.arc", 15);
            UIManager.put("Component.arc", 15);
            UIManager.put("TextComponent.arc", 10);
        } catch (Exception e) {
            e.printStackTrace();
        }

        createMainPanel();
        setVisible(true);
    }

    private void createMainPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (currentGradient != null) {
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setPaint(currentGradient);
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };
        mainPanel.setOpaque(false);
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        mainPanel.add(createStatusBar(), BorderLayout.NORTH);
        mainPanel.add(createChatPanel(), BorderLayout.CENTER);
        mainPanel.add(createUserListPanel(), BorderLayout.EAST);
        mainPanel.add(createMessageInputPanel(), BorderLayout.SOUTH);

        setContentPane(mainPanel);
        setupWindowListeners();
    }

    private JPanel createStatusBar() {
        statusPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gradient = new GradientPaint(
                    0, 0, THEME_COLOR, 
                    getWidth(), 0, THEME_SECONDARY
                );
                g2d.setPaint(gradient);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 0, 0);
                super.paintComponent(g);
            }
        };
        statusPanel.setOpaque(false);
        statusPanel.setBorder(new EmptyBorder(10, 15, 10, 15));
        statusPanel.setPreferredSize(new Dimension(getWidth(), 50));

        statusLabel = new JLabel("Connected as: " + name);
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        statusPanel.add(statusLabel, BorderLayout.WEST);

        settingsButton = createIconButton("/assets/settings-icon.png", "Settings", 24);
        settingsButton.setContentAreaFilled(false);
        settingsButton.setBorderPainted(false);
        settingsButton.setFocusPainted(false);
        settingsButton.setOpaque(false);
        settingsButton.addActionListener(e -> showSettingsDialog());

        JPanel settingsWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        settingsWrapper.setOpaque(false);
        settingsWrapper.add(settingsButton);

        statusPanel.add(settingsWrapper, BorderLayout.EAST);

        return statusPanel;
    }

    private ImageIcon loadIcon(String filename, int size) {
        try {
            Image image = ImageIO.read(getClass().getResource(filename));
            return new ImageIcon(image.getScaledInstance(size, size, Image.SCALE_SMOOTH));
        } catch (Exception e) {
            if (DEBUG_MODE) {
                System.err.println("Error loading icon: " + filename + " - " + e.getMessage());
            }
            return null;
        }
    }
    
    private JButton createIconButton(String iconFile, String tooltip, int iconSize) {
        JButton button = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.0f));
                g2d.setColor(new Color(0, 0, 0, 0));
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        
        button.setToolTipText(tooltip);
        ImageIcon icon = loadIcon(iconFile, iconSize);
        if (icon != null) {
            button.setIcon(icon);
        } else {
            button.setText(tooltip);
        }
        
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setBackground(new Color(0, 0, 0, 0));
        
        styleButton(button);
        return button;
    }

    private JPanel createChatPanel() {
        chatArea = new JTextPane() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(BACKGROUND_COLOR);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                super.paintComponent(g);
            }
        };
        chatArea.setEditable(false);
        chatArea.setOpaque(false);
        chatArea.setFont(DEFAULT_FONT);
        setupMessageStyles();

        JScrollPane chatScrollPane = new JScrollPane(chatArea) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(BACKGROUND_COLOR);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                super.paintComponent(g);
            }
        };
        chatScrollPane.setOpaque(false);
        chatScrollPane.getViewport().setOpaque(false);
        chatScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        chatScrollPane.setBorder(BorderFactory.createEmptyBorder());

        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setOpaque(false);
        chatPanel.add(chatScrollPane, BorderLayout.CENTER);
        chatPanel.add(createEmojiPanel(), BorderLayout.SOUTH);
        
        return chatPanel;
    }

    private void setupMessageStyles() {
        StyledDocument doc = chatArea.getStyledDocument();
        Style defaultStyle = chatArea.getStyle(StyleContext.DEFAULT_STYLE);
        StyleConstants.setFontFamily(defaultStyle, "Segoe UI");
        StyleConstants.setFontSize(defaultStyle, 14);

        Style serverStyle = doc.addStyle("Server", defaultStyle);
        StyleConstants.setForeground(serverStyle, new Color(120, 120, 120));
        StyleConstants.setItalic(serverStyle, true);

        Style userStyle = doc.addStyle("User", defaultStyle);
        StyleConstants.setForeground(userStyle, THEME_COLOR.darker());
        StyleConstants.setBold(userStyle, true);

        Style messageStyle = doc.addStyle("Message", defaultStyle);
        StyleConstants.setForeground(messageStyle, new Color(60, 60, 60));

        Style timeStyle = doc.addStyle("Time", defaultStyle);
        StyleConstants.setForeground(timeStyle, new Color(150, 150, 150));
        StyleConstants.setFontSize(timeStyle, 12);
        StyleConstants.setItalic(timeStyle, true);
    }

    private JPanel createEmojiPanel() {
        emojiPanel = new JPanel(new GridLayout(2, 8, 10, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(255, 255, 255, 200));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                super.paintComponent(g);
            }
        };
        emojiPanel.setOpaque(false);
        emojiPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220, 150)),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        emojiPanel.setPreferredSize(new Dimension(getWidth(), 90));
        emojiPanel.setVisible(false);
    
        String[] emojis = { "😀", "😃", "❤️", "👍", "🎉", "🔥", "👋", "😊",
                          "😎", "🤔", "😕", "😢", "🥰", "🙏", "💔", "✨" };
    
        for (String emoji : emojis) {
            ImageIcon emojiIcon = getTwemojiImage(emoji);
            JButton emojiButton;
            
            if (emojiIcon != null) {
                emojiButton = new JButton(emojiIcon);
                emojiButton.setPreferredSize(new Dimension(32, 32));
            } else {
                emojiButton = new JButton(emoji);
                emojiButton.setFont(getEmojiFont(20));
            }
            
            emojiButton.setOpaque(false);
            emojiButton.setContentAreaFilled(false);
            emojiButton.setBorderPainted(false);
            emojiButton.setBorder(new EmptyBorder(5, 5, 5, 5));
            emojiButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            emojiButton.addActionListener(e -> addEmojiToMessage(emoji));
            emojiPanel.add(emojiButton);
        }
    
        return emojiPanel;
    }

private ImageIcon getTwemojiImage(String emoji) {
    return emojiCache.computeIfAbsent(emoji, e -> {
        try {
            String codePoint = toCodePoint(emoji);
            
            // Multiple paths to try loading the emoji
            String[] possiblePaths = {
                "/assets/twemoji/72x72/" + codePoint + ".png",  // Primary path
                "/twemoji/" + codePoint + ".png",         // Alternative path
                "assets/twemoji/" + codePoint + ".png",   // Another possible path
                "twemoji/" + codePoint + ".png"           // Yet another path
            };
            
            for (String path : possiblePaths) {
                URL url = getClass().getResource(path);
                if (url != null) {
                    if (DEBUG_MODE) {
                        System.out.println("Attempting to load emoji from path: " + path);
                    }
                    
                    Image image = ImageIO.read(url);
                    return new ImageIcon(image.getScaledInstance(24, 24, Image.SCALE_SMOOTH));
                } else if (DEBUG_MODE) {
                    System.out.println("URL not found for path: " + path);
                }
            }
            
            // Fallback logging if no emoji found
            if (DEBUG_MODE) {
                System.err.println("No emoji found for code point: " + codePoint);
                
                // Alternative debugging to list resources
                try {
                    Enumeration<URL> resources = getClass().getClassLoader().getResources("twemoji");
                    if (resources.hasMoreElements()) {
                        System.out.println("Twemoji resources found:");
                        while (resources.hasMoreElements()) {
                            System.out.println(resources.nextElement());
                        }
                    } else {
                        System.out.println("No twemoji resources found in classpath");
                    }
                } catch (IOException ioEx) {
                    ioEx.printStackTrace();
                }
            }
        } catch (IOException | NullPointerException ex) {
            if (DEBUG_MODE) {
                System.err.println("Error loading Twemoji: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
        return null;
    });
}

private String toCodePoint(String emoji) {
    StringBuilder sb = new StringBuilder();
    emoji.codePoints()
         .mapToObj(cp -> String.format("%x", cp))
         .forEach(sb::append);
    return sb.toString();
}
    private Font getEmojiFont(int size) {
        String[] emojiFonts = {
            "Segoe UI Emoji", "Apple Color Emoji", 
            "Noto Color Emoji", "EmojiOne"
        };
    
        for (String fontName : emojiFonts) {
            Font font = new Font(fontName, Font.PLAIN, size);
            if (font.getFamily().equals(fontName)) {
                return font;
            }
        }
        return new Font(Font.SANS_SERIF, Font.PLAIN, size);
    }

    private JPanel createUserListPanel() {
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.setFont(DEFAULT_FONT);
        userList.setBackground(Color.WHITE);
        userList.setBorder(new EmptyBorder(10, 10, 10, 10));
        userList.setCellRenderer(new UserListRenderer());

        JScrollPane userScrollPane = new JScrollPane(userList) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(Color.WHITE);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                super.paintComponent(g);
            }
        };
        userScrollPane.setOpaque(false);
        userScrollPane.getViewport().setOpaque(false);
        userScrollPane.setPreferredSize(new Dimension(220, getHeight()));
        userScrollPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(5, 5, 5, 5),
            BorderFactory.createMatteBorder(1, 1, 1, 1, new Color(220, 220, 220))
        ));

        JPanel userPanel = new JPanel(new BorderLayout());
        userPanel.setOpaque(false);
        
        userPanelHeader = new JLabel("Online Users", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gradient = new GradientPaint(
                    0, 0, THEME_COLOR, 
                    getWidth(), 0, THEME_SECONDARY
                );
                g2d.setPaint(gradient);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 0, 0);
                super.paintComponent(g);
            }
        };
        userPanelHeader.setFont(new Font("Segoe UI", Font.BOLD, 14));
        userPanelHeader.setForeground(Color.WHITE);
        userPanelHeader.setOpaque(false);
        userPanelHeader.setBorder(new EmptyBorder(12, 0, 12, 0));
        userPanel.add(userPanelHeader, BorderLayout.NORTH);
        userPanel.add(userScrollPane, BorderLayout.CENTER);
        
        return userPanel;
    }
    
    private JPanel createMessageInputPanel() {
        messagePanel = new JPanel(new BorderLayout(15, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(MESSAGE_PANEL_COLOR);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                super.paintComponent(g);
            }
        };
        messagePanel.setOpaque(false);
        messagePanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 220, 220)),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
    
        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setOpaque(false);
        messageField = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(255, 255, 255, 200));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                super.paintComponent(g);
            }
        };
        styleTextField(messageField);
        
        emojiButton = createIconButton("/assets/emojies-icon.png", "Insert Emoji", 28);
        emojiButton.addActionListener(e -> toggleEmojiPanel());
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(emojiButton, BorderLayout.WEST);
        messagePanel.add(inputPanel, BorderLayout.CENTER);
    
        sendButton = createStyledButton("Send", "Send Message");
        styleButton(sendButton);
        sendButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        sendButton.setPreferredSize(new Dimension(100, 45));
        sendButton.addActionListener(e -> sendMessage());
    
        addHoverEffects(sendButton);
        messagePanel.add(sendButton, BorderLayout.EAST);
        
        return messagePanel;
    }

    private void setupWindowListeners() {
        messageField.addActionListener(e -> sendMessage());
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnect();
            }
        });
    }

    private void connectToServer(String serverIP) {
        try {
            System.setProperty("java.security.policy", "client.policy");
            Registry registry = LocateRegistry.getRegistry(serverIP, 1099);
            service = (ChatService) registry.lookup("ChatService");
            callback = new ClientCallbackImpl(this);
            service.registerClient(name, callback);
            displayMessage("SERVER", "Connected to chat server as " + name);
        } catch (RemoteException | NotBoundException e) {
            JOptionPane.showMessageDialog(this,
                    "Error connecting to server: " + e.getMessage(),
                    "Connection Error",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void updateUI() {
        SwingUtilities.invokeLater(() -> {
            if (needsFullUIUpdate) {
                SwingUtilities.updateComponentTreeUI(this);
                needsFullUIUpdate = false;
            }
            
            updateAllComponentStyles();
            revalidate();
            repaint();
        });
    }

    private void updateAllComponentStyles() {
        synchronized (getTreeLock()) {
            getContentPane().setBackground(BACKGROUND_COLOR);
            chatArea.setBackground(BACKGROUND_COLOR);
            
            updateButtonStyles();
        }
    }

    private void updateButtonStyles() {
        Component[] buttons = {sendButton, emojiButton, settingsButton, changeNameButton, okButton};
        for (Component comp : buttons) {
            if (comp != null && comp instanceof AbstractButton) {
                AbstractButton button = (AbstractButton) comp;
                button.setForeground(THEME_COLOR);
                button.setFont(DEFAULT_FONT);
                
                if (button.getIcon() != null) {
                    button.setContentAreaFilled(false);
                    button.setOpaque(false);
                    button.setBorder(new EmptyBorder(5, 5, 5, 5));
                }
            }
        }
    }

    private void applyTheme(String theme) {
        switch (theme) {
            case "Ocean Wave":
                THEME_COLOR = new Color(0, 150, 255);
                THEME_SECONDARY = new Color(0, 200, 255);
                BACKGROUND_COLOR = new Color(230, 245, 255);
                MESSAGE_PANEL_COLOR = new Color(210, 235, 255);
                currentGradient = new GradientPaint(0, 0, new Color(0, 180, 255), getWidth(), 0, new Color(0, 120, 200));
                break;
                
            case "Sunset Glow":
                THEME_COLOR = new Color(255, 100, 50);
                THEME_SECONDARY = new Color(255, 50, 100);
                BACKGROUND_COLOR = new Color(255, 240, 230);
                MESSAGE_PANEL_COLOR = new Color(255, 230, 220);
                currentGradient = new GradientPaint(0, 0, new Color(255, 120, 80), getWidth(), 0, new Color(255, 80, 120));
                break;
                
            case "Emerald Forest":
                THEME_COLOR = new Color(0, 180, 120);
                THEME_SECONDARY = new Color(0, 150, 100);
                BACKGROUND_COLOR = new Color(230, 255, 240);
                MESSAGE_PANEL_COLOR = new Color(220, 250, 230);
                currentGradient = new GradientPaint(0, 0, new Color(0, 200, 140), getWidth(), 0, new Color(0, 160, 110));
                break;
                
            case "Purple Haze":
                THEME_COLOR = new Color(150, 50, 200);
                THEME_SECONDARY = new Color(200, 50, 150);
                BACKGROUND_COLOR = new Color(245, 230, 255);
                MESSAGE_PANEL_COLOR = new Color(235, 220, 250);
                currentGradient = new GradientPaint(0, 0, new Color(170, 70, 220), getWidth(), 0, new Color(220, 70, 170));
                break;
                
            case "Midnight Sky":
                THEME_COLOR = new Color(30, 30, 50);
                THEME_SECONDARY = new Color(50, 30, 70);
                BACKGROUND_COLOR = new Color(40, 40, 60);
                MESSAGE_PANEL_COLOR = new Color(50, 50, 70);
                currentGradient = new GradientPaint(0, 0, new Color(40, 40, 80), getWidth(), 0, new Color(30, 30, 60));
                break;
                
            case "Neon Cyber":
                THEME_COLOR = new Color(0, 255, 200);
                THEME_SECONDARY = new Color(200, 0, 255);
                BACKGROUND_COLOR = new Color(20, 20, 30);
                MESSAGE_PANEL_COLOR = new Color(30, 30, 40);
                currentGradient = new GradientPaint(0, 0, new Color(0, 255, 180), getWidth(), 0, new Color(180, 0, 255));
                break;
        }
        needsFullUIUpdate = true;
        updateUI();
    }

    private void applyFontSize(String fontSize) {
        int size = switch (fontSize) {
            case "Small" -> 12;
            case "Large" -> 16;
            default -> 14;
        };
        
        DEFAULT_FONT = new Font("Segoe UI", Font.PLAIN, size);
        
        chatArea.setFont(DEFAULT_FONT);
        messageField.setFont(DEFAULT_FONT);
        userList.setFont(DEFAULT_FONT);
        
        StyledDocument doc = chatArea.getStyledDocument();
        StyleConstants.setFontSize(doc.getStyle(StyleContext.DEFAULT_STYLE), size);
        StyleConstants.setFontSize(doc.getStyle("Time"), size - 2);
        
        updateUI();
    }

    private void playMessageSound() {
        if (notificationsEnabled) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    private void showSettingsDialog() {
        JDialog settingsDialog = new JDialog(this, "Settings", true);
        settingsDialog.setSize(500, 400);
        settingsDialog.setLocationRelativeTo(this);
        settingsDialog.setLayout(new BorderLayout(15, 15));
        settingsDialog.getContentPane().setBackground(BACKGROUND_COLOR);
    
        JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
        settingsPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        settingsPanel.setBackground(MESSAGE_PANEL_COLOR);
    
        // Change Username Section
        JPanel usernamePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        usernamePanel.setBackground(MESSAGE_PANEL_COLOR);
        JLabel usernameLabel = new JLabel("Change Username: ");
        usernameLabel.setFont(DEFAULT_FONT);
        JTextField usernameField = new JTextField(name, 15);
        usernameField.setFont(DEFAULT_FONT);
        changeNameButton = createStyledButton("Apply", "Change Username");
        styleButton(changeNameButton);
        addHoverEffects(changeNameButton);
    
        changeNameButton.addActionListener(e -> {
            String newName = usernameField.getText().trim();
            if (!newName.isEmpty() && !newName.equals(name)) {
                try {
                    service.changeUsername(name, newName);
                    name = newName;
                    statusLabel.setText("Connected as: " + name);
                    JOptionPane.showMessageDialog(settingsDialog,
                            "Username changed successfully!",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (RemoteException ex) {
                    JOptionPane.showMessageDialog(settingsDialog,
                            "Failed to change username: " + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(settingsDialog,
                        "Please enter a valid username.",
                        "Invalid Input",
                        JOptionPane.WARNING_MESSAGE);
            }
        });
    
        usernamePanel.add(usernameLabel);
        usernamePanel.add(usernameField);
        usernamePanel.add(changeNameButton);
        settingsPanel.add(usernamePanel);
    
        settingsPanel.add(Box.createVerticalStrut(15));
    
        // Theme Selection Section
        JPanel themePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        themePanel.setBackground(MESSAGE_PANEL_COLOR);
        JLabel themeLabel = new JLabel("Select Theme: ");
        themeLabel.setFont(DEFAULT_FONT);
        String[] themes = { 
            "Ocean Wave", "Sunset Glow", "Emerald Forest", 
            "Purple Haze", "Midnight Sky", "Neon Cyber"
        };
        JComboBox<String> themeCombo = new JComboBox<>(themes);
        themeCombo.setFont(DEFAULT_FONT);
    
        themeCombo.addActionListener(e -> {
            String selectedTheme = (String) themeCombo.getSelectedItem();
            applyTheme(selectedTheme);
        });
    
        themePanel.add(themeLabel);
        themePanel.add(themeCombo);
        settingsPanel.add(themePanel);
    
        settingsPanel.add(Box.createVerticalStrut(15));
    
        // Font Size Selection Section
        JPanel fontPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fontPanel.setBackground(MESSAGE_PANEL_COLOR);
        JLabel fontLabel = new JLabel("Font Size: ");
        fontLabel.setFont(DEFAULT_FONT);
        String[] fontSizes = { "Small", "Medium", "Large" };
        JComboBox<String> fontCombo = new JComboBox<>(fontSizes);
        fontCombo.setFont(DEFAULT_FONT);
        fontCombo.setSelectedItem("Medium");
    
        fontCombo.addActionListener(e -> {
            String selectedFontSize = (String) fontCombo.getSelectedItem();
            applyFontSize(selectedFontSize);
        });
    
        fontPanel.add(fontLabel);
        fontPanel.add(fontCombo);
        settingsPanel.add(fontPanel);
    
        settingsPanel.add(Box.createVerticalStrut(15));
    
        // Notification Settings Section
        JPanel notificationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        notificationPanel.setBackground(MESSAGE_PANEL_COLOR);
        JCheckBox notificationCheck = new JCheckBox("Enable Notifications");
        notificationCheck.setFont(DEFAULT_FONT);
        notificationCheck.setSelected(notificationsEnabled);
    
        notificationCheck.addActionListener(e -> {
            notificationsEnabled = notificationCheck.isSelected();
            NotificationUtil.setNotificationsEnabled(notificationsEnabled);
            if (notificationsEnabled) {
                playMessageSound();
            }
        });
    
        notificationPanel.add(notificationCheck);
        settingsPanel.add(notificationPanel);
    
        settingsPanel.add(Box.createVerticalStrut(20));
    
        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(MESSAGE_PANEL_COLOR);
        okButton = createStyledButton("OK", "Close Settings");
        styleButton(okButton);
        addHoverEffects(okButton);
    
        okButton.addActionListener(e -> settingsDialog.dispose());
    
        buttonPanel.add(okButton);
        settingsPanel.add(buttonPanel);
    
        settingsDialog.add(settingsPanel, BorderLayout.CENTER);
        settingsDialog.setVisible(true);
    }

    private JButton createStyledButton(String text, String tooltip) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (isEnabled()) {
                    GradientPaint gradient = new GradientPaint(
                        0, 0, new Color(THEME_COLOR.getRed(), THEME_COLOR.getGreen(), THEME_COLOR.getBlue(), 100), 
                        getWidth(), 0, new Color(THEME_SECONDARY.getRed(), THEME_SECONDARY.getGreen(), THEME_SECONDARY.getBlue(), 100)
                    );
                    g2d.setPaint(gradient);
                    g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                }
                
                super.paintComponent(g);
            }
        };
        
        button.setToolTipText(tooltip);
        button.setForeground(Color.WHITE);
        button.setBorder(new EmptyBorder(5, 15, 5, 15));
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setBackground(new Color(0, 0, 0, 0));
        
        return button;
    }

    private void styleButton(AbstractButton button) {
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setBackground(new Color(0, 0, 0, 0));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        if (button.getIcon() != null) {
            button.setBorder(new EmptyBorder(5, 5, 5, 5));
        }
    }

    private void styleTextField(JTextField field) {
        Font font = getEmojiFont(14);
        field.setFont(font);
        field.setBorder(new EmptyBorder(5, 10, 5, 10));
        field.setOpaque(false);
    
        field.setText("Type a message");
        field.setForeground(new Color(150, 150, 150));
    
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (field.getText().equals("Type a message")) {
                    field.setText("");
                    field.setForeground(Color.BLACK);
                }
            }
    
            @Override
            public void focusLost(FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText("Type a message");
                    field.setForeground(new Color(150, 150, 150));
                }
            }
        });
    
        field.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (field.getText().equals("Type a message")) {
                    field.setText("");
                    field.setForeground(Color.BLACK);
                }
            }
        });
    }
    
    private void addHoverEffects(AbstractButton button) {
        button.addMouseListener(new MouseAdapter() {
            private Color originalFg = button.getForeground();
            
            public void mouseEntered(MouseEvent evt) {
                if (button.getIcon() != null) {
                    button.setBackground(new Color(
                        THEME_COLOR.getRed(),
                        THEME_COLOR.getGreen(),
                        THEME_COLOR.getBlue(),
                        50
                    ));
                    button.setOpaque(true);
                }
                button.setForeground(THEME_COLOR.darker());
            }
            
            public void mouseExited(MouseEvent evt) {
                if (button.getIcon() != null) {
                    button.setOpaque(false);
                    button.setBackground(null);
                }
                button.setForeground(originalFg);
            }
    
            public void mousePressed(MouseEvent evt) {
                if (button.getIcon() != null) {
                    button.setBackground(new Color(
                        THEME_COLOR.getRed(),
                        THEME_COLOR.getGreen(),
                        THEME_COLOR.getBlue(),
                        100
                    ));
                }
                button.setForeground(THEME_COLOR.darker().darker());
            }
    
            public void mouseReleased(MouseEvent evt) {
                if (button.getIcon() != null) {
                    button.setBackground(new Color(
                        THEME_COLOR.getRed(),
                        THEME_COLOR.getGreen(),
                        THEME_COLOR.getBlue(),
                        50
                    ));
                }
                button.setForeground(THEME_COLOR.darker());
            }
        });
    }

    private void toggleEmojiPanel() {
        emojiPanelVisible = !emojiPanelVisible;
        emojiPanel.setVisible(emojiPanelVisible);
        revalidate();
    }

    private void addEmojiToMessage(String emoji) {
        String currentText = messageField.getText();
        if (currentText.equals("Type a message")) {
            messageField.setText(emoji);
            messageField.setForeground(Color.BLACK);
        } else {
            messageField.setText(currentText + emoji);
        }
        messageField.requestFocus();
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty() && !message.equals("Type a message")) {
            try {
                service.broadcastMessage(name, message);
                messageField.setText("");
            } catch (RemoteException e) {
                displayMessage("ERROR", "Failed to send message: " + e.getMessage());
            }
        }
    }

    public void displayMessage(String sender, String message) {
        SwingUtilities.invokeLater(() -> {
            StyledDocument doc = chatArea.getStyledDocument();
            try {
                LocalTime now = LocalTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
                String time = now.format(formatter);

                if (doc.getLength() > 0) {
                    doc.insertString(doc.getLength(), "\n", null);
                }

                if (sender.equals("SERVER")) {
                    doc.insertString(doc.getLength(), "[" + time + "] ", doc.getStyle("Time"));
                    doc.insertString(doc.getLength(), message, doc.getStyle("Server"));
                } else {
                    doc.insertString(doc.getLength(), "[" + time + "] ", doc.getStyle("Time"));
                    doc.insertString(doc.getLength(), sender + ": ", doc.getStyle("User"));
                    insertMessageWithEmojis(doc, message);
                }

                chatArea.setCaretPosition(doc.getLength());

                if (!sender.equals(name) && !sender.equals("SERVER")) {
                    playMessageSound();
                    if (notificationsEnabled && (getExtendedState() == ICONIFIED || !isActive())) {
                        NotificationUtil.showNotification("New message from " + sender, 
                            message.length() > 20 ? message.substring(0, 20) + "..." : message);
                    }
                }
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }

    private void insertMessageWithEmojis(StyledDocument doc, String message) throws BadLocationException {
        Matcher matcher = EMOJI_PATTERN.matcher(message);
        int lastPos = 0;
        
        while (matcher.find()) {
            if (lastPos < matcher.start()) {
                doc.insertString(doc.getLength(), 
                    message.substring(lastPos, matcher.start()), 
                    doc.getStyle("Message"));
            }
            
            String emoji = matcher.group();
            ImageIcon emojiIcon = getTwemojiImage(emoji);
            
            if (emojiIcon != null) {
                Style emojiStyle = doc.addStyle("Twemoji", null);
                StyleConstants.setIcon(emojiStyle, emojiIcon);
                doc.insertString(doc.getLength(), " ", emojiStyle);
            } else {
                Style fallbackStyle = doc.addStyle("EmojiFallback", doc.getStyle("Message"));
                StyleConstants.setFontFamily(fallbackStyle, getEmojiFont(14).getFamily());
                doc.insertString(doc.getLength(), emoji, fallbackStyle);
            }
            
            lastPos = matcher.end();
        }
        
        if (lastPos < message.length()) {
            doc.insertString(doc.getLength(), 
                message.substring(lastPos), 
                doc.getStyle("Message"));
        }
    }

    public void updateClientList(List<String> clients) {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            for (String client : clients) {
                userListModel.addElement(new UserListItem(client, true));
            }
        });
    }

    private void disconnect() {
        try {
            if (service != null) {
                service.unregisterClient(name);
            }
            NotificationUtil.removeTrayIcon();
        } catch (RemoteException e) {
            System.err.println("Error disconnecting: " + e.getMessage());
        }
    }

    private static class UserListItem {
        private final String username;
        private final boolean online;

        public UserListItem(String username, boolean online) {
            this.username = username;
            this.online = online;
        }

        public String getUsername() {
            return username;
        }

        public boolean isOnline() {
            return online;
        }
    }

    private class UserListRenderer extends DefaultListCellRenderer {
        private final ImageIcon ONLINE_ICON = createStatusIcon(new Color(100, 200, 100));
        private final ImageIcon OFFLINE_ICON = createStatusIcon(new Color(180, 180, 180));

        private ImageIcon createStatusIcon(Color color) {
            BufferedImage image = new BufferedImage(14, 14, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = image.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillOval(1, 1, 12, 12);
            g2.setColor(new Color(240, 240, 240));
            g2.drawOval(1, 1, 12, 12);
            g2.dispose();
            return new ImageIcon(image);
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
            if (value instanceof UserListItem) {
                UserListItem item = (UserListItem) value;
                label.setText(item.getUsername());
                label.setIcon(item.isOnline() ? ONLINE_ICON : OFFLINE_ICON);
                if (isSelected) {
                    label.setBackground(new Color(THEME_COLOR.getRed(), THEME_COLOR.getGreen(), THEME_COLOR.getBlue(), 150));
                    label.setForeground(Color.WHITE);
                } else {
                    label.setBackground(Color.WHITE);
                    label.setForeground(new Color(60, 60, 60));
                }
                label.setBorder(new EmptyBorder(8, 10, 8, 0));
            }
            return label;
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java ChatClientGUI <username> <server-ip>");
            System.exit(1);
        }
        String name = args[0];
        String serverIP = args[1];
        SwingUtilities.invokeLater(() -> new ChatClientGUI(name, serverIP));
    }
}