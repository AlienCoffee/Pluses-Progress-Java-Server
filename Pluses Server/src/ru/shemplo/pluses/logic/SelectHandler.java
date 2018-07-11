package ru.shemplo.pluses.logic;

import static ru.shemplo.pluses.network.message.AppMessage.MessageDirection.*;
import static ru.shemplo.pluses.network.message.ControlMessage.ControlType.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import ru.shemplo.pluses.log.Log;
import ru.shemplo.pluses.network.message.AppMessage;
import ru.shemplo.pluses.network.message.ControlMessage;
import ru.shemplo.pluses.network.message.ListMessage;
import ru.shemplo.pluses.network.message.Message;
import ru.shemplo.pluses.network.pool.AppConnection;
import ru.shemplo.pluses.struct.OrganizationHistory;
import ru.shemplo.pluses.util.Arguments;

public class SelectHandler {
 
    public static void runSelect (StringTokenizer tokens, AppMessage message,
            AppConnection connection) {
        if (!tokens.hasMoreTokens ()) { return; }
        String type = tokens.nextToken ().toUpperCase ();
        switch (type) {
            case "GROUPS":
                runSelectGroups (tokens, message, connection);
                break;
            
            case "TOPICS":
                System.out.println ("Topics of student 1:");
                System.out.println (OrganizationHistory.getTopics ());
                break;
            
            default:
                String content = "Failed to select `" + type + "` (unknown type)";
                AppMessage error = new ControlMessage (STC, ERROR, 0, content);
                Log.error (CommandHandler.class.getSimpleName (), content);
                connection.sendMessage (error);
        }
    }
    
    private static void runSelectGroups (StringTokenizer tokens, AppMessage message,
            AppConnection connection) {
        Map <String, String> params = new HashMap <> ();
        params = Arguments.parse (params, tokens, null);
        
        List <Integer> groups = OrganizationHistory.getGroups (params);
        Message list = new ListMessage<> (message, STC, groups);
        connection.sendMessage (list);
    }
    
}
