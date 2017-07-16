import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Boka on 17-Jul-17.
 */
public class Server {
    // a unique ID for each connection
    private static int uniqueId;
    // an ArrayList to keep the list of the Client
    private ArrayList<ClientThread> al;
    // if I am in a GUI
    private ServerGUI sg;
    // to display time
    private SimpleDateFormat sdf;
    // the port number to listen for connection
    private int port;
    // the boolean that will be turned of to stop the server
    private boolean keepGoing;

    private int demon = 0;

    public Server(int port) {
        this(port, null);
    }

    public Server(int port, ServerGUI sg) {
        // GUI or not
        this.sg = sg;
        // the port
        this.port = port;
        // to display hh:mm:ss
        sdf = new SimpleDateFormat("HH:mm:ss");
        // ArrayList for the Client list
        al = new ArrayList<ClientThread>();
        uniqueId = 0;
    }

    public void start() {
        keepGoing = true;
        demon = 0;
		/* create socket server and wait for connection requests */
        try {
            // the socket used by the server
            ServerSocket serverSocket = new ServerSocket(port);

            // infinite loop to wait for connections
            while (keepGoing) {
                // format message saying we are waiting
                display("Server waiting for Clients on port " + port + ".");

                Socket socket = serverSocket.accept(); // accept connection
                // if I was asked to stop
                if (!keepGoing)
                    break;
                ClientThread t = new ClientThread(socket); // make a thread of
                // it
                al.add(t); // save it in the ArrayList
                t.start();
            }
            // I was asked to stop
            try {
                serverSocket.close();
                for (int i = 0; i < al.size(); ++i) {
                    ClientThread tc = al.get(i);
                    try {
                        tc.sInput.close();
                        tc.sOutput.close();
                        tc.socket.close();
                    } catch (IOException ioE) {
                        // not much I can do
                    }
                }
            } catch (Exception e) {
                display("Exception closing the server and clients: " + e);
            }
        }
        // something went bad
        catch (IOException e) {
            String msg = sdf.format(new Date()) + " Exception on new ServerSocket: " + e + "\n";
            display(msg);
        }
    }

    /*
     * For the GUI to stop the server
     */
    protected void stop() {
        keepGoing = false;
        // connect to myself as Client to exit statement
        // Socket socket = serverSocket.accept();
        try {
            new Socket("localhost", port);
        } catch (Exception e) {
            // nothing I can really do
        }
    }

    private void display(String msg) {
        String time = sdf.format(new Date()) + " " + msg;
        if (sg == null)
            System.out.println(time);
        else
            sg.appendEvent(time + "\n");
    }

    // send msg to destination and busy msg to other client
    private synchronized void broadcast(ChatMessage cm) {
        if(cm.getType() == ChatMessage.MESSAGE && demon == 3){
            demon = 0;
            return;
        }
        if(cm.getType() == ChatMessage.MESSAGE){
            demon++;
        }

        // display message on console or GUI
        String msg = "from: " + cm.getSourceId() + " to " + cm.getDestenationId() + " ";
        switch(cm.getType()){
            case ChatMessage.MESSAGE:
                msg += cm.getMessage();
                break;
            case ChatMessage.COMPLETE:
                msg += "check packets ACK";
                break;
            default:
                if(cm.getAckType() == ChatMessage.PACKET_LENGTH){
                    msg += "packet length " + cm.getAckNo();
                }else{
                    msg += "resend request at packet " + cm.getAckNo();
                }
        }
        msg += "\n";
        if (sg == null)
            System.out.print(
                    "from:" + cm.getSourceId() + " to " + cm.getDestenationId() + " - " + cm.getMessage() + "\n");
        else
            sg.appendRoom(msg);

        for (int i = al.size(); --i >= 0;) {
            ClientThread ct = al.get(i);

            if (cm.getDestenationId() == 0) {
                if (!ct.writeMsg(cm)) {
                    al.remove(i);
                    display("Disconnected Client " + ct.username + " removed from list.");
                }
            } else if (cm.getDestenationId() == ct.id) {
                if (!ct.writeMsg(cm)) {
                    al.remove(i);
                    display("Disconnected Client " + ct.username + " removed from list.");
                }
            } else if (cm.getDestenationId() != ct.id) {
				/*
				 * if (!ct.writeMsg("server busy\n")) { al.remove(i); display(
				 * "Disconnected Client " + ct.username + " removed from list."
				 * ); }
				 */
            }

        }
    }

