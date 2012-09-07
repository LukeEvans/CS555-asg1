package cs555.messages.node;

import java.io.IOException;
import java.net.Socket;
import java.util.Vector;


import cs555.messages.communications.*;
import cs555.messages.utilities.*;
import cs555.messages.wireformats.*;

// Main process in the system
public class Node {

	ServerSockThread server;

	// Configuration 
	PeerList peerList;
	int serverPort;
	int messagePerRound;

	// Message counters
	int sendTracker;
	int sendSummation;

	// Message sums
	int receiveTracker;
	int receiveSummation;

	// Vector to hold
	Vector<Payload> messageList;

	Boolean sendLock;
	Boolean recLock;

	// Used for the collection server
	int totalSent;
	int totalSumSent;
	int totalReceived;
	int totalSumReceived;
	int finishCount;

	//================================================================================
	// Constructor Process
	//================================================================================
	public Node(PeerList list, int port, int messPerRound){
		peerList = list;
		serverPort = port;
		messagePerRound = messPerRound;
		server = new ServerSockThread(serverPort, this);

		sendTracker = 0;
		sendSummation = 0;

		receiveTracker = 0;
		receiveSummation = 0;

		messageList = new Vector<Payload>();

		sendLock = new Boolean(true);
		recLock = new Boolean(true);

		totalSent = 0;
		totalSumSent = 0;
		totalReceived = 0;
		totalSumReceived = 0;
		finishCount = 0;
	}

	public void initServer(){
		server.start();
	}


	//================================================================================
	// Round
	//================================================================================
	// Start round
	public void beginRound(){

		// Get new peer to talk to
		Peer peer = peerList.getNextPeer();
		if (peer == null) return;


		// Abstract the link from the peer
		Link link = connect(peer);
		if (link == null) return;

		//System.out.println("Sending to: " + peer.hostname + " " + link.remoteHost);

		for (int i=0; i<messagePerRound; i++){
			// Send data
			int randomNumber = Tools.generateRandomNumber();
			sendPayload(link, randomNumber);

			int reply = link.waitForIntReply();

			if (reply != randomNumber){
				System.out.println("Verification did not match.");
				System.exit(1);
			}

			// We have heard back with the correct number
			trackSend(randomNumber);
		}

		// Close link
		link.close();

	}


	// Connect to peer
	public Link connect(Peer p){
		Socket sock;
		Link link = null;

		try {
			sock = new Socket(p.hostname,p.port);
			link = new Link(sock, this);
		} catch (IOException e){
			System.out.println("Could not connect to: " + p.hostname + ", " + p.port);
			Tools.printStackTrace(e);
		}

		return link;
	}

	//================================================================================
	// Send
	//================================================================================
	// Send data
	public void sendPayload(Link link, int number){

		Payload payload = new Payload(number);
		link.sendData(payload.marshall());

	}

	// Thread safe increment send tracker and summation
	public synchronized void trackSend(int n){
		sendTracker++;
		sendSummation += n;

	}

	// Send our results to everyone in the system
	public void broadcastResults(){
		for (Peer p : peerList.getAllPeers()){

			publishResults(p);
		}
	}

	// Publish results to cumulation server
	public void publishResults(Peer peer){
		// publish Results to peer
		Link link = connect(peer);

		NodeResults results = new NodeResults(sendTracker, receiveTracker, sendSummation, receiveSummation);
		link.sendData(results.marshall());
		byte[] replyData = link.waitForData();

		NodeResults reply = new NodeResults();
		reply.unmarshall(replyData);
		
		if (!reply.equals(results)){
			System.out.println("Results verification did not match.");
			System.exit(1);
		}

		// Close the link
		link.close();

	}

