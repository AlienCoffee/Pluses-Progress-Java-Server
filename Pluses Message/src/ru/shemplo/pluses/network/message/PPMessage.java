package ru.shemplo.pluses.network.message;

import org.json.JSONObject;

public class PPMessage implements Message {
    
    /**
     * 
     */
    private static final long serialVersionUID = -1496297117110071620L;

    public static enum Ping {
        PING, PONG, BUY
    }
    
    private final long TIMESTAMP;
    public final Ping VALUE;
    public final int ID;
    
    public PPMessage (Ping value) {
        this.TIMESTAMP = System.currentTimeMillis ();
        this.ID = RANDOM.nextInt ();
        this.VALUE = value;
    }
    
    @Override
    public JSONObject toJSON (JSONObject root) {
        root.put ("timestamp", TIMESTAMP);
        root.put ("type", "ppmessage");
        root.put ("value", VALUE);
        root.put ("id", ID);
        return root;
    }

    @Override
    public long getTimestamp () {
        return TIMESTAMP;
    }

    @Override
    public int getID () {
        return ID;
    }
    
}
