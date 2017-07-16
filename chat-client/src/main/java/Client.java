import java.net.*;
import java.io.*;
import java.util.*;

/**
 * Created by Boka on 17-Jul-17.
 */
public class Client {
    public int id, destenationId;

    //public static Client client;

    // for I/O
    private ObjectInputStream sInput; // to read from the socket
    private ObjectOutputStream sOutput; // to write on the socket
    private Socket socket;

    private static boolean isSendingMessage;
    static String[] packetMsg; // track packet data

    // if I use a GUI or not
    private ClientGUI cg;

    // the server, the port and the username
    private String server, username;
    private int port;

    private String[] incomingPackets;

    // constractor for console mode
    Client(String server, int port, String username) {
        // which calls the common constructor with the GUI set to null
        this(server, port, username, null);
    }

    // constractor for GUI mode
    Client(String server, int port, String username, ClientGUI cg) {
        this.server = server;
        this.port = port;
        this.username = username;
        this.cg = cg;
        destenationId = 0;
    }

    public boolean start() {
        // try to connect to the server
        try {
            socket = new Socket(server, port);
        } catch (Exception ec) {
            display("Error connectiong to server:" + ec);
            return false;
        }

        String msg = "Connection accepted " + socket.getInetAddress() + ":" + socket.getPort();
        display(msg);

        try {
            sInput = new ObjectInputStream(socket.getInputStream());
            sOutput = new ObjectOutputStream(socket.getOutputStream());
        } catch (Exception eIO) {
            display("Exception creating new Input/output Streams: " + eIO);
            return false;
        }

        // Send our username to the server this is the only message that we
        // will send as a String. All other messages will be ChatMessage objects
        try {
            sOutput.writeObject(username);
            id = Integer.parseInt((String) sInput.readObject());
            // creates the Thread to listen from the server
            new ListenFromServer().start();
        } catch (Exception eIO) {
            display("Exception doing login : " + eIO);
            disconnect();
            return false;
        }

        // success we inform the caller that it worked
        return true;
    }

    private void display(String msg) {
        if (cg == null)
            System.out.println(msg);
        else
            cg.append(msg + "\n");
    }

    void sendMessage(ChatMessage msg) {
        try {
            sOutput.writeObject(msg);
        } catch (IOException e) {
            display("Exception writing to server: " + e);
        }
    }

    /*
     * When something goes wrong Close the Input/Output streams and disconnect
     * not much to do in the catch clause
     */
    private void disconnect() {
        try {
            if (sInput != null)
                sInput.close();
        } catch (Exception e) {
        } // not much else I can do
        try {
            if (sOutput != null)
                sOutput.close();
        } catch (Exception e) {
        } // not much else I can do
        try {
            if (socket != null)
                socket.close();
        } catch (Exception e) {
        } // not much else I can do

        // inform the GUI
        if (cg != null)
            cg.connectionFailed();

    }

    public static void main(String[] args) {
        // default values
        int portNumber = 1500;
        String serverAddress = "localhost";
        String userName = "Shuza";

        // depending of the number of arguments provided we fall through
        switch (args.length) {
            // > javac Client username portNumber serverAddr
            case 3:
                serverAddress = args[2];
                // > javac Client username portNumber
            case 2:
                try {
                    portNumber = Integer.parseInt(args[1]);
                } catch (Exception e) {
                    System.out.println("Invalid port number.");
                    System.out.println("Usage is: > java Client [username] [portNumber] [serverAddress]");
                    return;
                }
                // > javac Client username
            case 1:
                userName = args[0];
                // > java Client
            case 0:
                break;
            // invalid number of arguments
            default:
                System.out.println("Usage is: > java Client [username] [portNumber] [serverAddress]");
                return;
        }
        // create the Client object
        Client client = new Client(serverAddress, portNumber, userName);
        // test if we can start the connection to the Server
        // if it failed nothing we can do
        if (!client.start())
            return;

        // wait for messages from user
        Scanner scan = new Scanner(System.in);
        // loop forever for message from the user
        while (true) {
            System.out.print("> ");
            // read message from user
            String msg = scan.nextLine();
            // logout if message is LOGOUT
            if (msg.equalsIgnoreCase("LOGOUT")) {
                client.sendMessage(new ChatMessage(ChatMessage.LOGOUT, ""));
                // break to do the disconnect
                break;
            }
            // message WhoIsIn
            else if (msg.equalsIgnoreCase("WHOISIN")) {
                client.sendMessage(new ChatMessage(ChatMessage.USERS, ""));
            } else if (msg.equalsIgnoreCase("SETDESTENATION")) {
                System.out.print("Enter destenation user id: ");
                try {
                    client.destenationId = Integer.parseInt(scan.nextLine());
                } catch (Exception e) {
                    client.destenationId = 0;
                }
                System.out.println("destenation set to " + client.destenationId);
                continue;
            } else { // default to ordinary message
                client.createPacketAndSend(client, msg);
            }
        }
        // done disconnect
        client.disconnect();
    }

