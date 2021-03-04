package client;

import server.Node;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.StringJoiner;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class Client {
	private Map<Node, ClientHandler> clientHandlerMap = new HashMap<>();
	private static volatile List<Node> quorumNodes = new ArrayList<>();
	private static volatile boolean abortRecieved = Boolean.FALSE;
	private static int randomObject;
	private static int requestId;
	private int clientId;
	private int quorum;
	private String proxy = "";

	public void startClient(int id) throws Exception
	{
		clientId = id;
		InputStream input = new FileInputStream("config.properties");
		Properties prop = new Properties();
		prop.load(input);
		int totalRequests = Integer.valueOf(prop.getProperty("noofrequests"));
		int readRequests = Integer.valueOf(prop.getProperty("readrequests"));
		this.quorum = Integer.valueOf(prop.getProperty("quorum"));
		int writeRequests = totalRequests - readRequests;
		requestId = 1;
		if (prop.getProperty("PROXY_ENABLED").equalsIgnoreCase("TRUE")) {
			proxy = "_PROXY";
		}

		while(requestId < totalRequests + 1) {
			System.out.println("----------------------------------------------------------------------------------------");
			System.out.println("Request: " + requestId);
			if (requestId < writeRequests + 1) {
				abortRecieved = false;
				List<Node> randomServers = getRandomServerList(prop);
				createServerToClientHandler(randomServers);
				if (!requestQuorum(prop, randomServers)) {
					//Send Abort if Quorum is not received
					sendAbort();
					TimeUnit.SECONDS.sleep(1);
					requestId++;
					continue;
				}
				Node coordinator = electCoordinator();
				System.out.println("Coordinator: " + coordinator.getNodeNumber());
				sendMsgCoordinatorIDToServers(coordinator);

				long currentTime = System.currentTimeMillis();
				boolean isAborted = checkForAbort(currentTime, 15000);
				if (isAborted) {
					System.out.println("Request " + requestId + " Successful");
				}
				else {
					System.out.println("Request " + requestId + " Failed");
				}
				resetQuorumNodes();
				closeClientHandler();
			}
			else {
				readFile(prop);
				TimeUnit.SECONDS.sleep(5);
			}
			TimeUnit.SECONDS.sleep(5);
			requestId++;
		}
	}

	private void readFile(Properties prop)
		throws IOException
	{
		//request to read text file from 1.txt till 7.txt
		int objectToRead = generatedRandomObjectId(prop);
		Node server = createNodesFromServerIds(objectToRead, prop);
		System.out.println("Requesting to read file " + objectToRead + " from server " + server.getHostName());
		if (!clientHandlerMap.containsKey(server)) {
			clientHandlerMap.put(server, new ClientHandler(server));
		}
		clientHandlerMap.get(server).sendMessage(server, "Read " + objectToRead);
	}

	private void closeClientHandler()
	{
		try {
			for (ClientHandler clientHandler : clientHandlerMap.values()) {
				clientHandler.close();
			}
		}
		catch (IOException e) {
			System.out.println("Client Handler closed");
		}
	}

	private void createServerToClientHandler(List<Node> randomServers)
			throws IOException
	{
		for (Node serverNode : randomServers) {
			clientHandlerMap.put(serverNode, new ClientHandler(serverNode));
		}
	}

	private boolean checkForAbort(long currentTime, int timeout) {
		while(Math.abs(System.currentTimeMillis() - currentTime) < timeout && !isAbortRecieved()) {
		}
		return isAbortRecieved();
	}

	private boolean requestQuorum(Properties prop, List<Node> randomServersIdList)
	{
		//TODO: Add while(true) for requesting to 3 servers continuously
		for (Node serverNode : randomServersIdList) {
			clientHandlerMap.get(serverNode).sendMessage(serverNode, "Request " + requestId);
		}
		long currentTime = System.currentTimeMillis();
		while (getQuorum().size() < 3 && Math.abs(System.currentTimeMillis() - currentTime) < 10000) {
		}

		if (getQuorum().size() < quorum) {
			System.out.println("Request failed: Client unable to get Quorum");
			return false;
		}
		System.out.println("Quorum received from : " + getQuorum().size() + " servers");
		return true;
	}

	private Node electCoordinator()
	{
		Random random = new Random();
		int index = random.nextInt(getQuorum().size());
		return getQuorum().get(index);
	}

	private int generatedRandomObjectId(Properties prop)
	{
		int serverCount = Integer.valueOf(prop.getProperty("serverCount"));
		//Code to generate random value between 0 to 6 (0 included, 7 excluded)
		int temp = ThreadLocalRandom.current().nextInt(0, serverCount);
		return temp + 1;
	}

	private List<Node>  getRandomServerList(Properties prop) {
		randomObject = generatedRandomObjectId(prop);

		System.out.println("Random object number: " + randomObject);

		
		//Code to select 3 servers based on random object Hk
		List<Integer> randomServersIdList = new ArrayList<>();
		randomServersIdList.add(randomObject);
		if (randomObject + 1 >= 8) {
			randomServersIdList.add(((randomObject + 1) % 8) + 1);
		}
		else {
			randomServersIdList.add(randomObject + 1);
		}
		if (randomObject + 2 >= 8) {
			randomServersIdList.add(((randomObject + 2) % 8) + 1);
		}
		else {
			randomServersIdList.add(randomObject + 2);
		}

		//TEST CODE
		/*
		List<Integer> randomServersIdList = new ArrayList<>();
		randomServersIdList.add(1);
		randomServersIdList.add(2);
		randomServersIdList.add(3);
		*/

		return  createNodesFromServerIds(randomServersIdList, prop);
	}

	private List<Node> createNodesFromServerIds(List<Integer> randomServersIdList, Properties prop)
	{
		List<Node> servers = new ArrayList<>();
		for (int serverId : randomServersIdList) {
			servers.add(new Node(prop.getProperty("SERVER" + serverId + "_HOST" + proxy),
					Integer.valueOf(prop.getProperty("SERVER" + serverId + "_PORT" + proxy)),
					Integer.valueOf(prop.getProperty("SERVER" + serverId + "_ID"))));
		}
		return servers;
	}

	private Node createNodesFromServerIds( int serverId, Properties prop)
	{
		Node server = new Node(prop.getProperty("SERVER" + serverId + "_HOST" + proxy),
					Integer.valueOf(prop.getProperty("SERVER" + serverId + "_PORT" + proxy)),
					Integer.valueOf(prop.getProperty("SERVER" + serverId + "_ID")));
		return server;
	}

	private void sendAbort() {
		for(Node serverNode : getQuorum()) {
			clientHandlerMap.get(serverNode).sendMessage(serverNode, "Abort");
		}
	}

	private void sendMsgCoordinatorIDToServers(Node coordinator) {
		List<Node> quorumNodes = new ArrayList<>(getQuorum());
		quorumNodes.remove(coordinator);
		// Sending message to coordinator
		StringJoiner cohortNodes = new StringJoiner(",");
		for (Node serverNode: quorumNodes) {
			cohortNodes.add(String.valueOf(serverNode.getNodeNumber()));
		}
		clientHandlerMap.get(coordinator).sendMessage(coordinator, "Coordinator Cohort_ids: " + cohortNodes.toString() + " RandomObject: " + randomObject + " RequestId: " + requestId);
	}

	public static synchronized void updateQuorum(Node node)
	{
		quorumNodes.add(node);
	}

	public List<Node> getQuorum()
	{
		return quorumNodes;
	}

	public static synchronized void resetQuorumNodes()
	{
		quorumNodes = new ArrayList<>();
	}

	public static boolean isAbortRecieved()
	{
		return abortRecieved;
	}

	public static synchronized void setAbortRecieved(boolean value)
	{
		abortRecieved = value;
	}

	/*
	private void sendPrepare() {
		for(int i = 0; i < agreedCohortList.size(); i++) {
			try {
				CohortDetails currentCohort = agreedCohortList.get(i);
				currentCohort.getDos().writeUTF("Prepare");

				//Wait for Acknowledgement for 0.5 Minute
				//connectToServer.setSoTimeout(30000);

				String output = currentCohort.getDis().readUTF();
				if(output != null && output.equals("ACKNOWLEDGED")) {
					AckReceivedCohortList.add(currentCohort);
				}
				else {
					//TODO: check if we need to abort if 3 agreed and only 2 sent ack
					//sendAbort();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void sendCommit() {
		System.out.println("Client: Sending Commit");
		for(int i = 0; i < AckReceivedCohortList.size(); i++) {
			try {
				CohortDetails currentCohort = AckReceivedCohortList.get(i);
				currentCohort.getDos().writeUTF("Commit");

				//if(currentCohort.getDis().readUTF() == null) {
					currentCohort.getDos().writeUTF("Message" + requestId + ": Client Node 1 Requested for Object " + randomObjectOk);
				//}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	*/

	/*
	Unused code

	private String getServerIDString() {
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < agreedCohortList.size() - 1; i++) {
			sb.append(agreedCohortList.get(i).getServerID());
			sb.append(',');
		}
		sb.append(agreedCohortList.get(agreedCohortList.size() - 1).getServerID());
		return sb.toString();
	}

	//Max server Id will be selected as coordinator
	private int getCoordinatorId() {
		int maxServerId = 0;
		for(int i = 0; i < agreedCohortList.size(); i++) {
			if(maxServerId < agreedCohortList.get(i).getServerID()) {
				maxServerId = agreedCohortList.get(i).getServerID();
			}
		}
		return maxServerId;
	}

	 */
}
