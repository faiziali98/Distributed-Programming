// This is the client part of the project client is listening 
// for the messages and is ready to send the messages as well.
// Client choose the name and if that name is already taken, 
// it is refused connection from the server
// 
//
// by: Faizan Safdar Ali

import java.io.*;
import java.net.*;
import java.util.Random;

class Client {

    // Local time stamp
    int timestamp;
    // Signals if the server has died
    int closed;

    public Client(){
        // Initialize the values
        Random rand = new Random();
        closed = 0;
        timestamp = rand.nextInt(100);
    }
    // This function is listening to the new messages
    public void listen_messages(ClientData c){
        // Lambda thread
        new Thread(()->{
            while (true){
                // Reads the message object
                Messages message = c.readMessage();
                // Extract the message
                String msg = message.getMessage();
                // Extract the timestamp
                setTs(message.getTs());

                // Stops the thread if server dies or client leaves the
                //  message room (needs improvement, signal should be
                // used within the program to stop the thread)
                if (msg.equals("Bye") || msg.equals("User Left")){
                    closed = 1;
                    break;
                }
                // Prints the msg
                System.out.println(msg);
            }
        }).start();
    }

    public synchronized void setTs(int ts){
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

    public static void main(String argv[]) throws Exception {
        // Variables for the server connection
        String ip = "127.0.0.1";
        Messages toSend = new Messages();
        Client cl = new Client();
        
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));

        //Select User Name
        System.out.print("Input User Name: ");
        String name = inFromUser.readLine();

        // Initialize the connection and add the user
        Socket clientSocket = new Socket(InetAddress.getByName(ip), 4444);
        ClientData c = new ClientData(clientSocket, name);

        // Send the name to the server
        cl.setTs(cl.timestamp);
        toSend.setValues(cl.timestamp, name);
        c.writeMessage(toSend);
        // Wait for the response
        Messages response = c.readMessage();
        cl.setTs(response.getTs());

        // Accept or reject from server
        if (response.getMessage().equals("accept")){
            // Start the listener
            cl.listen_messages(c);
            // Start sending messages
            while (true){
                // Increament the timestamp
                cl.setTs(cl.timestamp);
                // Read the message to send
                toSend.setValues(cl.timestamp, inFromUser.readLine());
                c.writeMessage(toSend);

                // Stops if the user quits or server stops
                if (toSend.getMessage().equals(c.name+" quit")){
                    break;
                } else if (cl.closed == 1) {
                    System.out.println("Server Closed");
                    break;
                }
            }
        }else{
            // Ask to connect again
            System.out.println("Your username has already been taken, enter a new one");
            // Close the connection
            clientSocket.close();
        }
    }
}