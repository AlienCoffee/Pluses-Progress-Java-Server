package ru.shemplo.pluses.network.message;

import java.io.Serializable;
import java.util.Random;

import org.json.JSONObject;

public interface Message extends Serializable {
    
    public static final Random RANDOM = new Random ();
    
    public JSONObject toJSON (JSONObject root);
    
    public long getTimestamp ();
    
    public int getID ();
    
}
