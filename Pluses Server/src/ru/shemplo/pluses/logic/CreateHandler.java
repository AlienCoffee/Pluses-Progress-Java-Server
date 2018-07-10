package ru.shemplo.pluses.logic;

import static ru.shemplo.pluses.network.message.AppMessage.MessageDirection.*;
import static ru.shemplo.pluses.network.message.ControlMessage.ControlType.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import ru.shemplo.pluses.db.MySQLAdapter;
import ru.shemplo.pluses.log.Log;
import ru.shemplo.pluses.network.message.AppMessage;
import ru.shemplo.pluses.network.message.ControlMessage;
import ru.shemplo.pluses.network.message.Message;
import ru.shemplo.pluses.network.pool.AppConnection;
import ru.shemplo.pluses.util.Arguments;
import ru.shemplo.pluses.util.SQLUtil;

public class CreateHandler {
    
    public static void runCreate (StringTokenizer tokens, AppMessage message,
            AppConnection connection) {
        if (!tokens.hasMoreTokens ()) { return; }
        String type = tokens.nextToken ().toUpperCase ();
        switch (type) {
            case "GROUP":
                runCreateGroup (tokens, message, connection);
                break;
                
            case "STUDENT":
                runCreateStudent (tokens, message, connection);
                break;
                
            case "TOPIC":
                runCreateTopic (tokens, message, connection);
                break;
            
            default:
                String content = "Failed to create `" + type + "` (unknown type)";
                AppMessage error = new ControlMessage (STC, ERROR, 0, content);
                Log.error (CommandHandler.class.getSimpleName (), content);
                connection.sendMessage (error);
        }
    }
    
    private static void runCreateGroup (StringTokenizer tokens, AppMessage message,
            AppConnection connection) {
        Map <String, String> params = new HashMap <> ();
        params.put ("created", SQLUtil.getDatetime ());
        params = Arguments.parse (params, tokens, "title");
        
        String query = SQLUtil.makeInsertQuery ("groups", params);
        _runInsertQuery (query, connection);
    }
    
    private static void runCreateStudent (StringTokenizer tokens, AppMessage message,
            AppConnection connection) {
        Map <String, String> params = new HashMap <> ();
        params.put ("created", SQLUtil.getDatetime ());
        params = Arguments.parse (params, tokens, null);
        
        String query = SQLUtil.makeInsertQuery ("students", params);
        _runInsertQuery (query, connection);
    }
    
    private static void runCreateTopic (StringTokenizer tokens, AppMessage message,
            AppConnection connection) {
        Map <String, String> params = new HashMap <> ();
        params.put ("created", SQLUtil.getDatetime ());
        params = Arguments.parse (params, tokens, "title");
        
        String query = SQLUtil.makeInsertQuery ("topics", params);
        _runInsertQuery (query, connection);
    }
    
    private static void _runInsertQuery (String query, AppConnection connection) {
        MySQLAdapter adapter = MySQLAdapter.getInstance ();
        
        try {
            PreparedStatement statement = adapter.getDB ()
                .prepareStatement (query, Statement.RETURN_GENERATED_KEYS);
            if (statement.executeUpdate () == 1) {
                ResultSet keys = statement.getGeneratedKeys ();
                if (!keys.next ()) { 
                    Message error = new ControlMessage (STC, ERROR, 0, 
                        "Entry created successfully but `id` is unknown");
                    connection.sendMessage (error);
                    return;
                }
                
                int id = keys.getInt (1);
                Message success = new ControlMessage (STC, ID, id, "");
                connection.sendMessage (success);
            } else {
                Message error = new ControlMessage (STC, ERROR, 0, 
                    "Entry wasn't created by unknown reason");
                connection.sendMessage (error);
                return;
            }
        } catch (SQLException sqle) {
            Log.error (CommandHandler.class.getSimpleName (), sqle);
            Message error = new ControlMessage (STC, ERROR, 0, 
                "Entry wasn't created:\n" + sqle);
            connection.sendMessage (error);
        }
    }
    
}
