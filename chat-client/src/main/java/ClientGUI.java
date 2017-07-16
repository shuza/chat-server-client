import javax.swing.*;

import java.awt.*;
import java.awt.event.*;

/**
 * Created by Boka on 17-Jul-17.
 */

public class ClientGUI extends JFrame implements ActionListener {

    private static final long serialVersionUID = 1L;
    // will first hold "Username:", later on "Enter message"
    private JLabel label;
    // to hold the Username and later on the messages
    public JTextField tf;
    // to hold the server address an the port number
    private JTextField tfServer, tfPort, tfDestenationId;
    // to Logout and get the list of the users
    private JButton login, logout, users;
    // for the chat room
    private JTextArea ta;
    // if it is for connection
    private boolean connected;
    // the Client object
    public Client client;
    // the default port number
    private int defaultPort;
    private String defaultHost;

    // Constructor connection receiving a socket number
    ClientGUI(String host, int port) {

        setTitle("This is for client GUI");
        defaultPort = port;
        defaultHost = host;

        // The NorthPanel with:
        JPanel mainWindow = new JPanel(new GridLayout(4, 1));
        // the server name anmd the port number
        JPanel serverAndPort = new JPanel(new GridLayout(1, 5, 1, 3));
        // the two JTextField with default value for server address and port
        // number
        tfServer = new JTextField(host);
        tfPort = new JTextField("" + port);
        tfPort.setHorizontalAlignment(SwingConstants.RIGHT);

        serverAndPort.add(new JLabel("Server Address:  "));
        serverAndPort.add(tfServer);
        serverAndPort.add(new JLabel("Port Number:  "));
        serverAndPort.add(tfPort);
        serverAndPort.add(new JLabel(""));
        // adds the Server an port field to the GUI
        mainWindow.add(serverAndPort);

        // the Label and the TextField
        // label = new JLabel("Enter your username below",
        // SwingConstants.RIGHT);
        // northPanel.add(label);

        // destenation id and show user to enter their message
        serverAndPort = new JPanel(new GridLayout(1, 2));
        serverAndPort.add(new JLabel("Destenation id:  "));
        tfDestenationId = new JTextField("1");
        tfDestenationId.setHorizontalAlignment(SwingConstants.RIGHT);
        serverAndPort.add(tfDestenationId);
        mainWindow.add(serverAndPort);
        JLabel l = new JLabel("Enter your message below and press enter");
        l.setHorizontalAlignment(SwingConstants.CENTER);
        mainWindow.add(l);

        tf = new JTextField("Shuza");
        tf.setBackground(Color.WHITE);
        tf.setEditable(true);

        mainWindow.add(tf);
        add(mainWindow, BorderLayout.NORTH);

        // The CenterPanel which is the chat room
        ta = new JTextArea("\t\tYour login and chat activity\n", 80, 80);
        JPanel centerPanel = new JPanel(new GridLayout(1, 1));
        centerPanel.add(new JScrollPane(ta));
        ta.setEditable(false);
        add(centerPanel, BorderLayout.CENTER);

        // the 3 buttons
        login = new JButton("Login");
        login.addActionListener(this);
        logout = new JButton("Logout");
        logout.addActionListener(this);
        logout.setEnabled(false); // you have to login before being able to
        // logout
        users = new JButton("User details");
        users.addActionListener(this);
        users.setEnabled(false); // you have to login before being able to Who
        // is in

        JPanel southPanel = new JPanel();
        southPanel.add(login);
        southPanel.add(logout);
        southPanel.add(users);
        add(southPanel, BorderLayout.SOUTH);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(600, 600);
        setVisible(true);
        tf.requestFocus();

    }

    // called by the Client to append text in the TextArea
    void append(String str) {
        ta.append(str);
        ta.setCaretPosition(ta.getText().length() - 1);
    }

    // called by the GUI is the connection failed
    // we reset our buttons, label, textfield
    void connectionFailed() {
        login.setEnabled(true);
        logout.setEnabled(false);
        users.setEnabled(false);
        // label.setText("Enter your username below");
        tf.setText("Anonymous");
        // reset port number and host name as a construction time
        tfPort.setText("" + defaultPort);
        tfServer.setText(defaultHost);
        // let the user change them
        tfServer.setEditable(false);
        tfPort.setEditable(false);
        // don't react to a <CR> after the username
        tf.removeActionListener(this);
        connected = false;
    }

    /*
     * Button or JTextField clicked
     */
    public void actionPerformed(ActionEvent e) {
        Object o = e.getSource();
        // if it is the Logout button
        if (o == logout) {
            client.sendMessage(new ChatMessage(ChatMessage.LOGOUT, ""));
            setTitle("Client chat");
            return;
        }
        // if it the who is in button
        if (o == users) {
            client.sendMessage(new ChatMessage(ChatMessage.USERS, ""));
            return;
        }

        // ok it is coming from the JTextField
        if (connected) {
            int destenationId = 0;
            try {
                destenationId = Integer.parseInt(tfDestenationId.getText()
                        .toString());
            } catch (Exception ex) {
            } finally {
                client.setDestenation(destenationId);
                //client.sendMessage(new ChatMessage(ChatMessage.MESSAGE, tf.getText(), destenationId));

                String msg = tf.getText().toString();
                client.createPacketAndSend(client, msg);
                append("Me: " + tf.getText() + "\n");
                tf.setText("");
            }
            return;
        }

        if (o == login) {
            // ok it is a connection request
            String username = tf.getText().trim();
            // empty username ignore it
            if (username.length() == 0)
                return;
            // empty serverAddress ignore it
            String server = tfServer.getText().trim();
            if (server.length() == 0)
                return;
            // empty or invalid port numer, ignore it
            String portNumber = tfPort.getText().trim();
            if (portNumber.length() == 0)
                return;
            int port = 0;
            try {
                port = Integer.parseInt(portNumber);
            } catch (Exception en) {
                return; // nothing I can do if port number is not valid
            }

            // try creating a new Client with GUI
            client = new Client(server, port, username, this);
            // test if we can start the Client
            if (!client.start())
                return;
            tf.setText("");
            connected = true;

            // disable login button
            login.setEnabled(false);
            // enable the 2 buttons
            logout.setEnabled(true);
            users.setEnabled(true);
            // disable the Server and Port JTextField
            tfServer.setEditable(false);
            tfPort.setEditable(false);
            // Action listener for when the user enter a message
            tf.addActionListener(this);
            setTitle(username + "  id:" + client.id);
        }

    }

    // to start the whole thing the server
    public static void main(String[] args) {
        new ClientGUI("localhost", 1500);
    }

}
