package ru.shemplo.pluses.network.message;

import java.io.Serializable;

public interface Message extends Serializable /* TOO MANY DOUBTS */ {
    
    public static final boolean DEBUG = true;
    
    public static enum DirectionWord {
        StC /* Server to Client */, CtS /* Client to Server */,
        CtC /* Client to Client */, StS /* Server to Server */
    }
    
    /*
     * Message frame
     * 
     *  0               1                             
     *  0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
     * +---+-----------+-+-+-+---------+
     * | D | MESS TYPE |R|V|E| xxxxxxx |
     * +---+-----------+-+-+-+---------+
     * |                               |
     *  . . . . . . . . . . . . . . . .
     * |                               |
     * +-------------------------------+
     * 
     * ==================================
     * D         | direction    | 2 bits
     * MESS TYPE | message type | 6 bits
     * R         | reply        | 1 bit
     * V         | verification | 1 bit
     * E         | rEpeated     | 1 bit
     * ----------+--------------+--------
     * TOTAL                    | 11 bits
     * ==================================
     * 
     * @ Direction (D) - label that shows who is sender
     * and who is receiver. If receiver got message
     * that is not addressed to him, it must read it
     * all (because length is not declared in header)
     * and then ignore this message
     * 
     * @ Message type (MESS TYPE) - integer value that
     * equals to some ordinal of message type. It is
     * necessary for interpretation of following data
     * 
     * @ Reply (R) - flag that shows that this message is
     * answer for next message (in input stream)
     * 
     * @ Verification (V) - flag that notify receiver
     * that sender waiting for verification of receiving
     * 
     * @ Repeated (E) - label that shows that this message
     * was already sent (but not fact that received)
     * and receiver should check for repeating of 
     * computation of this message
     * 
     */
    
    /**
     * 
     * 
     * @return
     * 
     */
    public DirectionWord getDirection ();
    
    /**
     * 
     * 
     * @return
     * 
     */
    public Message getReply ();
    
    /**
     * 
     * 
     * @return
     * 
     */
    public byte [] toByteArray ();
    
}
