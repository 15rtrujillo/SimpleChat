package simplechatserver;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author RYTRUJILLO
 */
public class SimpleChatServer {
    
    private static final ConcurrentHashMap<String, Socket> clientSockets = new ConcurrentHashMap<String, Socket>();
    
    private static void notifyAll(String message) {
        System.out.println(message);
        for (Socket clientSocket : clientSockets.values()) {
            try {
                sendPacket(clientSocket, new Packet(Opcode.SEND_CHAT_MESSAGE.id(), message.getBytes()));
            } catch (IOException e) {
                System.out.println("Error sending chat message");
            }
        }
    }
    
    private static void notify(Socket socket, String message) {
        System.out.println(message);
        try {
            sendPacket(socket, new Packet(Opcode.SEND_PRIVATE_MESSAGE.id(), message.getBytes()));
        } catch (IOException e) {
            System.out.println("Error sending chat message");
        }
}
    
    private static void processPacket(Socket socket, Packet packet) {
        switch (Opcode.getOpcodeById(packet.getOpcode())) {
            case SEND_CHAT_MESSAGE:
                notifyAll(new String(packet.getData()));
                break;
            case SEND_PRIVATE_MESSAGE:
                String senderName = "";
                for (String name : clientSockets.keySet()) {
                    if (clientSockets.get(name) == socket) {
                        senderName = name;
                        break;
                    }
                }
                
                String payload = new String(packet.getData());
                String otherName = payload.substring(payload.indexOf('@') + 1, payload.indexOf(' '));
                String chatMessage = senderName + " whispers: " + payload.substring(otherName.length() + 2);
                Socket otherSocket = clientSockets.get(otherName);
                if (otherSocket == null) {
                    notify(socket, "Server to " + senderName + ": Unable to send message to " + otherName + ". Perhaps they are not online.");
                } else {
                    notify(otherSocket, chatMessage);
                }
                break;
            default:
                System.out.print("Unrecognized or malformed packet with Opcode " + packet.getOpcode() + " and data ");
                for (byte datum : packet.getData()) {
                    System.out.print(datum + " ");
                }
                System.out.println("");
                break;
        } 
    }
    
    private static ServerSocket openServerSocket(int port) {
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setSoTimeout(10000);
            System.out.println("Server is now live on port " + port);
        } catch (IOException e) {
            System.out.println("Unable to open socket on port " + port + ". " + e.getMessage());
            return null;
        }
        return serverSocket;
    }
    
    private static void acceptConnections(ServerSocket serverSocket) {
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                
                // We need to read the username from the incoming connection
                Packet connectPacket = null;
                int i = 11;
                do {
                    if (i == 10) {
                        System.out.println("Never received initial connection packet. Closing connection.");
                        socket.close();
                        return;
                    }
                    connectPacket = readIncomingMessage(socket);
                    ++i;
                } while (connectPacket == null);
                
                String username = new String(connectPacket.getData(), 0, connectPacket.getData().length);
                
                // Check if we already have this username
                for (String connectedUser : clientSockets.keySet()) {
                    if (username.equalsIgnoreCase(connectedUser)) {
                        sendPacket(socket, new Packet(Opcode.CONNECTION_DENIED.id(), "There is already a user connected with that name.".getBytes()));
                        socket.close();
                        return;
                    }
                }
                
                clientSockets.put(username, socket);
                System.out.println(username + " has connected!");
                
                sendPacket(socket, new Packet(Opcode.CONNECTION_ACCEPTED.id()));
                notifyAll("Server: " + username + " has connected");
                
            } catch (IOException ioe) {}
        }
    }
    
    private static void closeConnection(Socket clientSocket) {
        for (String username : clientSockets.keySet()) {
            Socket currentSocket = clientSockets.get(username);
            if (currentSocket == clientSocket) {
                System.out.println("Closing connection with " + username);
                clientSockets.remove(username);
                notifyAll("Server: " + username + " has disconnected.");
                break;
            }
        }
        try {
            clientSocket.close();
        } catch (IOException e) {
            // Socket already closed I guess
            System.out.println("Error closing connection. It's likely the socket has already been closed.");
        }
    }
    
    private static void sendPacket(Socket clientSocket, Packet packet) throws IOException {
        OutputStream outputStream = clientSocket.getOutputStream();
        byte[] payload = new byte[packet.getData().length + 1];
        for (int i = 0; i < payload.length; ++i) {
            if (i == 0) {
                payload[i] = packet.getOpcode();
                continue;
            }
            payload[i] = packet.getData()[i-1];
        }
        outputStream.write(payload);
    }
    
    private static Packet readIncomingMessage(Socket clientSocket) {
        try {
            InputStream inputStream = clientSocket.getInputStream();
            clientSocket.setSoTimeout(500);
            
            byte[] buffer = new byte[1024];
            int numBytes;
            numBytes = inputStream.read(buffer);
            
            if (numBytes == -1) {
                closeConnection(clientSocket);
                return null;
            }
            
            return new Packet(buffer[0], Arrays.copyOfRange(buffer, 1, numBytes));
        } catch (SocketTimeoutException se) {
            return null;
        } catch (IOException e) {
            closeConnection(clientSocket);
            return null;
        }
    }
    
    private static void readIncomingMessages() {
        while (true) {
            for (Socket clientSocket : clientSockets.values()) {
                Packet packet = readIncomingMessage(clientSocket);
                if (packet == null) {
                    continue;
                }
                processPacket(clientSocket, packet);
            }
        }
    }

    public static void main(String[] args) {
        final int portNumber = 59999;
        
        ServerSocket serverSocket = openServerSocket(portNumber);
        
        Thread acceptConnectionsThread = new Thread(() -> {
            acceptConnections(serverSocket);
        });
        acceptConnectionsThread.start();
        
        Thread readIncomingMessagesThread = new Thread(() -> {
            readIncomingMessages();
        });
        readIncomingMessagesThread.start();
    }

}
