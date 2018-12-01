package ru.shemplo.pluses.struct;

import java.io.Serializable;

import org.json.JSONObject;

import ru.shemplo.pluses.network.message.HasJSON;

public class Pair <F, S> implements Serializable, HasJSON {
    
    /**
     * 
     */
    private static final long serialVersionUID = 8987198961832024934L;

    public final F F; public final S S;
    
    public Pair (final F f, final S s) {
        this.F = f; this.S = s;
    }
    
    @Override
    public String toString () {
        return "<" + F + "; " + S + ">";
    }
    
    @Override
    public JSONObject toJSON (JSONObject root) {
        root.put ("pair", new Object [] {F, S});
        return root;
    }
    
    public static <F, S> Pair <F, S> mp (final F f, final S s) {
        return new Pair <F, S> (f, s);
    }
    
}
