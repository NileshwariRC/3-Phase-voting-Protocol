package client;

import server.Node;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class ClientHandler
    extends Thread
{
    private Socket clientHandler;
    private Node serverNode;
    private Thread receiverThread;
    private DataInputStream dis;
    private DataOutputStream dos;

    public ClientHandler(Node node) throws IOException
    {
        this.serverNode = node;
        clientHandler = new Socket(node.getHostName(), node.getPort());
        this.dis = new DataInputStream(clientHandler.getInputStream());
        this.dos = new DataOutputStream(clientHandler.getOutputStream());
        receiverThread = new Thread(new Runnable() {
            @Override
            public void run()
            {
                try {
                    Listen();
                }
                catch (SocketTimeoutException e) {
                    System.out.println("Socket Timeout Reached");
                }
                catch (IOException e) {
                    throw new RuntimeException( "Unable to start receiver thread", e);
                }
            }
        });
        receiverThread.setDaemon(true);
        receiverThread.start();

        System.out.println("Client Receiver started");
    }

    public void sendMessage(Node node, String data)
    {
        try {
            System.out.println("Sending message: " + data + " to server " + node.getHostName() + ":" + node.getPort());
            dos.writeUTF(data);
        }
        catch(IOException e) {
            throw new RuntimeException("Error sending message to server " + node, e);
        }
    }

    private void Listen() throws IOException
    {
        while(true)
        {
            String input = dis.readUTF();
            if (input != null) {
                Process(input);
            }
        }
    }

    private void Process(String input)
            throws IOException
    {
        System.out.println("Processing " + input);
        if (input.startsWith("Grant")) {
            Client.updateQuorum(serverNode);
        }
        else if (input.equals("Request Completed")) {
            Client.setAbortRecieved(Boolean.TRUE);
        }
        else if (input.equals("Request Failed")) {
            Client.setAbortRecieved(Boolean.FALSE);
        }
        else if (input.startsWith("File Content")) {
            String[] str = input.split("\\s+");
            System.out.println("Contents of file length for file " + str[2] + " is " + str[3]);
        }
        else if(input.startsWith("Error while reading")) {
        	System.out.println(input);
        }
        else {
            System.out.println("No processing found for message " + input + "in client");
        }
    }

    public void close()
            throws IOException
    {
        /*receiverThread.interrupt();
        ClientHandler.shutdownOutput();
        ClientHandler.shutdownInput();*/
    }
}
