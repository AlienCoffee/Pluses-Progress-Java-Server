package ru.shemplo.pluses.network.message;

public abstract class AbsAppMessage implements AppMessage {

    /**
     * 
     */
    private static final long serialVersionUID = -2736659415758637345L;
    
    protected final MessageDirection DIRECTION;
    protected final long TIMESTAMP;
    
    public AbsAppMessage (MessageDirection direction) {
        this.TIMESTAMP = System.currentTimeMillis ();
        this.DIRECTION = direction;
    }
    
    @Override
    public long getTimestamp () {
        return TIMESTAMP;
    }
    
    @Override
    public MessageDirection getDirection () {
        return DIRECTION;
    }
    
}