	//================================================================================
	// Receive
	//================================================================================
	// Receieve data
	public synchronized void receive(byte[] bytes, Link l){

		int messageType = Tools.getMessageType(bytes);

		switch (messageType) {
		case Constants.Payload:

			Payload payload = new Payload();
			payload.unmarshall(bytes);

			trackReceive(payload.number);

			// Send verification
			Verification ack = new Verification(payload.number);
			l.sendData(ack.marshall());

			break;

		case Constants.Node_Results:
			NodeResults results = new NodeResults();
			results.unmarshall(bytes);

			// Save all of the node's information
			int numSent = results.numberSent;
			int numRec = results.numberReceived;
			int sumSent = results.sumSent;
			int sumRec = results.sumReceived;

			finishCount++;
			totalSent += numSent;
			totalReceived += numRec;
			totalSumSent += sumSent;
			totalSumReceived += sumRec;

			NodeResults resultsAck = new NodeResults(numSent, numRec, sumSent, sumRec);
			l.sendData(resultsAck.marshall());

			// If we've heard back from everybody, print
			if (finishCount == peerList.size()){
				totalSent += sendTracker;
				totalReceived += receiveTracker;
				totalSumSent += sendSummation;
				totalSumReceived += receiveSummation;
				printCumulativeOutput();

				// Cleanup
				l.close();
			}

			break;

		default:
			System.out.println("Received unrecognized message: " + messageType);
			break;
		}
	}

	// Thread safe increment receive tracker and summation
	public synchronized void trackReceive(int n){

		receiveTracker++;
		receiveSummation += n;

	}

	//================================================================================
	// Output
	//================================================================================
	// Print output
	public void printOutput() {

		System.out.println("\nNumber Sent: " + sendTracker);
		System.out.println("Number Received: " + receiveTracker);

		System.out.println("Difference Number: " + (sendTracker - receiveTracker));

		System.out.println("\nSend sum: " + sendSummation);
		System.out.println("Receive sum: " + receiveSummation);

		System.out.println("Difference Summation: " + (sendSummation - receiveSummation));
	}

	// Prints the results seen by all servers in the system
	public void printCumulativeOutput(){

		// Print our data
		printOutput();

		// Print all data
		System.out.println("\n\nAll nodes finished:");
		System.out.println("Total messages sent: " + totalSent);
		System.out.println("Total messages received: " + totalReceived);
		System.out.println("Total sum sent: " + totalSumSent);
		System.out.println("Total sum received:   " + totalSumReceived +"\n");	
	}

	//================================================================================
	// Cleanup
	//================================================================================
	// Close the server
	public void cleanup(){
		server.cont = false;
		System.exit(0);
	}

	//================================================================================
	//================================================================================
	// Main
	//================================================================================
	//================================================================================
	public static void main(String[] args){

		int numberOfRounds = 5000;
		int messagesPerRound = 5;
		int port = 0;
		String filename = "";

		if (args.length == 2) {
			port = Integer.parseInt(args[0]);
			filename = args[1];
		}

		else if (args.length == 4){
			port = Integer.parseInt(args[0]);
			filename = args[1];
			numberOfRounds = Integer.parseInt(args[2]);
			messagesPerRound = Integer.parseInt(args[3]);
		}

		else {
			System.out.println("Usage: java node PORT CONFIG-FILE <ROUNDS> <MESSAGES-PER-ROUND>");
			System.exit(1);
		}

		// Create peer list
		PeerList peerList = new PeerList(filename, port);

		// Create node
		Node node = new  Node(peerList, port, messagesPerRound);
		node.initServer();

		System.out.println("Waiting for other nodes to join the system...");

		// Sleep to give time for others to join
		Tools.sleep(5);

		System.out.println("Beginning " + numberOfRounds + " rounds...");

		// For each round begin round
		for (int i=0; i<numberOfRounds; i++){
			node.beginRound();
		}

		// Wait for stragglers
		Tools.sleep(3);

		// Send results to all nodes
		node.broadcastResults();

		// Give threads time to exit
		Tools.sleep(5);
		node.cleanup();
	}
}
