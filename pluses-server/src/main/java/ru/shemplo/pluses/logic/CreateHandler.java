package ru.shemplo.pluses.logic;

import static ru.shemplo.pluses.network.message.AppMessage.MessageDirection.*;
import static ru.shemplo.pluses.network.message.ControlMessage.ControlType.*;
import static ru.shemplo.pluses.struct.OrganizationHistory.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import ru.shemplo.pluses.db.MySQLAdapter;
import ru.shemplo.pluses.log.Log;
import ru.shemplo.pluses.network.message.AppMessage;
import ru.shemplo.pluses.network.message.ControlMessage;
import ru.shemplo.pluses.network.message.Message;
import ru.shemplo.pluses.network.pool.AppConnection;
import ru.shemplo.pluses.struct.OrganizationHistory;
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
                
            case "TASK":
                runCreateTask (tokens, message, connection);
                break;
            
            default:
                String content = "Failed to create `" + type + "` (unknown type)";
                AppMessage error = new ControlMessage (STC, ERROR, 0, content);
                Log.error (CommandHandler.class.getSimpleName (), content);
                connection.sendMessage (error);
        }
    }
    
    private static final Set <String> CREATE_TASK_PARAMS = new HashSet <> (
        Arrays.asList ("topic", "title")
    );
    
    private static void runCreateTask (StringTokenizer tokens, AppMessage message,
            AppConnection connection) {
        Map <String, String> params = new HashMap <> ();
        params = Arguments.parse (params, tokens, null);
        
        for (String name : CREATE_TASK_PARAMS) {
            if (params.containsKey (name)) { continue; }
            Message error = new ControlMessage (message, STC, ERROR, 0, 
                "Create task failed, parameter missed: [" + name + "]");
            connection.sendMessage (error);
            return;
        }
        
        int topicID = Integer.parseInt (params.get ("topic"));
        String title = params.get ("title");
        
        OrganizationHistory.createTask (topicID, title);
        
        Message answer = new ControlMessage (message, STC, INFO, 0, "New task created");
        connection.sendMessage (answer);
    }
    
    private static void runCreateGroup (StringTokenizer tokens, AppMessage message,
            AppConnection connection) {
        Map <String, String> params = new HashMap <> ();
        params.put ("created", SQLUtil.getDatetime ());
        params = Arguments.parse (params, tokens, "title");
        
        String query = SQLUtil.makeInsertQuery ("groups", params);
        _runInsertQuery (message, query, "group", connection);
    }
    
    private static void runCreateStudent (StringTokenizer tokens, AppMessage message,
            AppConnection connection) {
        Map <String, String> params = new HashMap <> ();
        params.put ("created", SQLUtil.getDatetime ());
        params = Arguments.parse (params, tokens, null);
        
        String query = SQLUtil.makeInsertQuery ("students", params);
        _runInsertQuery (message, query, "student", connection);
    }
    
    private static void runCreateTopic (StringTokenizer tokens, AppMessage message,
            AppConnection connection) {
        Map <String, String> params = new HashMap <> ();
        params.put ("created", SQLUtil.getDatetime ());
        params = Arguments.parse (params, tokens, "title");
        
        String query = SQLUtil.makeInsertQuery ("topics", params);
        _runInsertQuery (message, query, "topic", connection);
    }
    
    private static void _runInsertQuery (AppMessage message, String query, 
            String type,AppConnection connection) {
        MySQLAdapter adapter = MySQLAdapter.getInstance ();
        
        try {
            PreparedStatement statement = adapter.getDB ()
                .prepareStatement (query, Statement.RETURN_GENERATED_KEYS);
            if (statement.executeUpdate () == 1) {
                ResultSet keys = statement.getGeneratedKeys ();
                if (!keys.next ()) { 
                    Message error = new ControlMessage (message, STC, ERROR, 0, 
                        "Entry created successfully but `id` is unknown");
                    connection.sendMessage (error);
                    return;
                }
                
                int newID = keys.getInt (1);
             
                // Also adding created line to history instance
                switch (type) {
                    case "student": createStudent (newID); break;
                    case "group"  : createGroup (newID); break;
                    case "topic"  : createTopic (newID); break;
                }
                
                // Sending message to client with `id` of just created line
                Message success = new ControlMessage (message, STC, ID, 
                   newID, "New line inserted");
                connection.sendMessage (success);
            } else {
                Message error = new ControlMessage (message, STC, ERROR, 0, 
                    "Entry wasn't created by unknown reason");
                connection.sendMessage (error);
                return;
            }
        } catch (SQLException | IllegalStateException es) {
            Log.error (CommandHandler.class.getSimpleName (), es);
            Message error = new ControlMessage (message, STC, ERROR, 0, 
                "Entry wasn't created:\n" + es);
            connection.sendMessage (error);
        }
    }
    
}
