import javax.swing.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Jeppe Vinberg on 15-04-2016.
 * <p>
 * For a peer acting as server, the resposibility of the ConnectionManager is to await
 * incoming connections from clients, and delegate a TextEventSender and TextEventCapturer to handle communication with
 * this socket. It is also responsible for initializing all server related processes and objects.
 */
public class ConnectionManager implements Runnable, DisconnectHandler {


    private ServerSocket serverSocket; // The ServerSocket related to this server
    private LinkedBlockingQueue<MyTextEvent> incomingEvents; // A shared queue for exchanging incoming events between threads
    private LinkedBlockingQueue<MyTextEvent> outgoingEvents;
    private ServerSenderManager serverSenderManager; // A thread for managing the Sender threads of several clients
    private JTextArea textArea;

    public ConnectionManager(ServerSocket serverSocket, JTextArea textArea) {
        this.serverSocket = serverSocket;
        this.textArea = textArea;
        this.incomingEvents = new LinkedBlockingQueue<>();
        this.outgoingEvents = null;
        this.serverSenderManager = new ServerSenderManager(incomingEvents, true);
        new Thread(serverSenderManager).start();

    }

    public ConnectionManager(ServerSocket serverSocket, JTextArea textArea, String IPAddress, String portNumber){
        this.serverSocket = serverSocket;
        this.textArea = textArea;
        this.incomingEvents = new LinkedBlockingQueue<>();
        this.outgoingEvents = new LinkedBlockingQueue<>();
        initRootConnection(IPAddress,portNumber);
        this.serverSenderManager = new ServerSenderManager(outgoingEvents, false);
        new Thread(serverSenderManager).start();

    }


    @Override
    public void run() {
        Socket socket;
        while (true) {
            socket = waitForConnectionFromClient(serverSocket);
            if (socket != null) {
                System.out.println("New connection established to client " + socket);
                initClientThreads(socket);
            } else {
                System.out.println("Connection manager terminated");
                break;
            }
        }
    }

    private void initRootConnection(String IPAddress, String portNumber) {
        Socket rootSocket = connectToServer(IPAddress, portNumber);
        TextEventSender sender = new TextEventSender(rootSocket, incomingEvents);
        TextEventReceiver receiver = new TextEventReceiver(rootSocket, outgoingEvents, sender);
        Thread senderThread = new Thread(sender);
        Thread receiverThread = new Thread(receiver);
        senderThread.start();
        receiverThread.start();
    }

    private Socket connectToServer(String serverAddress, String portNumber) {
        Socket socket = null;
        try {
            socket = new Socket(serverAddress, Integer.parseInt(portNumber));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return socket;
    }

    private void initClientThreads(Socket socket) {
        TextEventSender sender = new TextEventSender(socket);
        TextEventReceiver receiver = new TextEventReceiver(socket, incomingEvents, sender);
        Thread senderThread = new Thread(sender);
        Thread receiverThread = new Thread(receiver);
        senderThread.start();
        receiverThread.start();
        try {
            serverSenderManager.addSender(sender, textArea);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private Socket waitForConnectionFromClient(ServerSocket serverSocket) {
        Socket res = null;
        try {
            res = serverSocket.accept();
        } catch (IOException e) {
            // We return null on IOExceptions
        }
        return res;
    }

    @Override
    public void disconnect() throws InterruptedException {
        incomingEvents.put(new ShutDownTextEvent(false));
    }
}