package simplechatclient;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Scanner;

import simplechatserver.Opcode;
import simplechatserver.Packet;

/**
 *
 * @author RYTRUJILLO
 */
public class SimpleChatClient {
    final static String serverName = "localhost";
    final static int portNumber = 59999;
    static String username;
    final static Scanner scanner = new Scanner(System.in);
    
    static Socket socket;
    
    private static void processPacket(Packet packet) {
        switch (Opcode.getOpcodeById(packet.getOpcode())) {
            case CONNECTION_ACCEPTED:
                System.out.println("Successfully connected to server");
                break;
            case CONNECTION_DENIED:
                System.out.println(new String(packet.getData()));
                System.exit(0);
                break;
            case SEND_CHAT_MESSAGE:
                String message = new String(packet.getData());
                if (message.substring(0, message.indexOf(":")).equals(username)) {
                    break;
                }
            case SEND_PRIVATE_MESSAGE:
                System.out.println(new String(packet.getData()));
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
    
    private static Socket connectToServer(String username) {
        try {
            socket = new Socket(serverName, portNumber);
            sendPacket(new Packet(Opcode.SET_USERNAME.id(), username.getBytes()));
        } catch (IOException e) {
            System.out.println("Error connecting to " + serverName + ":" + portNumber + ". " + e.getMessage());
        }
        return socket;
    }
    
    public static void sendPacket(Packet packet) throws IOException {
        OutputStream outputStream = socket.getOutputStream();
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
    
    private static void readIncomingMessages() {
        while (true) {
            try {
                InputStream inputStream = socket.getInputStream();
                byte[] buffer = new byte[1024];
                int numBytes;
                numBytes = inputStream.read(buffer);
                if (numBytes < 1) {
                    continue;
                }
                
                processPacket(new Packet(buffer[0], Arrays.copyOfRange(buffer, 1, numBytes)));
            } catch (IOException e) {
                System.out.println("Connection to the server seems to have been lost.");
                System.exit(0);
            }
        }
    }
    
    public static void main(String[] args) {
        System.out.print("Please enter your username: ");
        username = scanner.nextLine();
        connectToServer(username);
        
        Thread readIncomingMessagesThread = new Thread(() -> {
            readIncomingMessages();
        });
        readIncomingMessagesThread.start();
        
        while (true) {
            String message = scanner.nextLine();
            try {
                if (message.charAt(0) == '@') {
                    sendPacket(new Packet(Opcode.SEND_PRIVATE_MESSAGE.id(), message.getBytes()));
                } else {
                    sendPacket(new Packet(Opcode.SEND_CHAT_MESSAGE.id(), (username + ": " + message).getBytes()));
                }
            } catch (IOException e) {
                System.out.println("Unable to send message. Please try again.");
            }
        }
    }

}
