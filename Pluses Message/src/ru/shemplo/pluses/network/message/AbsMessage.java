package ru.shemplo.pluses.network.message;

import java.io.IOException;
import java.util.Objects;

import ru.shemplo.pluses.util.BitManip;

public abstract class AbsMessage implements Message {

    /**
     * 
     */
    private static final long serialVersionUID = 7929848654130956041L;
    
    protected final DirectionWord DIRECTION;
    private Message replyMessage;
    
    protected final boolean IS_REPLY, NEED_VERIFICATION, IS_REPEATED;
    
    public AbsMessage (Message reply, DirectionWord direction, 
            boolean replied, boolean verifying, boolean repeated) {
        this.DIRECTION    = direction;
        this.replyMessage = reply;
        
        this.IS_REPLY          = replied;
        this.NEED_VERIFICATION = verifying;
        this.IS_REPEATED       = repeated;
    }
    
    public AbsMessage (byte [] header) throws IOException {
        int directionOrdinal = BitManip.getBits (header [0], 6, 2); // 0b**______    _ - ignored
                                                                    //               x - selected
        
        this.DIRECTION         = DirectionWord.values () [directionOrdinal];
        this.IS_REPLY          = BitManip.getBit (header [1], 7) == 1;
        this.NEED_VERIFICATION = BitManip.getBit (header [1], 6) == 1;
        this.IS_REPEATED       = BitManip.getBit (header [1], 5) == 1;
        
        if (DEBUG) {
            System.out.println ("DO: " + directionOrdinal + ", R: " + IS_REPLY 
                + ", V: " + NEED_VERIFICATION + ", E: " + IS_REPEATED);
        }
    }
    
    @Override
    public DirectionWord getDirection () {
        return DIRECTION;
    }
    
    public void setReply (Message reply) {
        if (Objects.isNull (replyMessage)) {
            this.replyMessage = reply;
        }
    }
    
    public Message getReply () {
        return replyMessage;
    }
    
    public boolean isReply () {
        return IS_REPLY;
    }
    
    public boolean needVerification () {
        return NEED_VERIFICATION;
    }
    
    public boolean isRepeated () {
        return IS_REPEATED;
    }
    
}
