/**
 * This is the client code. Every client will join the
 * cluster and create a table for all of the other
 * clients. Each client can send/receive the messages
 * This also opens a registery for other clients so that they
 * can send the messages.
 * 
 * @author: Faizan Ali
 * @since: 2013
 */


import java.rmi.*;
import java.io.*;
import java.util.*;
import java.rmi.registry.*;
import java.rmi.server.*;

// P2P client class
public class P2pclient extends UnicastRemoteObject implements MessageInterface {

    // This class is used to keep the rmi of the connecting clients
    private class OtherData {
        public MessageInterface rmi;
        public int port;
        public String ip;
        
        /**
         * @param rmi: The Remtote methode handler of the client
         * @param port: The port of the client
         * @param ip: The Ip of the client
         */
        public OtherData(MessageInterface rmi, int port, String ip) {
            this.rmi = rmi;
            this.port = port;
            this.ip = ip;
        }
    }

    /** Global Variables
     *  Most of them are global because they are shared
     * They can be used locally as well
     * The port the loca server listening to
     */ 
    private int thisPort;
    // The ip address of the server
    private String ipaddr = "127.0.0.1"; 
    // The registry on which the methode will be posted
    private Registry registry;
    // Keep the count of other clients in the system
    ArrayList<OtherData> clients = new ArrayList<OtherData>();
    // Local vector clock
    public static int VCLOCK[] = { 0, 0 };
    // Index of the current client in the vector clock
    public static int MYINDEX;
    // The buffer to store the messages
    public static ArrayList<MessageData> buffer 
                            = new ArrayList<MessageData>();
    // Total number of clients in the system
    int tclients = 0;
    
    public P2pclient() throws RemoteException {
        super();
    }


    /** 
     * It has its own VCLOCK because P2pclient it is the object binded to the
     *  register and contains VCLOCK. That is why I was not getting any
     * errors. Now this error is solved by changing the VCLOCK of the
     * P2pclient. Yes, we can do that :P. Java hacks ;)
     * 
     * @param text: The text posted by te other client
     */ 

    @Override
    public MessageData messagePost(MessageData text) throws RemoteException {
        // This is the remote post message function used to
        // receive the messages from the clients in the form 
        // of abstract class object MessageData
        //
        // Extract the message
        String msg = text.getData();
        // If the message is not connection request
        if (!msg.equals("L3tConn3ct")) {
            // Put the message in the buffer
            insertMsg(text);
        } else {
            // To see if the client is connected
            System.out.println("Client Connected\n");
        }
        // Send the accept receipt to every message
        return (new MessageData("accept", Integer.toString(thisPort)));
    }

    /**
     * Synchronously inserts the message in the buffer
     * sysnchronized as many clients can send the data
     *  at a same time.
     * 
     * @param msg: Message to be inserted
     */
    public synchronized void insertMsg(MessageData msg){
        this.buffer.add(msg);
    } 

    /**
     *  This check if the message is causally ordered
     *  by comparing the vector clocks according to the 
     *  rules. Return 1 if message passes.
     * 
     * @param msg: Messge to be checked
    */
    public int checkDelivery(MessageData msg){
        int []mvc = msg.VCLOCK;
        // Means that r is the next msg, k was expecting from j
        if (mvc[msg.sender_index] == VCLOCK[msg.sender_index] + 1){
            // Means Pk has not seen any msgs that were not seen by 
            // j when it sends msg r Pk has seen msg a.
            for (int x = 0; x < tclients; x++){
                if (x != msg.sender_index){
                    if (mvc[x] > VCLOCK[x]){
                        return 0;
                    }
                }
            }
        }else{
            return 0;
        }
        // Update the clock if the message passes
        update_vclock(msg.VCLOCK, 0);
        // Print the message
        System.out.println("\n" + msg.getID() + " Sent: " + msg.getData()
                                    + " " + Arrays.toString(VCLOCK));
        System.out.print("Enter Message: ");
        return 1;
    }

    // Runs in the seperate thread to see if there is any message
    // in the buffer to display
    public void displayer(){
        new Thread(() -> {
            while (true){
                for (int x = 0; x<buffer.size(); x++){
                    if (checkDelivery(buffer.get(x)) == 1)
                        buffer.remove(x);
                }
                // Used to have a little break, so no
                // thread starves.
                try {
                    Thread.sleep(200);
                } catch (Exception e) {
                    System.out.println(e);
                }
            }
        }).start();
    }

