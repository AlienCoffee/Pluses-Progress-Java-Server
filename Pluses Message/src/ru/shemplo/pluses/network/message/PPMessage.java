package ru.shemplo.pluses.network.message;

public class PPMessage implements Message {
    
    /**
     * 
     */
    private static final long serialVersionUID = -1496297117110071620L;

    public static enum Ping {
        PING, PONG
    }
    
    private final long TIMESTAMP;
    public final Ping VALUE;
    
    public PPMessage (Ping value) {
        this.TIMESTAMP = System.currentTimeMillis ();
        this.VALUE = value;
    }

    @Override
    public long getTimestamp () {
        return TIMESTAMP;
    }
    
}
