package ru.shemplo.pluses.logic;

import static ru.shemplo.pluses.network.message.AppMessage.MessageDirection.*;
import static ru.shemplo.pluses.network.message.ControlMessage.ControlType.*;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import ru.shemplo.pluses.log.Log;
import ru.shemplo.pluses.network.message.AppMessage;
import ru.shemplo.pluses.network.message.ControlMessage;
import ru.shemplo.pluses.network.pool.AppConnection;
import ru.shemplo.pluses.struct.OrganizationHistory;
import ru.shemplo.pluses.util.Arguments;

public class UpdateHandler {
    
    public static void runCreate (StringTokenizer tokens, AppMessage message,
            AppConnection connection) {
        if (!tokens.hasMoreTokens ()) { return; }
        String type = tokens.nextToken ().toUpperCase ();
        switch (type) {
            case "TASK":
                runUpdateTask (tokens, message, connection);
                break;
            
            default:
                String content = "Failed to update `" + type + "` (unknown type)";
                AppMessage error = new ControlMessage (STC, ERROR, 0, content);
                Log.error (CommandHandler.class.getSimpleName (), content);
                connection.sendMessage (error);
        }
    }
    
    private static void runUpdateTask (StringTokenizer tokens, AppMessage message,
            AppConnection connection) {
        Map <String, String> params = new HashMap <> ();
        params = Arguments.parse (params, tokens, null);
        
        int topicID = Integer.parseInt (params.get ("topic"));
        int taskID = Integer.parseInt (params.get ("id"));
        String title = params.get ("title");
        
        OrganizationHistory.renameTask (topicID, taskID, title);
    }
    
}
