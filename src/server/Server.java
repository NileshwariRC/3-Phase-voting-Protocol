package server;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

//check server Id 1/5
public class Server {
	private boolean serverLocked;
	private DataInputStream dis;
	private DataOutputStream dos;
	private Thread listnerThread;
	private int serverId;

	public Server(int id) throws Exception {
		this.serverId = id;
		InputStream readConfig = new FileInputStream("config.properties");
		Properties prop = new Properties();
		prop.load(readConfig);
		//check server Id 2/5
		String serverPort = prop.getProperty("SERVER" + serverId + "_PORT");
		System.out.println("Server port: " + serverPort);
		ServerSocket serverSocket = new ServerSocket(Integer.valueOf(serverPort));
		Listen(serverSocket, prop);
	}
	//A function which listens to SERVER messages
	private void Listen(ServerSocket serverSocket, Properties prop)
			throws IOException
	{
		System.out.println("Server " + serverId +  " started");
		while(true)
		{
			Socket socket = serverSocket.accept();
			listnerThread = new Thread(new Runnable() {
				@Override
				public void run()
				{
					try {
						dis = new DataInputStream(socket.getInputStream());
						dos = new DataOutputStream(socket.getOutputStream());
						while (true) {
							String input = dis.readUTF();
							if (input != null) {
								process(socket, input, prop);
							}
						}
					}
					catch(IOException e) {
						throw new RuntimeException(e);
					}
				}
			});
			listnerThread.start();
			//s.close();
		}
	}

	private void process(Socket socket, String input, Properties prop)
			throws IOException
	{

		System.out.println("Processing input: " + input);

		if(input.startsWith("Request")) {
			if(!serverLocked) {
				//check server Id 3/5
				synchronized (Server.class) {
					if(!serverLocked) {
						serverLocked = true;
						dos.writeUTF("Grant");
						System.out.println("Grant sent to node: " + socket.getInetAddress().getHostName() + ":" + socket.getPort());
					}
					else {
						System.out.println("REJECT sent");
						dos.writeUTF("REJECT");
					}
				}
			}
		}
		else if(input.equalsIgnoreCase("Prepare")) {
			System.out.println("Sending ACK");
			dos.writeUTF("ACKNOWLEDGED");
		}

		else if(input.startsWith("Commit")) {
			String[] token = input.split(":");
			String message = token[1];
			int objectNumber = Integer.valueOf(token[3]);
			writeToFile(prop, message, objectNumber);
		}

		else if(input.equalsIgnoreCase("Abort")) {
			serverLocked = false;
		}

		else if(input.startsWith("Coordinator")) {
			String[] strToken = input.split("\\s+");
			String[] cohorts = strToken[2].split(",");
			int objectNumber = Integer.valueOf(strToken[4]);
			int requestId = Integer.valueOf(strToken[6]);
			sendPrepareAndCommit(socket, cohorts, prop, objectNumber, requestId);
		}

		else if(input.startsWith("Read")) {
			String[] strToken = input.split("\\s+");
			String fileId = strToken[1];
			String toRead = formFilePath(prop, Integer.valueOf(fileId));
			File file = new File(toRead);
			if(!file.exists()) {
				String message = "Error while reading file: File does not exists";
				System.out.println(message);
				dos.writeUTF(message);
				dos.flush();
			}
			else {
				byte [] byteRead  = new byte [(int)file.length()];
				System.out.println("Sending read content length of file " + file.getPath() + " to client");
				System.out.println("Content of File length: " + byteRead.length);
				dos.writeUTF("File Content " + file.getPath() + " " + byteRead.length);
				dos.flush();
			}
		}

		else {
			System.out.println("No handling for input" + input);
		}
	}

	private void sendPrepareAndCommit(Socket socket, String[] cohorts, Properties prop, int objectNumber, int requestId)
			throws IOException
	{
		boolean success = false;
		String commitMessage = null;
		for (int i = 0; i < cohorts.length; i++) {
			int currServerId = Integer.valueOf(cohorts[i]);
			Socket connectToCohort = new Socket(prop.getProperty("SERVER" + currServerId + "_HOST"), Integer.valueOf(prop.getProperty("SERVER" + currServerId + "_PORT")));
			connectToCohort.setSoTimeout(6000);
			DataInputStream serverDis = new DataInputStream(connectToCohort.getInputStream());
			DataOutputStream serverDos = new DataOutputStream(connectToCohort.getOutputStream());
			System.out.println("Sending Prepare to server: " + currServerId);
			serverDos.writeUTF("Prepare");
			System.out.println("Waiting for ACK");
			long currentTime = System.currentTimeMillis();
			String input;
			while (true) {
				input = serverDis.readUTF();
				while (input == null && Math.abs(System.currentTimeMillis() - currentTime) < 5000) {
				}
				break;
			}
			System.out.println("Done Waiting for ACK");
			if (input.equalsIgnoreCase("ACKNOWLEDGED")) {
				commitMessage = "Request "+ requestId +" received from " + socket.getInetAddress().getCanonicalHostName() + " client to server " + connectToCohort.getInetAddress().getCanonicalHostName() + "\n";
				System.out.println("Sending Commit to server: " + currServerId);
				serverDos.writeUTF("Commit:" + commitMessage + ":ObjectNumber:" + objectNumber);
				System.out.println("Sending Request Completed to client");
				dos.writeUTF("Request Completed");
				success = Boolean.TRUE;
			}
			else {
				System.out.println("Sending Request Failed to client");
				dos.writeUTF("Request Failed");
				success = Boolean.FALSE;
			}
			//serverDis.close();
			//serverDos.close();
		}
		if (success) {
			writeToFile(prop, commitMessage, objectNumber);
		}
	}

	private String formFilePath(Properties prop, int fileId)
	{
		//return prop.getProperty("filePath") + "/server" + serverId + "/" + fileId + ".txt";
		return prop.getProperty("filePath") + "/output/server" + serverId + "/" + fileId + ".txt";
	}

	private void writeToFile(Properties prop, String message, int objectNumber) throws IOException {
		//check server Id 5/5
		String path = formFilePath(prop, objectNumber);
		System.out.println("Writing to file " + path);

		File file = new File(path);
		file.createNewFile();

		System.out.println("File at path " + file.getAbsolutePath() + " exists: " + file.exists());
		synchronized (file) {
			FileWriter fileWriter;
			try {
				fileWriter = new FileWriter(file, true);
				BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
				bufferedWriter.write("File: " + message);
				System.out.println("Successfully wrote to the file.");
				System.out.println("----------------------------------------------------------------------------------------");
				serverLocked = false;
				bufferedWriter.close();
				fileWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}