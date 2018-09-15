package ru.shemplo.pluses.util;

import java.io.IOException;
import java.io.InputStream;

import ru.shemplo.pluses.network.message.AbsMessage;
import ru.shemplo.pluses.network.message.Message;
import ru.shemplo.pluses.network.message.Message.MessageWord;

public class Messenger {
    
    public static <R extends Message> R readMessage (InputStream is) {
        try {
            byte [] header = new byte [2];
            is.read (header, 0, header.length);
            
            int messageOrdinal  = BitManip.getBits (header [0], 0, 6);
            MessageWord message = MessageWord.values () [messageOrdinal];
            
            R result = message.fromByteArray (header, is);
            if (result.isReply () && result instanceof AbsMessage) {
                AbsMessage abstraction = (AbsMessage) result;
                abstraction.setReply (readMessage (is));
            }
            
            return result;
        } catch (IOException ioe) {
            if (Message.DEBUG) { System.err.println (ioe); }
            return null;
        }
    }
    
}
