package ru.shemplo.pluses.struct;

import java.io.Serializable;
import java.util.Objects;

import org.json.JSONObject;

import ru.shemplo.pluses.network.message.HasJSON;

public class Trio <F, S, T> implements Serializable, HasJSON {
    
    /**
     * 
     */
    private static final long serialVersionUID = 2833820891560067271L;
    
    public final F F;
    public final S S;
    public final T T;
    
    public Trio (F f, S s, T t) {
        this.F = f; this.S = s; 
        this.T = t;
    }
    
    @Override
    public String toString () {
        return "<" + F + "; " + S + "; " + T + ">";
    }
    
    @Override
    public boolean equals (Object obj) {
        if (Objects.isNull (obj) 
            || !(obj instanceof Trio)) { 
            return false; 
        }
        
        Trio <?, ?, ?> trio = (Trio <?, ?, ?>) obj;
        return (Objects.isNull (F) ? Objects.isNull (trio.F) : F.equals (trio.F))
                && (Objects.isNull (S) ? Objects.isNull (trio.S) : S.equals (trio.S))
                && (Objects.isNull (T) ? Objects.isNull (trio.T) : T.equals (trio.T));
    }

    @Override
    public JSONObject toJSON (JSONObject root) {
        root.put ("trio", new Object [] {F, S, T});
        return root;
    }
    
    public static <F, S, T> Trio <F, S, T> mt (F f, S s, T t) {
        return new Trio <F, S, T> (f, s, t);
    }
    
}
