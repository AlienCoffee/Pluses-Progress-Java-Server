package ru.shemplo.pluses.network.message;

import org.json.JSONObject;

public class ControlMessage extends AbsAppMessage {

    /**
     * 
     */
    private static final long serialVersionUID = -2101506025334294654L;

    public static enum ControlType {
        ERROR, ID, INFO, JSON
    }
    
    protected final ControlType TYPE;
    protected final String COMMENT;
    protected final int CODE;
    
    public ControlMessage (Message reply, MessageDirection direction, 
            ControlType type, int code, String comment) {
        super (reply, direction);
        this.COMMENT = comment;
        this.TYPE = type;
        this.CODE = code;
    }
    
    public ControlMessage (MessageDirection direction, ControlType type, 
            int code, String comment) {
        this (null, direction, type, code, comment);
    }
    
    @Override
    public JSONObject toJSON (JSONObject root) {
        JSONObject tmp = super.toJSON (root);
        tmp.put ("type", "controlmessage");
        tmp.put ("comment", getComment ());
        tmp.put ("kind", getType ());
        tmp.put ("code", getCode ());
        return tmp;
    }
    
    public ControlType getType () {
        return TYPE;
    }
    
    public int getCode () {
        return CODE;
    }
    
    public String getComment () {
        return COMMENT;
    }
    
}
