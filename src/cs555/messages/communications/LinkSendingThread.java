package cs555.messages.communications;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import cs555.messages.utilities.*;

// Thread to send data
public class LinkSendingThread extends Thread{

	byte[] data;
	Socket socket;
	
	//================================================================================
	// Constructor
	//================================================================================
	public LinkSendingThread(Socket s, byte[] d){
		socket = s;
		data = d;
	}
	
	//================================================================================
	// Run
	//================================================================================
	public void run(){
		OutputStream sout = Tools.createOutputStream(socket);
		
		try {
			sout.write(data);
		} catch (IOException e){
			Tools.printStackTrace(e);
		}
	}
}
