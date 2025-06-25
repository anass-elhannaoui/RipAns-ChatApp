# RMI Chat Application

## Overview
The RMI Chat Application is a distributed chat system built using Java RMI (Remote Method Invocation) technology. It allows multiple users to connect to a central chat server and communicate in real-time through both public and private messages.

## Features

### Core Functionality
- **Real-time messaging**: Send and receive messages instantly
- **User list**: See all connected users with online status indicators
- **Private messaging**: Send direct messages to specific users
- **Username management**: Change your username while connected

### User Experience
- **Modern UI**: Clean, responsive interface with theme support
- **Emoji support**: Insert emojis in messages with visual display
- **Notifications**: Visual and audio alerts for new messages
- **System tray integration**: Minimize to tray with message notifications

### Technical Features
- **Distributed architecture**: Client-server model using Java RMI
- **Fault tolerance**: Automatic detection and removal of disconnected clients
- **Custom text rendering**: Supports emoji display with fallback options
- **Theming system**: Multiple color themes with live switching

## System Requirements

- Java 11 or higher
- Network connectivity (for client-server communication)
- 500MB RAM minimum
- Screen resolution of 1024x768 or higher recommended

## Installation

### Server Setup
1. Compile the server code:
   ```
   javac ChatServiceImpl.java
   ```
2. Start the RMI registry:
   ```
   rmiregistry
   ```
3. Run the server:
   ```
   java ChatServiceImpl
   ```

### Client Setup
1. Compile the client code:
   ```
   javac ChatClientGUI.java
   ```
2. Run the client:
   ```
   java ChatClientGUI
   ```
   Or with parameters:
   ```
   java ChatClientGUI <username> <serverIP>
   ```

## Usage

### Starting the Application
1. Launch the client application
2. If no command line arguments are provided, a login dialog will appear
3. Enter your desired username and server IP address
4. Click "Login" to connect

### Interface Guide
- **Main Chat Area**: Displays all messages
- **User List**: Shows connected users on the right side
- **Message Input**: Bottom panel for composing messages
- **Emoji Picker**: Click the emoji button to insert emojis
- **Settings**: Access via the gear icon in the top-right

### Basic Operations
- **Send message**: Type in the input field and press Enter or click Send
- **Private message**: Right-click a user and select "Send Private Message"
- **Change username**: Open Settings and enter a new username
- **Change theme**: Select from available themes in Settings

## Security Considerations

- The application includes basic username validation
- All network communication uses Java RMI's built-in security
- For production use, consider implementing SSL/TLS for RMI communication

## Known Issues

- Emoji display may vary across different operating systems
- Very long messages may cause UI performance issues
- Network interruptions may require client restart

## Troubleshooting

### Common Problems
- **Connection refused**: Ensure the server is running and the correct IP is specified
- **Username taken**: Choose a different username
- **Blank screen**: Check Java version compatibility (requires Java 11+)

### Error Messages
- "Server not found": Verify the server IP and that the server is running
- "Failed to send message": Check your network connection
- "Invalid username": Username may contain invalid characters

## License

This application is provided under the MIT License. See the LICENSE file for details.

## Contributing

Contributions are welcome! Please fork the repository and submit pull requests.

## Contact

For support or questions, please contact the maintainers at anass.elhannaoui.io@gmail.com