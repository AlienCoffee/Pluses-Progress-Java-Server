package ru.shemplo.pluses.network.message;

public class JavaAppError implements AppMessage {

    /**
     * 
     */
    private static final long serialVersionUID = -7511146492305268573L;

    private final MessageDirection DIRECTION;
    private final long TIMESTAMP;
    private final String ERROR;
    
    public JavaAppError (MessageDirection direction, String error) {
        this.TIMESTAMP = System.currentTimeMillis ();
        this.DIRECTION = direction;
        this.ERROR = error;
    }
    
    public JavaAppError (MessageDirection direction, Exception exception) {
        this (direction, "Exception occured (" + exception + ")");
    }
    
    @Override
    public MessageDirection getDirection () {
        return DIRECTION;
    }

    @Override
    public String getContent () {
        return ERROR;
    }
    
    @Override
    public long getTimestamp () {
        return TIMESTAMP;
    }
    
    @Override
    public String getSection () {
        return "error";
    }
    
}
