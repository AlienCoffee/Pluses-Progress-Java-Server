package ru.shemplo.pluses.logic;

import static ru.shemplo.pluses.network.message.AppMessage.MessageDirection.*;
import static ru.shemplo.pluses.network.message.ControlMessage.ControlType.*;

import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
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
        Arrays.asList ("student", "group", "topic", "task", "verdict", "teacher")
    );
    
    private static void runInsertTry (StringTokenizer tokens, AppMessage message, 
            AppConnection connection) {
        Map <String, String> params = Arguments.readAndCheck ("insert try", 
                INSERT_TRY_PARAMS, tokens, message, connection);
        if (Objects.isNull (params)) { return; }
        
        Timestamp timestamp = new Timestamp (System.currentTimeMillis ());
        params.put ("time", SQLUtil.getDatetime ());
        
        int teacherID = Integer.parseInt (params.get ("teacher"));
        int studentID = Integer.parseInt (params.get ("student"));
        int verdict = Integer.parseInt (params.get ("verdict"));
        int groupID = Integer.parseInt (params.get ("group"));
        int topicID = Integer.parseInt (params.get ("topic"));
        int taskID = Integer.parseInt (params.get ("task"));
        
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
    
    private static final Set <String> INSERT_STUDENT_PARAMS = new HashSet <> (
        Arrays.asList ("group", "student")
    );

    private static void runInsertStudent (StringTokenizer tokens, AppMessage message, 
            AppConnection connection) {
        Map <String, String> params = Arguments.readAndCheck ("insert topic", 
                INSERT_STUDENT_PARAMS, tokens, message, connection);
        if (Objects.isNull (params)) { return; }
        
        int studentID = Integer.parseInt (params.get ("student"));
        int groupID = Integer.parseInt (params.get ("group"));
        
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
            params.put ("time", SQLUtil.getDatetime (time.getTime ()));
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
    
    private static final Set <String> INSERT_TOPIC_PARAMS = new HashSet <> (
        Arrays.asList ("group", "topic")
    );
    
    private static void runInsertTopic (StringTokenizer tokens, AppMessage message, 
            AppConnection connection) {
        Map <String, String> params = Arguments.readAndCheck ("insert topic", 
                INSERT_TOPIC_PARAMS, tokens, message, connection);
        if (Objects.isNull (params)) { return; }
        
        int groupID = Integer.parseInt (params.get ("group"));
        int topicID = Integer.parseInt (params.get ("topic"));
        
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
