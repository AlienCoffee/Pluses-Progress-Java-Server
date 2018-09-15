package ru.shemplo.pluses.util;

import java.util.concurrent.atomic.AtomicInteger;

import ru.shemplo.pluses.network.message.Message.DirectionWord;
import ru.shemplo.pluses.network.message.app.CommandMessage.CommandWord;
import ru.shemplo.pluses.network.message.app.CommandMessage.TypeWord;

public class BitManip {
    
    private static AtomicInteger counter = new AtomicInteger (0);
    
    public static int nextID () {
        while (true) {
            int current = counter.intValue ();
            if (counter.compareAndSet (current, current + 1)) {
                return current;
            }
        }
    }
    
    public static int getBit (int number, int index) {
        return 0b1 & (number >>> index);
    }
    
    public static int getBits (int number, int index, int length) {
        int offset = 32 - length - index;
        return (number << offset) >>> (index + offset);
    }
    
    public static byte [] genMessageHeader (DirectionWord dir, int messType, 
            boolean reply, boolean verification, boolean repeat) {
        byte [] out = new byte [2];                           //__6 bits
        out [0] = (byte) ((dir.ordinal () << 6) | (messType & 0b00111111));
        out [1] = (byte) (
                      ((reply        ? 0b1 : 0b0) << 7) 
                    | ((verification ? 0b1 : 0b0) << 6) 
                    | ((repeat       ? 0b1 : 0b0) << 5)
                  ); // it's not sad face
        return out;
    }
    
    public static byte [] genCommandMessageHeader (CommandWord com, TypeWord typ, int params) {
        byte [] out = new byte [2];                                 //__4 bits
        out [0] = (byte) ((com.ordinal () << 4) | (typ.ordinal () & 0b00001111));
                                   //__6 bits
        out [1] = (byte) (params & 0b00111111);
        return out;
    }
    
}
