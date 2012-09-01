package communications;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import javax.net.ssl.HostnameVerifier;

import utilities.Tools;

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
		receiver.start();
	}
	
	
	//================================================================================
	// Send 
	//================================================================================
	public void sendData(byte[] dataToBeSent){
//		OutputStream  sout = Tools.createOutputStream(socket);
//		
//		try {
//			sout.write(dataToBeSent);
//		} catch (IOException e){
//			e.printStackTrace();
//		}
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
