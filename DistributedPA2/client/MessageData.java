/** This is the data class used for the communication
 * between the clients
 * 
 * @author: Faizan Ali
*/ 

import java.io.*;

public class MessageData implements Serializable {
    private String data;
    private String id;
    public int sender_index;
    public int VCLOCK[];
    // public static int k = 0;

    // Empty constructor
    public MessageData() {
        data = "";
        id = "";
    }

    // Constructor with input
    public MessageData(String data, String id) {
        this.data = data;
        this.id = id;
    }

    // To get data
    public String getData() {
        return data;
    }

    // To get Id
    public String getID() {
        return id;
    }

    // To set data
    public void setData(String data) {
        this.data = data;
    }

    // TO set ID
    public void setId(String Id) {
        this.id = Id;
    }
}