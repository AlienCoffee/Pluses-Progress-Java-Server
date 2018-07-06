package ru.shemplo.pluses.network.message;

import java.io.Serializable;

public interface Message extends Serializable {
    
    public long getTimestamp ();
    
}
