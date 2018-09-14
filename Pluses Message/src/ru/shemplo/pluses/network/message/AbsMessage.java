package ru.shemplo.pluses.network.message;

import java.io.IOException;
import java.io.InputStream;

import ru.shemplo.pluses.util.BitManip;

public abstract class AbsMessage implements Message {

    /**
     * 
     */
    private static final long serialVersionUID = 7929848654130956041L;
    
    protected final DirectionWord DIRECTION;
    protected final Message REPLY;
    
    protected final boolean IS_REPLY, NEED_VERIFICATION, IS_REPEATED;
    
    public AbsMessage (Message reply, DirectionWord direction, 
            boolean replied, boolean verifying, boolean repeated) {
        this.DIRECTION = direction;
        this.REPLY     = reply;
        
        this.IS_REPLY          = replied;
        this.NEED_VERIFICATION = verifying;
        this.IS_REPEATED       = repeated;
    }
    
    public AbsMessage (InputStream is) throws IOException {
        this.REPLY = null;
        
        byte [] header = new byte [2];
        is.read (header, 0, header.length);
        
                                                                    //               x - selected
        int directionOrdinal = BitManip.getBits (header [0], 6, 2); // 0bxx______    _ - ignored
        this.DIRECTION = DirectionWord.values () [directionOrdinal];
        
        this.IS_REPLY          = BitManip.getBit (header [1], 7) == 1;
        this.NEED_VERIFICATION = BitManip.getBit (header [1], 6) == 1;
        this.IS_REPEATED       = BitManip.getBit (header [1], 5) == 1;
        
        int messageOrdinal   = BitManip.getBits (header [0], 0, 6); // 0b__xxxxxx
        
        if (Message.DEBUG) {
            System.out.println ("DO: " + directionOrdinal + ", MO: " + messageOrdinal 
                    + ", R: " + IS_REPLY + ", V: " + NEED_VERIFICATION + ", E: " + IS_REPEATED);
        }
    }
    
}
