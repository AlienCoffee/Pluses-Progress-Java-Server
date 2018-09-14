package ru.shemplo.pluses.struct;

import java.io.Serializable;
import java.util.Objects;

public class Pair <F, S> implements Serializable {
    
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
    public boolean equals (Object obj) {
        if (Objects.isNull (obj) 
            || !(obj instanceof Pair)) { 
            return false; 
        }
        
        Pair <?, ?> pair = (Pair <?, ?>) obj;
        return (Objects.isNull (F) ? Objects.isNull (pair.F) : F.equals (pair.F))
                && (Objects.isNull (S) ? Objects.isNull (pair.S) : S.equals (pair.S));
    }
    
    public static <F, S> Pair <F, S> mp (final F f, final S s) {
        return new Pair <F, S> (f, s);
    }
    
}
