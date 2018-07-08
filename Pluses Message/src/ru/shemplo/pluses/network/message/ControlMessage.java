package ru.shemplo.pluses.network.message;

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
