package proxy;

import java.io.*;
import java.net.*;
import java.util.Properties;

/**
 *
 * Reference http://www.jcgonzalez.com/java-simple-proxy-socket-server-examples
 * @author jcgonzalez.com
 *
 */
public class ProxyMultiThread {
    public static void main(String[] args) {
        try {
            /*if (args.length != 3)
                throw new IllegalArgumentException("insuficient arguments");
            // and the local port that we listen for connections on*/
            InputStream input = new FileInputStream("config.properties");
            Properties prop = new Properties();
            prop.load(input);
            for (int i = 1; i < 8; i++) {
                String host = prop.getProperty("SERVER" + i + "_HOST");
                int remoteport = Integer.valueOf(prop.getProperty("SERVER" + i + "_PORT"));
                int localport = Integer.valueOf(prop.getProperty("SERVER" + i + "_PORT_PROXY"));
                Boolean blocked = Boolean.valueOf(prop.getProperty("SERVER" + i + "_BLOCKED"));
                // Print a start-up message
                System.out.println("Starting proxy for " + host + ":" + remoteport
                        + " on port " + localport);
                new Thread(new Runnable() {
                    @Override
                    public void run()
                    {
                        try {
                            ServerSocket server = new ServerSocket(localport);
                            while (true) {
                                new ThreadProxy(server.accept(), host, remoteport, blocked);
                            }
                        }
                        catch(Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }).start();
            }
        } catch (Exception e) {
            System.err.println(e);
            System.err.println("Usage: java ProxyMultiThread "
                    + "<host> <remoteport> <localport>");
        }
    }
}
/**
 * Handles a socket connection to the proxy server from the client and uses 2
 * threads to proxy between server and client
 *
 * @author jcgonzalez.com
 *
 */
class ThreadProxy extends Thread {
    private Socket sClient;
    private final String SERVER_URL;
    private final int SERVER_PORT;
    private final boolean blocked;
    ThreadProxy(Socket sClient, String ServerUrl, int ServerPort, boolean blocked) {
        this.SERVER_URL = ServerUrl;
        this.SERVER_PORT = ServerPort;
        this.sClient = sClient;
        this.blocked = blocked;
        this.start();
    }
    @Override
    public void run() {
        try {
            final byte[] request = new byte[1024];
            byte[] reply = new byte[4096];
            final InputStream inFromClient = sClient.getInputStream();
            final OutputStream outToClient = sClient.getOutputStream();
            Socket client = null, server = null;
            // connects a socket to the server
            try {
                server = new Socket(SERVER_URL, SERVER_PORT);
            } catch (IOException e) {
                PrintWriter out = new PrintWriter(new OutputStreamWriter(
                        outToClient));
                out.flush();
                throw new RuntimeException(e);
            }
            // a new thread to manage streams from server to client (DOWNLOAD)
            final InputStream inFromServer = server.getInputStream();
            final OutputStream outToServer = server.getOutputStream();
            // a new thread for uploading to the server
            new Thread() {
                public void run() {
                    int bytes_read;
                    try {
                        while ((bytes_read = inFromClient.read(request)) != -1) {
                            if (!blocked) {
                                outToServer.write(request, 0, bytes_read);
                                outToServer.flush();
                            }
                        }
                    } catch (IOException e) {
                    }
                    try {
                        outToServer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }.start();
            // current thread manages streams from server to client (DOWNLOAD)
            int bytes_read;
            try {
                while ((bytes_read = inFromServer.read(reply)) != -1) {
                    if (!blocked) {
                        outToClient.write(reply, 0, bytes_read);
                        outToClient.flush();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (server != null)
                        server.close();
                    if (client != null)
                        client.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            outToClient.close();
            sClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}