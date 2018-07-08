package ru.shemplo.pluses.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;

public class Arguments {

    public static Map <String, String> parse (Map <String, String> base, List <String> args) {
        Map <String, String> parametrs = new HashMap <> (base);
        for (int i = 0; i < args.size (); i++) {
            String argument = args.get (i);
            if (Objects.isNull (argument) || argument.length () == 0) {
                continue; // It's just a useless object in a list
            }
            
            if (argument.charAt (0) != '@' && argument.charAt (0) != '-') {
                continue; // It's just a string without key word
            }
            
            argument = argument.substring (1); // Skipping first support character
            if (i + 1 < args.size ()) {
                parametrs.put (argument, args.get (i + 1));
                i += 1; // 2 tokens handled by the iteration
            }
        }
        
        return parametrs;
    }
    
    public static Map <String, String> parse (Map <String, String> base, StringTokenizer tokens) {
        List <String> arguments = new ArrayList <> ();
        while (tokens.hasMoreTokens ()) {
            arguments.add (tokens.nextToken ());
        }
        
        return parse (base, arguments);
    }
    
}
