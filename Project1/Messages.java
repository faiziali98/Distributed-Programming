// This class is used to send the custom messages
// including the timestamp and the broadcast
// text message
//
// by: Faizan Safdar Ali

import java.util.*;
import java.io.Serializable;

public class Messages implements Serializable{
    // Time stamp
    private int timestamp;
    // Message
    private String message;

    public Messages(){
        // Empty Constructor
        //
        // Initialize to zero
        timestamp = 0;
        message = "";
    }

    public Messages(int ts, String msg){
        // Duplicate Constructor
        //
        // Initialize to input values
        timestamp = ts;
        message = msg;
    }

    public void setValues(int ts, String msg){
        // Updates values
        timestamp = ts;
        message = msg;
    }

    public String getMessage(){
        // Returnes message
        return message;
    }

    public int getTs(){
        // Returns Timestamp
        return timestamp;
    }
}