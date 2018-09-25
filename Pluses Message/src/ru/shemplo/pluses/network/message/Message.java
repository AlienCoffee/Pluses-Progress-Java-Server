package ru.shemplo.pluses.network.message;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;

import ru.shemplo.pluses.network.message.app.CommandMessage;

public interface Message extends Serializable /* TOO MANY DOUBTS */ {
    
    public static final boolean DEBUG = true;
    
    public static enum DirectionWord {
        StC /* Server to Client */, CtS /* Client to Server */,
        CtC /* Client to Client */, StS /* Server to Server */
    }
    
    public static enum MessageWord {
        
        COMMAND (CommandMessage.class),
        CONTROL (ControlMessage.class)
        ;
        
        private final Class <? extends Message> TOKEN;
        
        private MessageWord (Class <? extends Message> token) {
            this.TOKEN = token;
        }
        
        public <R extends Message> R fromByteArray (byte [] header, InputStream is) throws IOException {
            try {
                Constructor <? extends Message> 
                    constructor = TOKEN.getConstructor (byte [].class, InputStream.class);
                
                @SuppressWarnings ("unchecked")
                R instance = (R) constructor.newInstance (header, is);
                return instance;
            } catch (Exception e) { throw new IOException (e); }
        }
        
        public <R extends Message> R fromByteArray (byte [] header, byte [] bytes) throws IOException {
            try {
                Constructor <? extends Message> 
                    constructor = TOKEN.getConstructor (byte [].class, byte [].class);
                
                @SuppressWarnings ("unchecked")
                R instance = (R) constructor.newInstance (header, bytes);
                return instance;
            } catch (NoSuchMethodException nsme) {
                /* One more attempt */
                ByteArrayInputStream bais = new ByteArrayInputStream (bytes);
                return fromByteArray (header, bais);
            } catch (Exception e) { throw new IOException (e); }
        }
        
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
    public boolean isReply ();
    
    /**
     * 
     * 
     * @return
     * 
     */
    public boolean needVerification ();
    
    /**
     * 
     * 
     * @return
     * 
     */
    public boolean isRepeated ();
    
    /**
     * 
     * 
     * @return
     * 
     */
    public byte [] toByteArray ();
    
}
