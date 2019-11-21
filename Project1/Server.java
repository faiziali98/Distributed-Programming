// The purpose of the server is to accept 
// new users, then set up the system such that
// each message from the client is broadcasted
// to the other users.
//
// The server is also responsible to see if
// any of the client has left or is disconnected
//
//
// Created by: Faizan Safdar Ali


import java.io.*;
import java.net.*;
import java.util.*;

// Make a server class to listen to the 
// incoming clients

public class Server {

    // We are going to use multithreading, so following
    // variables are global
    //
    // port at which server is listening
	int port;
    // Used to see if the user is still avaiable
	int magic;
    // Ip address of the server
	String ip;
    // Timestamp
    int timestamp;
    // TCP server socket
	ServerSocket socket;
    // Array of clients
	ArrayList<ClientData> clients;
    // Array of names
    ArrayList<String> names;
    // Array of messages
    Queue queue = new LinkedList();

    // Constructor
	public Server(){
        // Initializing the values
        port = 4444;
        magic = 15440;
        ip = "127.0.0.1";
        clients = new ArrayList<ClientData>();
        names = new ArrayList<String>();

        // Initializing time stamp between 0-100
        Random rand = new Random();
        timestamp = rand.nextInt(100);
	}
    
    private void broadCaster(){
        // This is the broadcaster, it is always waiting for
        // new message to post
        //
        // Lambda thread
        new Thread(()->{
            while (true){
                // Get the queue size and see if it has value
                if (queue.size()>0){
                    // Get value from the queue
                    String msg = add_get("", 2);
                    for(int j=0 ;j<clients.size();j++){
                        // Send message to all the clients
                        setTs(timestamp);
                        Messages tosend = new Messages(timestamp, msg);
                        ClientData c = clients.get(j);
                        c.writeMessage(tosend);
                    }
                }

                // Thread sleep might send Interrupt expection
                try {
			    	Thread.sleep(251);
				} catch(InterruptedException ex) {
			    	Thread.currentThread().interrupt();
				}
            }
        }).start();
    }

    private synchronized String add_get(String message, int action){
        // Add or remove string from the queue in synchronized manner
        //
        // action 1 means adding new message, 2 means removing
        if (action == 1){
            // Adding to queue
            queue.add(message);
        }else if(action == 2){
            // Removing from queue
            return (String)queue.remove();
        }
        return "Done";
    }

    private synchronized void setTs(int ts){
        // Used to increament the timestamp or update with 
        // other time stamps
        //
        if (ts>timestamp){
            timestamp = ts + 1;
        }else{
            timestamp += 1;
        }
        
        System.out.println("Current timestamp: "+ timestamp);
    }

    private synchronized void addClient(ClientData c, String name){
        // Add or remove string from the queue in synchronized manner
        clients.add(c);
        names.add(name);
    }

    public void ClientHandler(Socket cSocket){
        new Thread(()->{
            // Create new client
            ClientData c = new ClientData (cSocket, "");
            Messages message = c.readMessage();
            setTs(message.getTs());

            // Check if the name is avaiable
            if (names.contains(message.getMessage())){
                // Rejects if not
                setTs(timestamp);
                message.setValues(timestamp, "reject");
                c.writeMessage(message);
            }else{
                // If name is avaiable, initialize the connection
                c.name = message.getMessage();
                addClient (c, c.name);
                // Send accept to the client
                setTs(timestamp);
                Messages accept = new Messages(timestamp, "accept");
                c.writeMessage(accept);

                add_get("From Server: New user " + c.name + " is added", 1);
                while (true){
                    // Read message from the client
                    message = c.readMessage();
                    String msg = message.getMessage();
                    setTs(message.getTs());

                    if (msg.equals("User Left") || msg.equals(c.name+" quit")){
                        // Sends the goodbye message, timestamp does not matter
                        message.setValues(timestamp, "Bye");
                        // Now writes the message oblject
                        c.writeMessage(message);
                        // Remove client and its taken name
                        clients.remove(c);
                        names.remove(c.name);
                        // Add message tp braodcast that user has left
                        add_get("User " + c.name + " left the room", 1);
                        // Breaks the loop
                        break;
                    }

                    // Add received message to list to broadcast to
                    // other users
                    //
                    add_get(c.name + ": " + msg, 1);
                }
            }
        }).start();
    }

    public void listner(){
        // Listens to the new connection at port 4444, 
        // we need to update the Ip once we go global!!
        //
		try {
            // Port of the server
			socket = new ServerSocket(port);
			System.out.println("Server Started");
            broadCaster();
            // Listening for the client
            while(true){
                // Connects the new client
                Socket cSocket = socket.accept();
                System.out.println("Client Connected");
                // Handle in new thread
                ClientHandler(cSocket);
            }
        }catch(IOException e){
				e.printStackTrace();
		}
	}	

    public static void main(String[] args) {
		Server s = new Server();

        try {
            // s.PingClient();
            s.listner();
		} catch(Exception ex) {
			Thread.currentThread().interrupt();
		}
	}
}