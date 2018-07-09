package ru.shemplo.pluses.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringEscapeUtils;

public class Arguments {

    public static Map <String, String> parse (Map <String, String> base, 
            StringTokenizer tokens, String first) {
        List <String> args = new ArrayList <> ();
        if (!Objects.isNull (first)) {
            args.add ("-" + first); // first parameter  
        }
        
        while (tokens.hasMoreTokens ()) {
            args.add (tokens.nextToken ());
        }
        
        // Change special symbols to their's codes (f.e. > -> &gt;)
        args = args.stream ().map (StringEscapeUtils::escapeHtml)
            .map (StringEscapeUtils::escapeJavaScript)
            .map (StringEscapeUtils::escapeXml)
            .collect (Collectors.toList ());
        
        return parse (base, args);
    }
    
    public static Map <String, String> parse (Map <String, String> base, 
            List <String> args) {
        Map <String, String> parametrs = new HashMap <> (base);
        StringBuilder sb = new StringBuilder ();
        String key = "";
        
        for (int i = 0; i < args.size (); i++) {
            String argument = args.get (i);
            
            if (argument.charAt (0) != '@' && argument.charAt (0) != '-') {
                sb.append (argument);
                sb.append (" ");
                continue;
            }
            
            argument = argument.substring (1); // Skipping first support character
            if (key.length () > 0 && sb.length () > 0) {
                parametrs.put (key, sb.toString ().trim ());
            }
            
            sb = new StringBuilder ();
            key = argument;
        }
        
        if (key.length () > 0 && sb.length () > 0) {
            parametrs.put (key, sb.toString ().trim ());
        }
        
        return parametrs;
    }
    
}
