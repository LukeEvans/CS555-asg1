package utilities;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

// Functions used all over the program. Handy location
public class Tools {

	// ================================================================================
	// Message functions
	// ================================================================================
	// Get Message type
	public static int getMessageType(byte[] bytes) {
		byte[] copy = bytes.clone();
		ByteBuffer bbuff = ByteBuffer.wrap(copy);

		// Size
		bbuff.getInt();

		// Return type
		return bbuff.getInt();
	}

	// Generate random number
	public static int generateRandomNumber() {

		int Min = 0;
		int Max = 65535;

		int random = Min + (int) (Math.random() * ((Max - Min) + 1));

		random -= 32768;

		return random;
	}

	
	//================================================================================
	// Link Functions 
	//================================================================================
	// Create input stream
	public static InputStream createInput(Socket s){
		InputStream sin;
		
		try {
			sin = s.getInputStream();
			return sin;
		} catch (IOException e){
			e.printStackTrace();
			return null;
		}
	}
	
	// Create output stream
	public static OutputStream createOutputStream(Socket s){
		OutputStream sout;
		
		try {
			sout = s.getOutputStream();
			return sout;
		} catch (IOException e){
			e.printStackTrace();
			return null;
		}
	}
	
	
	// ================================================================================
	// Host Functions
	// ================================================================================
	public static String getLocalHostname() {
		try {
			String localhostname = java.net.InetAddress.getLocalHost().getHostName();
			return localhostname;
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	public static void sleep(int time){
		try {
			Thread.sleep(time * 1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	//================================================================================
	// Byte Manipulations
	//================================================================================
	// convert string to byte array
	public static byte[] convertToBytes(String s){
		return s.getBytes();
	}
	
	// Convert Int to byte array
	public static byte[] convertToBytes(int i){
		return convertToBytes(Integer.toString(i));
	}
	
}
