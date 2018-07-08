package ru.shemplo.pluses.util;

import java.util.Iterator;

public class SQLUtil {
 
    public static String toArray (Iterable <?> iterable, char bound) {
        Iterator <?> iterator = iterable.iterator ();
        StringBuilder sb = new StringBuilder ("(");
        while (iterator.hasNext ()) {
            Object object = iterator.next ();
            sb.append (bound);
            sb.append ("" + object);
            sb.append (bound);
            if (iterator.hasNext ()) {
                sb.append (", ");
            }
        }
        sb.append (")");
        
        return sb.toString ();
    }
    
}
