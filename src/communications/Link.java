package communications;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

import utilities.Tools;
import wireformats.Constants;
import wireformats.Payload;
import wireformats.Verification;


import node.Node;

// Link is a class to abstract a connection between nodes
public class Link {

	String remoteHost;
	Socket socket;
	Node node;
	LinkReceiverThread receiver;

	//================================================================================
	// Constructor
	//================================================================================
	public Link(Socket s, Node n) {
		socket = s;
		node = n;
		remoteHost = socket.getInetAddress().getHostName();
		receiver = new LinkReceiverThread(socket, this);
	}

	public void initLink(){
		System.out.println("Init link");
		receiver.start();
	}


	//================================================================================
	// Send 
	//================================================================================
	public void sendData(byte[] dataToBeSent){
		LinkSendingThread sender = new LinkSendingThread(socket, dataToBeSent);
		sender.start();
	}


	//================================================================================
	// Receive
	//================================================================================
	public void dataReceived(int bytes, byte[] dataReceived){
		node.receive(dataReceived,this);
	}


	//================================================================================
	// House Keeping
	//================================================================================
	public void close() {
		receiver.cont = false;

		try {
			socket.close();
		} catch (IOException e){
			System.out.println("Could not close socket");
		}
	}
}
