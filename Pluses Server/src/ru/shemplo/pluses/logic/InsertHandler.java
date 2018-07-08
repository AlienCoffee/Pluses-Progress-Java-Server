package ru.shemplo.pluses.logic;

import static ru.shemplo.pluses.network.message.AppMessage.MessageDirection.*;
import static ru.shemplo.pluses.network.message.ControlMessage.ControlType.*;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import ru.shemplo.pluses.db.MySQLAdapter;
import ru.shemplo.pluses.log.Log;
import ru.shemplo.pluses.network.message.AppMessage;
import ru.shemplo.pluses.network.message.ControlMessage;
import ru.shemplo.pluses.network.pool.AppConnection;
import ru.shemplo.pluses.struct.OrganizationTree;

public class InsertHandler {
    
    // IMPORTANT // This is temporal solution // It will be removed in future
    private static final OrganizationTree TREE = new OrganizationTree ();
    
    public static void runInsert (StringTokenizer tokens, AppMessage message, 
            AppConnection connection) {
        if (!tokens.hasMoreTokens ()) { return; }
        String type = tokens.nextToken ().toUpperCase ();
        switch (type) {
            case "CLASS":
                runInsertClass (tokens, message, connection);
                break;
            
            default:
                String content = "Failed to insert `" + type + "` (unknown type)";
                AppMessage error = new ControlMessage (STC, ERROR, 0, content);
                Log.error (CommandHandler.class.getSimpleName (), content);
                connection.sendMessage (error);
        }
    }
    
    private static void runInsertClass (StringTokenizer tokens, AppMessage message,
            AppConnection connection) {
        if (!tokens.hasMoreTokens ()) { return; }
        List <Integer> ids = new ArrayList <> ();
        String token = null;
        while (true) {
            if (tokens.hasMoreTokens ()) {
                token = tokens.nextToken ();
            } else { break; }
            
            try {
                ids.add (Integer.parseInt (token));
            } catch (NumberFormatException nfe) {
                break;
            }
        }
        
        String query = "SELECT `id` FROM `teachers` WHERE `id` IN ? ORDER BY `id` ASC";
        ids = MySQLAdapter.getInstance ().runFetchFromArray (query, ids);
        TREE.getOrganization ().commitAll (ids);
        System.out.println (TREE.toString ());
    }
    
}
