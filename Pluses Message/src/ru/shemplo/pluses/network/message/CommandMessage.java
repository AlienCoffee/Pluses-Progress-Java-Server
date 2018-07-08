package ru.shemplo.pluses.network.message;

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
    
    public String getCommand () {
        return COMMAND;
    }
    
}
