package ru.shemplo.pluses.util;

//import static ru.shemplo.pluses.network.message.AppMessage.MessageDirection.*;
//import static ru.shemplo.pluses.network.message.ControlMessage.ControlType.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import org.apache.commons.text.StringEscapeUtils;
import ru.shemplo.pluses.network.message.ControlMessage;
import ru.shemplo.pluses.network.message.Message;
import ru.shemplo.pluses.network.message.AppMessage;
import ru.shemplo.pluses.network.pool.AppConnection;

import static ru.shemplo.pluses.network.message.AppMessage.MessageDirection.STC;
import static ru.shemplo.pluses.network.message.ControlMessage.ControlType.ERROR;

public class Arguments {

    public static Map <String, String> readAndCheck (String action, Set <String> paramsSet,
                                                     StringTokenizer tokens, AppMessage message, AppConnection connection) {
        Map <String, String> params = parse (new HashMap <> (), tokens, null);

        for (String name : paramsSet) {
            if (params.containsKey (name)) { continue; }
            Message error = new ControlMessage(message, STC, ERROR, 0,
                action + " failed, parameter missed: [" + name + "]");
            connection.sendMessage (error);
            
            // Special signal that error was sent to client
            return null;
        }
        
        return params;
    }
    
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
        args = args.stream ().map (StringEscapeUtils::escapeJava)
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
