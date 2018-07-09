package ru.shemplo.pluses.network.message;

import java.util.Objects;

import org.json.JSONObject;

public abstract class AbsAppMessage implements AppMessage {

    /**
     * 
     */
    private static final long serialVersionUID = -2736659415758637345L;
    
    protected final MessageDirection DIRECTION;
    protected final long TIMESTAMP;
    protected final Message REPLY;
    
    public final int ID;
    
    public AbsAppMessage (Message reply, MessageDirection direction) {
        this.TIMESTAMP = System.currentTimeMillis ();
        this.ID = RANDOM.nextInt ();
        this.DIRECTION = direction;
        this.REPLY = reply;
    }
    
    public AbsAppMessage (MessageDirection direction) {
        this (null, direction);
    }
    
    @Override
    public JSONObject toJSON (JSONObject root) {
        root.put ("timestamp", getTimestamp ());
        root.put ("direction", getDirection ());
        root.put ("id", getID ());
        
        if (!Objects.isNull (REPLY)) {
            JSONObject tmp = new JSONObject ();
            root.put ("reply", 
                REPLY.toJSON (tmp).toString ());
        }
        
        return root;
    }
    
    @Override
    public long getTimestamp () {
        return TIMESTAMP;
    }
    
    @Override
    public MessageDirection getDirection () {
        return DIRECTION;
    }
    
    @Override
    public Message getReplyMessage () {
        return REPLY;
    }
    
    @Override
    public int getID () {
        return ID;
    }
    
}
