package ru.shemplo.pluses.network.message.app;

import java.io.IOException;
import java.io.InputStream;

import ru.shemplo.pluses.network.message.AbsMessage;
import ru.shemplo.pluses.network.message.Message;

public abstract class AbsAppMessage extends AbsMessage implements AppMessage {

    /**
     * 
     */
    private static final long serialVersionUID = 8543596305168807368L;
    
    public AbsAppMessage (Message reply, DirectionWord direction, 
            boolean replied, boolean verifying, boolean repeated) {
        super (reply, direction, replied, verifying, repeated);
    }
    
    public AbsAppMessage (InputStream is) throws IOException {
        super (is);
    }
    
    @Override
    public DirectionWord getDirection () {
        return DIRECTION;
    }
    
    @Override
    public Message getReply () {
        return REPLY;
    }
    
}
