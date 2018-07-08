package ru.shemplo.pluses.network.message;

public class JavaAppError extends AbsAppMessage {

    /**
     * 
     */
    private static final long serialVersionUID = -7511146492305268573L;

    private final String ERROR;
    
    public JavaAppError (MessageDirection direction, String error) {
        super (direction);
        this.ERROR = error;
    }
    
    public JavaAppError (MessageDirection direction, Exception exception) {
        this (direction, "Exception occured (" + exception + ")");
    }

    @Override
    public String getContent () {
        return ERROR;
    }
    
}