    // for a client who logoff using the LOGOUT message
    synchronized void remove(int id) {
        // scan the array list until we found the Id
        for (int i = 0; i < al.size(); ++i) {
            ClientThread ct = al.get(i);
            // found it
            if (ct.id == id) {
                al.remove(i);
                return;
            }
        }
    }

    /*
     * To run as a console application just open a console window and: > java
     * Server > java Server portNumber If the port number is not specified 1500
     * is used
     */
    public static void main(String[] args) {
        // start server on port 1500 unless a PortNumber is specified
        int portNumber = 1500;
        switch (args.length) {
            case 1:
                try {
                    portNumber = Integer.parseInt(args[0]);
                } catch (Exception e) {
                    System.out.println("Invalid port number.");
                    System.out.println("Usage is: > java Server [portNumber]");
                    return;
                }
            case 0:
                break;
            default:
                System.out.println("Usage is: > java Server [portNumber]");
                return;

        }
        // create a server object and start it
        Server server = new Server(portNumber);
        server.start();
    }

    /** One instance of this thread will run for each client */
    class ClientThread extends Thread {
        // the socket where to listen/talk
        Socket socket;
        ObjectInputStream sInput;
        ObjectOutputStream sOutput;
        // my unique id (easier for deconnection)
        int id;
        // the Username of the Client
        String username;
        // the only type of message a will receive
        ChatMessage cm;
        // the date I connect
        String date;

        // Constructore
        ClientThread(Socket socket) {
            id = ++uniqueId;
            this.socket = socket;
            System.out.println("Thread trying to create Object Input/Output Streams");
            try {
                // create output first
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput = new ObjectInputStream(socket.getInputStream());
                System.out.println("i/o created");

                // read the username
                username = (String) sInput.readObject();
                display(username + " just connected.");

                sOutput.writeObject("" + id);
                // sOutput.writeObject(id);
                // System.out.println("id: " + id);
            } catch (IOException e) {
                display("Exception creating new Input/output Streams: " + e);
                return;
            }
            // have to catch ClassNotFoundException
            // but I read a String, I am sure it will work
            catch (ClassNotFoundException e) {
            }
            date = new Date().toString() + "\n";
        }

        // what will run forever
        public void run() {
            // to loop until LOGOUT
            boolean keepGoing = true;
            while (keepGoing) {
                // read a String (which is an object)
                try {
                    cm = (ChatMessage) sInput.readObject();
                } catch (IOException e) {
                    display(username + " Exception reading Streams: " + e);
                    break;
                } catch (ClassNotFoundException e2) {
                    break;
                }
                // the messaage part of the ChatMessage
                String message = cm.getMessage();

                // Switch on the type of message receive
                switch (cm.getType()) {

                    case ChatMessage.ACK:
                    case ChatMessage.MESSAGE:
                    case ChatMessage.COMPLETE:
                        broadcast(cm);
                        break;
                    case ChatMessage.LOGOUT:
                        display(username + " disconnected with a LOGOUT message.");
                        keepGoing = false;
                        break;
                    case ChatMessage.USERS:
                        // writeMsg("List of the users connected at " +
                        // sdf.format(new Date()) + "\n");
                        // scan al the users connected

                        try {
                            sOutput.writeObject(new ChatMessage(ChatMessage.USERS,
                                    "List of the users connected at " + sdf.format(new Date()) + "\n"));
                            for (int i = 0; i < al.size(); ++i) {
                                ClientThread ct = al.get(i);
                                sOutput.writeObject(new ChatMessage(ChatMessage.USERS,
                                        (i + 1) + ") " + ct.username + "\tid: " + ct.id + "\n"));
                            }
                        } catch (Exception e) {
                            display("error: " + e.getMessage());
                        }

                        break;
                }
            }
            // remove myself from the arrayList containing the list of the
            // connected Clients
            remove(id);
            close();
        }

        // try to close everything
        private void close() {
            // try to close the connection
            try {
                if (sOutput != null)
                    sOutput.close();
            } catch (Exception e) {
            }
            try {
                if (sInput != null)
                    sInput.close();
            } catch (Exception e) {
            }
            ;
            try {
                if (socket != null)
                    socket.close();
            } catch (Exception e) {
            }
        }

        /*
         * Write a String to the Client output stream
         */
        private boolean writeMsg(ChatMessage cm) {
            // if Client is still connected send the message to it
            if (!socket.isConnected()) {
                close();
                return false;
            }
            // write the message to the stream
            try {
                sOutput.writeObject(cm);
            }
            // if an error occurs, do not abort just inform the user
            catch (IOException e) {
                display("Error sending message to " + username);
                display(e.toString());
            }
            return true;
        }
    }
}