    /**
     * The function used to connect to the other client
     * 
     * @param ipad: Ip address of the other client
     * @param port: Port of the other client
     */ 
    public Thread connectToClient(String ipad, int port) {
        return (new Thread(() -> {
            // Generation of the objects in the loop
            // decreases the performance
            Registry registry;
            MessageInterface rmi;
            String id = Integer.toString(thisPort);
            MessageData toSend = new MessageData("L3tConn3ct", id);
            toSend.sender_index = MYINDEX;
            toSend.VCLOCK = VCLOCK;
            MessageData text;

            while (true) {
                // Catch the exception which RMI can send
                try {
                    // Finds the registry
                    registry = LocateRegistry.getRegistry(ipad, port);
                    // Look for the serever entry
                    rmi = (MessageInterface) registry.lookup("server");
                    System.out.println("\nConnected to a Client");
                    // Invoke the function using stub and RMI call
                    text = rmi.messagePost(toSend);
                    String msg = text.getData();
                    // Break when connects
                    if (msg.equals("accept"))
                        break;
                } catch (Exception e) {
                    System.out.println("Client not avaiable");
                }
                // Used to have a little break, so no
                // thread starves.
                try {
                    Thread.sleep(200);
                } catch (Exception e) {
                    System.out.println(e);
                }
            }

            // Add the client to the list
            addClientList(new OtherData(rmi, port, ipad));

        }));
    }

    /**
     * Synchronous addition of the client
     * 
     * @param data: client to be added 
     */ 
    private synchronized void addClientList(OtherData data) {
        clients.add(data);
    }

    /** 
     * Sysnchronous update of the vector clock
     * 
     * @param vc: New vector clock
     * @param s: Is this update for send or not?
    */
    private synchronized void update_vclock(int vc[], int s) {
        if (s == 1) {
            // If sending just add 1
            VCLOCK[MYINDEX] = VCLOCK[MYINDEX] + 1;
        } else {
            // If receive then update according to the
            // new vector clock
            for (int i = 0; i < vc.length; i++) {
                if (this.VCLOCK[i] < vc[i]) {
                    this.VCLOCK[i] = vc[i];
                }
            }
        }
    }

    // This function wait for the user input and then
    // send the message to everyone
    public void Texting() {
        // Open the scanner
        Scanner reader = new Scanner(System.in);
        // Create the message
        MessageData toSend = new MessageData("", ipaddr + "/" 
                                    + Integer.toString(thisPort));
        toSend.sender_index = MYINDEX;
        String msg;

        while (true) {
            try {
                System.out.print("Enter Message: ");
                msg = reader.nextLine();

                // If user type quit, exit
                if (msg.equals("quit"))
                    break;

                // Update the message body
                toSend.setData(msg);
                // Update VCLOCK
                update_vclock(this.VCLOCK, 1);
                toSend.VCLOCK = VCLOCK;

                for (int j = 0; j < clients.size(); j++) {
                    // Send message to all the clients
                    OtherData c = clients.get(j);
                    c.rmi.messagePost(toSend);
                }

            } catch (Exception e) {
                // If client leaves, will get the error
                System.out.println("Client left");
                break;
            }
        }

        reader.close();

    }

    /** 
     * This function hears to the client
     * 
     * @param port: Port to hear to
     */ 
    public void hearToClient(int port) throws Exception {
        thisPort = port;
        // Open the registry on the local ip on the
        // specified port
        // System.setProperty("java.rmi.server.hostname", "0.0.0.0");

        registry = LocateRegistry.createRegistry(port);
        // Rebind to tell everyone for the registry
        registry.rebind("server", this);
        System.out.println("Server started!");
    }

    // This function opens the file of the clients and then
    // create threads to connect to the clients
    public void connectToEveryone() throws Exception {
        // Open file
        FileInputStream in = new FileInputStream("ports.txt");
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        // Create thread list
        ArrayList<Thread> threads = new ArrayList<Thread>();

        for (String line; (line = br.readLine()) != null;) {
            // Read the line
            int p = Integer.parseInt(line);
            if (p != thisPort) {
                // Send the ip and port to connect to
                Thread t = connectToClient("85.101.110.2", p);
                threads.add(t);
                t.start();
            } else {
                // Set the index value
                this.MYINDEX = tclients;
            }
            tclients++;
        }

        // Wait for threads to be done with connecting to
        // other clients
        for (Thread thread : threads) {
            thread.join();
        }

        // Start texting and displaying the messages
        br.close();
        this.displayer();
        this.Texting();
    }

    // Main function to start the client
    public static void main(String[] args) {
        // Input the port to connect to
        Scanner reader = new Scanner(System.in);
        System.out.print("Enter your port: ");
        int n = reader.nextInt();

        try {
            // Create the client object
            P2pclient client = new P2pclient();
            // Start hearing to the port
            client.hearToClient(n);
            // Start connecting to everyone and start
            // messaging!!!!!!!!!!!!!!!!!!!!!!
            client.connectToEveryone();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}