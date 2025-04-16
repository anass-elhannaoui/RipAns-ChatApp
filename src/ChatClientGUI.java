import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import javax.swing.border.*;
import javax.swing.plaf.basic.BasicTextFieldUI;
import javax.swing.text.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

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

    // Login dialog components
    private JDialog loginDialog;
    private JTextField loginUsernameField;
    private JTextField loginServerIPField;
    private JButton loginButton;

    // Twemoji settings
    private static final String TWEMOJI_CDN = "https://twemoji.maxcdn.com/v/latest/72x72/";
    private static final String TWEMOJI_EXT = ".png";
    private static final Map<String, ImageIcon> emojiCache = new HashMap<>();
    private static final Pattern EMOJI_PATTERN = Pattern.compile(
        "(?:[\uD83C-\uDBFF\uDC00-\uDFFF]+|" +
        "[\u20E3\uFE0F]|" +
        "(?:[\uD83D\uD83E][\uDC00-\uDFFF]|" +
        "[\uD83C][\uDFFB-\uDFFF]|" +
        "[\uD83C][\uDDE6-\uDDFF][\uD83C][\uDDE6-\uDDFF])" +
        ")+"
    );

    public ChatClientGUI() {
        applyTheme("Ocean Wave");
        initializeLoginUI();
        setAppIcon();
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

    private void initializeLoginUI() {
        loginDialog = new JDialog(this, "Login", true);
        loginDialog.setSize(400, 300);
        loginDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        loginDialog.setLocationRelativeTo(null);
        loginDialog.setLayout(new BorderLayout(10, 10));
        
        JPanel loginPanel = new JPanel();
        loginPanel.setLayout(new BoxLayout(loginPanel, BoxLayout.Y_AXIS));
        loginPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        loginPanel.setBackground(BACKGROUND_COLOR);
        
        // Title label
        JLabel titleLabel = new JLabel("Chat Client Login", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Username field
        JPanel usernamePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        usernamePanel.setBackground(BACKGROUND_COLOR);
        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setFont(DEFAULT_FONT);
        loginUsernameField = new JTextField(20);
        loginUsernameField.setFont(DEFAULT_FONT);
        loginUsernameField.setBorder(new EmptyBorder(5, 10, 5, 10));
        loginUsernameField.setOpaque(false);
        usernamePanel.add(usernameLabel);
        usernamePanel.add(loginUsernameField);
        
        // Server IP field
        JPanel serverIPPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        serverIPPanel.setBackground(BACKGROUND_COLOR);
        JLabel serverIPLabel = new JLabel("Server IP:");
        serverIPLabel.setFont(DEFAULT_FONT);
        loginServerIPField = new JTextField(20);
        loginServerIPField.setFont(DEFAULT_FONT);
        loginServerIPField.setText("localhost");
        loginServerIPField.setBorder(new EmptyBorder(5, 10, 5, 10));
        loginServerIPField.setOpaque(false);
        serverIPPanel.add(serverIPLabel);
        serverIPPanel.add(loginServerIPField);
        
        // Login button
        loginButton = createStyledButton("Connect", "Connect to chat server");
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginButton.addActionListener(e -> attemptLogin());
        
        // Add components to login panel
        loginPanel.add(Box.createVerticalStrut(10));
        loginPanel.add(titleLabel);
        loginPanel.add(Box.createVerticalStrut(20));
        loginPanel.add(usernamePanel);
        loginPanel.add(Box.createVerticalStrut(10));
        loginPanel.add(serverIPPanel);
        loginPanel.add(Box.createVerticalStrut(30));
        loginPanel.add(loginButton);
        
        // Add action listener for Enter key
        loginServerIPField.addActionListener(e -> attemptLogin());
        loginUsernameField.addActionListener(e -> attemptLogin());
        
        loginDialog.add(loginPanel, BorderLayout.CENTER);
        loginDialog.setVisible(true);
    }
    
    private void attemptLogin() {
        String username = loginUsernameField.getText().trim();
        String serverIP = loginServerIPField.getText().trim();
        
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(loginDialog, 
                "Please enter a username", 
                "Login Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (serverIP.isEmpty()) {
            JOptionPane.showMessageDialog(loginDialog, 
                "Please enter server IP", 
                "Login Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        this.name = username;
        
        // Try to connect to server
        try {
            System.setProperty("java.security.policy", "client.policy");
            Registry registry = LocateRegistry.getRegistry(serverIP, 1099);
            service = (ChatService) registry.lookup("ChatService");
            callback = new ClientCallbackImpl(this);
            service.registerClient(name, callback);
            
            // If successful, close login dialog and open main UI
            loginDialog.dispose();
            initializeMainUI();
            displayMessage("SERVER", "Connected to chat server as " + name);
        } catch (RemoteException | NotBoundException e) {
            JOptionPane.showMessageDialog(loginDialog,
                "Error connecting to server: " + e.getMessage(),
                "Connection Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void initializeMainUI() {
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
    
        String[] emojis = {
            "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣",
            "😊", "😇", "🙂", "🙃", "😉", "😌", "😍", "🥰",
            "😘", "😗", "😙", "😚", "😋", "😛", "😝", "😜",
            "🤪", "🤨", "🧐", "🤓", "😎", "🥸", "🤩", "🥳"
        };
    
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
                
                InputStream is = getClass().getResourceAsStream(
                    "/assets/twemoji/72x72/" + codePoint + ".png");
                if (is == null) {
                    is = getClass().getResourceAsStream(
                        "/twemoji/" + codePoint + ".png");
                }
                
                if (is != null) {
                    try {
                        BufferedImage image = ImageIO.read(is);
                        return new ImageIcon(image.getScaledInstance(16, 16, Image.SCALE_SMOOTH));
                    } finally {
                        is.close();
                    }
                }
                
                return null;
            } catch (IOException ex) {
                if (DEBUG_MODE) {
                    System.err.println("Error loading emoji: " + ex.getMessage());
                }
                return null;
            }
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

    private void updateUI() {
        SwingUtilities.invokeLater(() -> {
            if (needsFullUIUpdate) {
                SwingUtilities.updateComponentTreeUI(this);
                needsFullUIUpdate = false;
                
                if (messageField != null) {
                    applyEmojiTextFieldUI(messageField);
                }
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
                THEME_COLOR = new Color(100, 140, 235);
                THEME_SECONDARY = new Color(80, 120, 215);
                BACKGROUND_COLOR = new Color(235, 245, 255);
                MESSAGE_PANEL_COLOR = new Color(225, 235, 245);
                currentGradient = new GradientPaint(0, 0, new Color(90, 120, 205), getWidth(), 0, new Color(70, 100, 180));
                break;
            case "Emerald Forest":
                THEME_COLOR = new Color(80, 160, 100);
                THEME_SECONDARY = new Color(60, 120, 80);
                BACKGROUND_COLOR = new Color(230, 245, 230);
                MESSAGE_PANEL_COLOR = new Color(210, 230, 220);
                currentGradient = new GradientPaint(0, 0, new Color(70, 140, 90), getWidth(), 0, new Color(50, 100, 70));
                break;
            case "Neutral Gray":
                THEME_COLOR = new Color(128, 128, 128);
                THEME_SECONDARY = new Color(100, 100, 100);
                BACKGROUND_COLOR = new Color(240, 240, 240);
                MESSAGE_PANEL_COLOR = new Color(230, 230, 230);
                currentGradient = new GradientPaint(0, 0, new Color(150, 150, 150), getWidth(), 0, new Color(100, 100, 100));
                break;
            case "Soft Twilight":
                THEME_COLOR = new Color(100, 100, 200);
                THEME_SECONDARY = new Color(80, 80, 180);
                BACKGROUND_COLOR = new Color(240, 240, 250);
                MESSAGE_PANEL_COLOR = new Color(230, 230, 240);
                currentGradient = new GradientPaint(0, 0, new Color(100, 100, 200), getWidth(), 0, new Color(80, 80, 180));
                break;
            case "Graphite Echo":
                THEME_COLOR = new Color(100, 100, 120);
                THEME_SECONDARY = new Color(80, 80, 100);
                BACKGROUND_COLOR = new Color(230, 230, 235);
                MESSAGE_PANEL_COLOR = new Color(220, 220, 225);
                currentGradient = new GradientPaint(0, 0, new Color(100, 100, 120), getWidth(), 0, new Color(80, 80, 100));
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
        
        if (messageField != null) {
            applyEmojiTextFieldUI(messageField);
        }
        
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
            "Ocean Wave", "Emerald Forest", 
            "Neutral Gray", 
            "Soft Twilight",
            "Graphite Echo"
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
        if (field == null) return;
        
        field.setFont(DEFAULT_FONT);
        field.setBorder(new EmptyBorder(5, 10, 5, 10));
        field.setOpaque(false);
        
        // Only apply custom UI to message field, not login fields
        if (field == messageField) {
            applyEmojiTextFieldUI(field);
        }
        
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
    }
    
    private void applyEmojiTextFieldUI(JTextField field) {
        if (field == null) return;
        
        field.setUI(new BasicTextFieldUI() {
            @Override
            protected void paintSafely(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, 
                                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                g2.setColor(new Color(255, 255, 255, 200));
                g2.fillRoundRect(0, 0, field.getWidth(), field.getHeight(), 10, 10);

                int caretX = paintTextWithEmojis(g2);

                if (field.isEditable() && field.isEnabled()) {
                    paintCaret(g2, caretX);
                }

                if (field.getBorder() != null) {
                    field.getBorder().paintBorder(field, g, 0, 0, 
                                                  field.getWidth(), field.getHeight());
                }
            }

            private int paintTextWithEmojis(Graphics2D g2) {
                String text = field.getText();
                int caretPosition = field.getCaretPosition();
                int x = 5;
                int lastPos = 0;

                if (text.isEmpty() || text.equals("Type a message")) {
                    g2.setColor(new Color(150, 150, 150));
                    g2.setFont(DEFAULT_FONT);
                    g2.drawString("Type a message", x, 
                                  field.getBaseline(field.getWidth(), field.getHeight()));
                    return x;
                }

                Matcher matcher = EMOJI_PATTERN.matcher(text);
                while (matcher.find()) {
                    if (lastPos < matcher.start()) {
                        String regularText = text.substring(lastPos, matcher.start());
                        g2.setFont(DEFAULT_FONT);
                        g2.setColor(Color.BLACK);
                        x += drawString(g2, regularText, x, 
                                        field.getBaseline(field.getWidth(), field.getHeight()));
                    }

                    if (caretPosition >= lastPos && caretPosition < matcher.start()) {
                        return x;
                    }

                    String emojiSequence = matcher.group();
                    int sequenceLength = emojiSequence.length();
                    int pos = 0;

                    while (pos < sequenceLength) {
                        int emojiLength = 2;
                        if (pos + 3 < sequenceLength && 
                            emojiSequence.charAt(pos) == '\uD83C' && 
                            emojiSequence.charAt(pos + 1) >= '\uDDE6' && 
                            emojiSequence.charAt(pos + 1) <= '\uDDFF') {
                            emojiLength = 4;
                        }
                        emojiLength = Math.min(emojiLength, sequenceLength - pos);
                        String currentEmoji = emojiSequence.substring(pos, pos + emojiLength);

                        ImageIcon emojiIcon = getTwemojiImage(currentEmoji);
                        if (emojiIcon != null) {
                            int yOffset = (field.getFontMetrics(DEFAULT_FONT).getAscent() - emojiIcon.getIconHeight()) / 2;
                            g2.drawImage(emojiIcon.getImage(), 
                                         x, 
                                         field.getBaseline(field.getWidth(), field.getHeight()) - emojiIcon.getIconHeight() + yOffset, 
                                         emojiIcon.getIconWidth(), 
                                         emojiIcon.getIconHeight(), 
                                         null);
                            x += emojiIcon.getIconWidth();
                        } else {
                            g2.setFont(getEmojiFont(field.getFont().getSize()));
                            g2.setColor(Color.BLACK);
                            x += drawString(g2, currentEmoji, x, 
                                            field.getBaseline(field.getWidth(), field.getHeight()));
                        }
                        pos += emojiLength;
                    }

                    lastPos = matcher.end();

                    if (caretPosition >= matcher.start() && caretPosition < lastPos) {
                        return x;
                    }
                }

                if (lastPos < text.length()) {
                    String remainingText = text.substring(lastPos);
                    g2.setFont(DEFAULT_FONT);
                    g2.setColor(Color.BLACK);
                    x += drawString(g2, remainingText, x, 
                                    field.getBaseline(field.getWidth(), field.getHeight()));
                }

                if (caretPosition >= text.length()) {
                    return x;
                }

                return x;
            }

            private int drawString(Graphics2D g2, String text, int x, int y) {
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(text, x, y);
                return fm.stringWidth(text);
            }

            private void paintCaret(Graphics2D g2, int caretX) {
                try {
                    int baseline = field.getBaseline(field.getWidth(), field.getHeight());
                    FontMetrics fm = g2.getFontMetrics();
                    int caretHeight = fm.getAscent() + fm.getDescent();
                    g2.setColor(Color.BLACK);
                    g2.fillRect(caretX, baseline - fm.getAscent(), 2, caretHeight);
                } catch (Exception e) {
                    e.printStackTrace();
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
            
            String emojiSequence = matcher.group();
            int sequenceLength = emojiSequence.length();
            int pos = 0;
            
            while (pos < sequenceLength) {
                int emojiLength = 2;
                if (pos + 3 < sequenceLength && 
                    emojiSequence.charAt(pos) == '\uD83C' && 
                    emojiSequence.charAt(pos + 1) >= '\uDDE6' && 
                    emojiSequence.charAt(pos + 1) <= '\uDDFF') {
                    emojiLength = 4;
                }
                emojiLength = Math.min(emojiLength, sequenceLength - pos);
                String currentEmoji = emojiSequence.substring(pos, pos + emojiLength);
                
                ImageIcon emojiIcon = getTwemojiImage(currentEmoji);
                if (emojiIcon != null) {
                    Image img = emojiIcon.getImage();
                    int size = DEFAULT_FONT.getSize() + 2;
                    Image scaledImg = img.getScaledInstance(size, size, Image.SCALE_SMOOTH);
                    emojiIcon = new ImageIcon(scaledImg);
                    
                    Style emojiStyle = doc.addStyle("Twemoji", null);
                    StyleConstants.setIcon(emojiStyle, emojiIcon);
                    doc.insertString(doc.getLength(), " ", emojiStyle);
                } else {
                    Style fallbackStyle = doc.addStyle("EmojiFallback", doc.getStyle("Message"));
                    StyleConstants.setFontFamily(fallbackStyle, getEmojiFont(DEFAULT_FONT.getSize()).getFamily());
                    doc.insertString(doc.getLength(), currentEmoji, fallbackStyle);
                }
                pos += emojiLength;
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
        SwingUtilities.invokeLater(() -> new ChatClientGUI());
    }
}