    public void setDestenation(int destenation) {
        destenationId = destenation;
    }

    public void createPacketAndSend(Client client, String msg, int source, int destenation) {
        client.id = source;
        client.destenationId = destenation;
        createPacketAndSend(client, msg);
    }

	/*
     * public int getClientId(){ return client.id; } public int
	 * getClientDestenationId(){ return client.destenationId; }
	 */

    public void createPacketAndSend(Client client, String msg) {
        isSendingMessage = true;
        packetMsg = msg.split("(?<=\\G.{4})");

        ChatMessage ackPacket = new ChatMessage(ChatMessage.ACK, client.id, client.destenationId);
        ackPacket.setAckType(ChatMessage.PACKET_LENGTH);
        ackPacket.setAckNo(packetMsg.length);
        client.sendMessage(ackPacket);
        System.out.println("packetMsg length: " + packetMsg.length);

        for (int i = 0; i < packetMsg.length; i++) {
            ChatMessage cm = new ChatMessage(ChatMessage.MESSAGE, client.id, client.destenationId);
            cm.setMessage(packetMsg[i]);
            cm.setAckNo(i);
            client.sendMessage(cm);
        }
        ChatMessage ackComplete = new ChatMessage(ChatMessage.COMPLETE, client.id, client.destenationId);
        ackComplete.setMessage(client.username);
        client.sendMessage(ackComplete);
        System.out.println("complete type send");
    }

    // always read from server
    class ListenFromServer extends Thread {

        public void run() {
            while (true) {
                try {
                    ChatMessage chatMessage = (ChatMessage) sInput.readObject();
                    switch (chatMessage.getType()) {
                        case ChatMessage.USERS:
                            if (cg == null) {
                                System.out.println(chatMessage.getMessage());
                                System.out.print("> ");
                            } else {
                                cg.append(chatMessage.getMessage());
                            }
                            break;

                        case ChatMessage.MESSAGE:
                            // System.out.println("incoming packet no: " +
                            // chatMessage.getAckNo() + "\t" +
                            // chatMessage.getMessage());
                            incomingPackets[chatMessage.getAckNo()] = chatMessage.getMessage();
                            break;

                        case ChatMessage.ACK:
                            if (chatMessage.getAckType() == chatMessage.PACKET_LENGTH) {
                                System.out.println("incoming length: " + chatMessage.getAckNo());
                                incomingPackets = new String[chatMessage.getAckNo()];
                            } else if (chatMessage.getAckType() == chatMessage.ERROR_CODE) {
                                System.out.println("resend packet no " + chatMessage.getAckNo());
                                ChatMessage resendMsg = new ChatMessage(ChatMessage.MESSAGE, chatMessage.getDestenationId(),
                                        chatMessage.getSourceId());
                                resendMsg.setMessage(packetMsg[chatMessage.getAckNo()]);
                                resendMsg.setAckNo(chatMessage.getAckNo());
                                sendMessage(resendMsg);

                                ChatMessage ackComplete = new ChatMessage(ChatMessage.COMPLETE, chatMessage.getDestenationId(),
                                        chatMessage.getSourceId());
                                ackComplete.setMessage(username);
                                sendMessage(ackComplete);
                            } else {

                                isSendingMessage = false;
                            }
                            break;

                        case ChatMessage.COMPLETE:
                            System.out.println("check if packet complete");
                            boolean isError = false;
                            String msg = chatMessage.getMessage() + ": ";
                            for (int i = 0; i < incomingPackets.length; i++) {
                                if (incomingPackets[i] == null) {
                                    ChatMessage ctMsg = new ChatMessage(ChatMessage.ACK, id, chatMessage.getSourceId());
                                    ctMsg.setAckType(ChatMessage.ERROR_CODE);
                                    ctMsg.setAckNo(i);
                                    sendMessage(ctMsg);
                                    isError = true;
                                    System.out.println("packet missing at " + i);
                                    break;
                                } else {
                                    msg += incomingPackets[i];
                                }
                            }
                            if (!isError) {
                                if (cg == null) {
                                    System.out.println(msg);
                                    System.out.print("> ");
                                } else {
                                    cg.append(msg + "\n");
                                }
                                isSendingMessage = false;
                            }
                            break;

                    }

                } catch (IOException e) {
                    display("Server has close the connection: " + e);
                    if (cg != null)
                        cg.connectionFailed();
                    break;
                }
                // can't happen with a String object but need the catch anyhow
                catch (ClassNotFoundException e2) {
                }
            }
        }
    }
}
