package node;

import java.io.IOException;
import java.net.Socket;
import java.util.Vector;

import utilities.Tools;
import wireformats.Constants;
import wireformats.Payload;
import wireformats.SumDifference;
import wireformats.TotalDifference;
import wireformats.Verification;
import communications.Link;
import communications.ServerSockThread;

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
	int countTotal;
	int sumTotal;
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
		
		countTotal = 0;
		sumTotal = 0;
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

			int reply = link.waitForData();

			//System.out.println("Sent: " + randomNumber + " Received: " + reply);

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
			e.printStackTrace();
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

		// Increment data
		//trackSend(number);
	}

	// Thread safe increment send tracker and summation
	public synchronized void trackSend(int n){
		sendTracker++;
		sendSummation += n;

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

			//System.out.println("Received Payload");
			//System.out.println(payload);

			trackReceive(payload.number);

			// Send verification
			Verification ack = new Verification(payload.number);
			l.sendData(ack.marshall());

			break;

		case Constants.Verification:

			Verification verification = new Verification();
			verification.unmarshall(bytes);

			System.out.println("Received verification");
			System.exit(1);

			break;

		case Constants.Total_Difference:
			TotalDifference total = new TotalDifference();
			total.unmarshall(bytes);
			
			//System.out.println(l.remoteHost + ", " + total.number);
			
			countTotal += total.number;
			
			break;
			
		case Constants.Sum_Difference:
			SumDifference sum = new SumDifference();
			sum.unmarshall(bytes);
			
			sumTotal += sum.number;
			finishCount++;
			
			// If we've heard back from everybody, print
			if (finishCount == peerList.size()){
				System.out.println("\n\nFinished:");
				System.out.println("Total Difference: " + countTotal);
				System.out.println("Sum Difference:   " + sumTotal);
			}
			break;
			
		default:

			System.out.println("Received unrecognized message");
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

		// Connect to the cumulation server to print totals
		Peer collection = new Peer("bean", 5555);
		Link link = connect(collection);
		
		TotalDifference total = new TotalDifference((sendTracker - receiveTracker));
		link.sendData(total.marshall());
		
		SumDifference sumTotal = new SumDifference((sendSummation - receiveSummation));
		link.sendData(sumTotal.marshall());
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
		Tools.sleep(10);

		System.out.println("Beginning " + numberOfRounds + " rounds...");
		
		// For each round begin round
		for (int i=0; i<numberOfRounds; i++){
			node.beginRound();
		}

		// Wait for stragglers
		Tools.sleep(3);

		node.printOutput();
	}
}
