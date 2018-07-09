package ru.shemplo.pluses.network.message;

import org.json.JSONObject;

public class CommandMessage extends AbsAppMessage {

    /**
     * 
     */
    private static final long serialVersionUID = 8074646972538672771L;

    protected final String COMMAND;
    
    public CommandMessage (Message reply, MessageDirection direction, String command) {
        super (reply, direction);
        this.COMMAND = command;
    }
    
    public CommandMessage (MessageDirection direction, String command) {
        this (null, direction, command);
    }
    
    @Override
    public JSONObject toJSON (JSONObject root) {
        JSONObject tmp = super.toJSON (root);
        tmp.put ("type", "commandmessage");
        tmp.put ("command", getCommand ());
        return tmp;
    }
    
    public String getCommand () {
        return COMMAND;
    }
    
}
