package ru.shemplo.pluses.logic;

import static ru.shemplo.pluses.network.message.AppMessage.MessageDirection.*;
import static ru.shemplo.pluses.network.message.ControlMessage.ControlType.*;

import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
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

public class InsertHandler {
    
    public static void runInsert (StringTokenizer tokens, AppMessage message,
            AppConnection connection) {
        if (!tokens.hasMoreTokens ()) { return; }
        String type = tokens.nextToken ().toUpperCase ();
        switch (type) {
            case "STUDENT":
                runInsertStudent (tokens, message, connection);
                break;
                
            case "TOPIC":
                runInsertTopic (tokens, message, connection);
                break;
                
            case "TRY":
                runInsertTry (tokens, message, connection);
                break;
            
            default:
                String content = "Failed to insert `" + type + "` (unknown type)";
                AppMessage error = new ControlMessage (STC, ERROR, 0, content);
                Log.error (CommandHandler.class.getSimpleName (), content);
                connection.sendMessage (error);
        }
    }
    
    private static final Set <String> INSERT_TRY_PARAMS = new HashSet <> (
        Arrays.asList ("student", "group", "topic", "task", "res", "teacher")
    );
    
    private static void runInsertTry (StringTokenizer tokens, AppMessage message, 
            AppConnection connection) {
        Map <String, String> params = new HashMap <> ();
        params = Arguments.parse (params, tokens, null);
        
        for (String name : INSERT_TRY_PARAMS) {
            if (params.containsKey (name)) { continue; }
            Message error = new ControlMessage (message, STC, ERROR, 0, 
                "Insert verdict failed, parameter missed: [" + name + "]");
            connection.sendMessage (error);
            return;
        }
        
        Timestamp timestamp = new Timestamp (System.currentTimeMillis ());
        int teacherID = Integer.parseInt (params.get ("teacher"));
        int studentID = Integer.parseInt (params.get ("student"));
        int groupID = Integer.parseInt (params.get ("group"));
        int topicID = Integer.parseInt (params.get ("topic"));
        int taskID = Integer.parseInt (params.get ("task"));
        int verdict = Integer.parseInt (params.get ("res"));
        
        try {
            boolean isOK = 1 == verdict;
            OrganizationHistory.insertTry (groupID, studentID, 
                topicID, taskID, teacherID, isOK, timestamp);
            MySQLAdapter adapter = MySQLAdapter.getInstance ();
            Statement statement = adapter.getDB ().createStatement ();
            String query = SQLUtil.makeInsertQuery ("tries", params);
            statement.execute (query);
        } catch (SQLException | IllegalStateException se) {
            Message error = new ControlMessage (message, STC, ERROR, 0, 
                "Insert try failed:\n" + se);
            connection.sendMessage (error);
            return;
        }
    }

    private static void runInsertStudent (StringTokenizer tokens, AppMessage message, 
            AppConnection connection) {
        if (!tokens.hasMoreTokens ()) {
            Message error = new ControlMessage (message, STC, ERROR, 0, 
                "Insert student failed, argument missed: [student id]");
            connection.sendMessage (error);
            return;
        }
        
        int studentID = -1;
        try {
            studentID = Integer.parseInt (tokens.nextToken ());
        } catch (NumberFormatException nfe) {
            Message error = new ControlMessage (message, STC, ERROR, 0, 
                "Insert student failed, argument [student id] must be integer");
            connection.sendMessage (error);
            return;
        }
        
        if (!tokens.hasMoreTokens ()) {
            Message error = new ControlMessage (message, STC, ERROR, 0, 
                "Insert student failed, argument missed: [group id]");
            connection.sendMessage (error);
            return;
        }
        
        int groupID = -1;
        try {
            groupID = Integer.parseInt (tokens.nextToken ());
        } catch (NumberFormatException nfe) {
            Message error = new ControlMessage (message, STC, ERROR, 0, 
                "Insert student failed, argument [group id] must be integer");
            connection.sendMessage (error);
            return;
        }
        
        if (!OrganizationHistory.existsStudent (studentID)) {
            Message error = new ControlMessage (message, STC, ERROR, 0, 
                "Insert student failed, no such student " + studentID);
            connection.sendMessage (error);
            return;
        }
        
        try {
            Timestamp time = new Timestamp (System.currentTimeMillis ());
            // Also adding changes to history instance
            OrganizationHistory.insertStudent (studentID, groupID, time);
            
            MySQLAdapter adapter = MySQLAdapter.getInstance ();
            
            Map <String, String> params = new HashMap <> ();
            params.put ("time", SQLUtil.getDatetime (time.getTime ()));
            params.put ("student", "" + studentID);
            params.put ("to", "" + groupID);
            
            String query = SQLUtil.makeInsertQuery ("movements", params);
            Statement statement = adapter.getDB ().createStatement ();
            statement.execute (query); // Committing changes to DB
        } catch (SQLException sqle) {
            Log.error (CommandHandler.class.getSimpleName (), sqle);
            Message error = new ControlMessage (message, STC, ERROR, 0, 
                "Insert student failed:\n" + sqle);
            connection.sendMessage (error);
        }
    }
    
    private static void runInsertTopic (StringTokenizer tokens, AppMessage message, 
            AppConnection connection) {
        if (!tokens.hasMoreTokens ()) {
            Message error = new ControlMessage (message, STC, ERROR, 0, 
                "Insert topic failed, argument missed: [topic id]");
            connection.sendMessage (error);
            return;
        }
        
        int topicID = -1;
        try {
            topicID = Integer.parseInt (tokens.nextToken ());
        } catch (NumberFormatException nfe) {
            Message error = new ControlMessage (message, STC, ERROR, 0, 
                "Insert topic failed, argument [topic id] must be integer");
            connection.sendMessage (error);
            return;
        }
        
        if (!tokens.hasMoreTokens ()) {
            Message error = new ControlMessage (message, STC, ERROR, 0, 
                "Insert topic failed, argument missed: [group id]");
            connection.sendMessage (error);
            return;
        }
        
        int groupID = -1;
        try {
            groupID = Integer.parseInt (tokens.nextToken ());
        } catch (NumberFormatException nfe) {
            Message error = new ControlMessage (message, STC, ERROR, 0, 
                "Insert topic failed, argument [group id] must be integer");
            connection.sendMessage (error);
            return;
        }
        
        if (!OrganizationHistory.existsTopic (topicID)) {
            Message error = new ControlMessage (message, STC, ERROR, 0, 
                "Insert topic failed, no such topic " + topicID);
            connection.sendMessage (error);
            return;
        }
        
        try {
            long time = System.currentTimeMillis ();
            
            // Adding changes to history instance
            OrganizationHistory.insertTopic (topicID, groupID, time);
        } catch (IllegalStateException ise) {
            Log.error (CommandHandler.class.getSimpleName (), ise);
            Message error = new ControlMessage (message, STC, ERROR, 0, 
                "Insert student failed:\n" + ise);
            connection.sendMessage (error);
        }
    }
    
}
