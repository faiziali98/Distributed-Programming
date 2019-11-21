/** 
 *  This is the interface used in the communication of
 * the peers. This has following methodes
 * 
 * -> sendMessage: Used to send the message to others
 * 
 * @author: Faizan Ali
*/

import java.rmi.*;
import java.util.ArrayList; 

public interface MessageInterface extends Remote { 
    // Declaring the method prototype 
    public ArrayList<MessageData> buffer = new ArrayList<MessageData>();
    public int k = 0;
    // Message data function to be overridden later
    public MessageData messagePost(MessageData msg) throws RemoteException; 
} 