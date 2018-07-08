package ru.shemplo.pluses.network.message;

public class JSONMessage extends AbsAppMessage {

    /**
     * 
     */
    private static final long serialVersionUID = -9105112801272097899L;

    private final String JSON;
    
    public JSONMessage (MessageDirection direction, String json) {
        super (direction);
        this.JSON = json;
    }

    @Override
    public String getContent () {
        return JSON;
    }
    
}
