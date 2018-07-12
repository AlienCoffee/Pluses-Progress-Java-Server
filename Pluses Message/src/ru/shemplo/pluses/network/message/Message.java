package ru.shemplo.pluses.network.message;

import java.io.Serializable;
import java.util.Random;

public interface Message extends Serializable, HasJSON {
    
    public static final Random RANDOM = new Random ();
    
    public long getTimestamp ();
    
    public int getID ();
    
}
