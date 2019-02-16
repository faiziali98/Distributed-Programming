// This class is used to save the data of the client
// this is used to save the pings and the ping misses
// as well
//
// by: Faizan Safdar Ali

import java.io.*;
import java.net.*;
import java.util.*;

public class ClientData{
	// Client name
	String name;
	// Clock to see timeout
	private int clock;
	// Socket of the client
	private Socket cSocket;
	// In buffer from the client
	private ObjectInputStream fClient;
	// Output from the client
	private ObjectOutputStream toClient;

   	public ClientData(Socket sok, String _name){
		// This is the constructor of the client data 
		// it takes the socket as the input
		//
		// Initializing the values
		name = _name;
		cSocket = sok;
		clock = 0;
		// Input/Output streams
		try {
			// Very important, first output then input!!!!otherwise deadlock
			toClient = new ObjectOutputStream(cSocket.getOutputStream());
			fClient = new ObjectInputStream(cSocket.getInputStream());
		}catch(IOException e){
			e.printStackTrace();
		}
	}

	public Messages readMessage(){
		// This function is used to receive the message
		// from the socket
		//
		// Read the message from the buffer
		Messages message = new Messages(); 

		try {
			message = (Messages) fClient.readObject();
		}catch(IOException e){
			// Sends message that client left
			System.out.println("Client Disconnected");
			message.setValues(0, "User Left");
			// e.printStackTrace();
		}catch(ClassNotFoundException e){
			e.printStackTrace();
		}

		return message;
	}

	public void writeMessage(Messages m){
		// This function is used to send the message
		// from the socket
		//
		try {
			// Write the message to the buffer
			toClient.writeObject((Object)m);
			
			// Flush the socket
			toClient.flush();
			// Reset the pipe to solve reference problem
			toClient.reset();
			
		} catch(IOException e){
			// e.printStackTrace();
			return;
		} 
	}

	public void setClock(int i){
		// used to increament/reset the clock value
		//
		// i = 1 means increament, i = 0 means reset
		if (i == 1){
			clock += 1;
		}else{
			clock = 0;
		}
	}
}