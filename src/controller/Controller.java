package controller;

import client.Client;
import proxy.ProxyMultiThread;
import server.Server;

public class Controller {

	public static void main(String[] args) throws Exception {
		if (args.length < 2) {
			System.out.println("Enter Valid input: <client/server><ID>");
			return;
		}
		String clientOrServer = args[0].toLowerCase();
		int id = Integer.valueOf(args[1]);

		if(clientOrServer.equalsIgnoreCase("server")) {
			Server server = new Server(id);
		}

		if(clientOrServer.equalsIgnoreCase("client")) {
			Client client = new Client();
			try {
				client.startClient(id);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		if(clientOrServer.equalsIgnoreCase("proxy")) {
			try {
				ProxyMultiThread.main(args);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}

}
