package ru.shemplo.pluses.logic;

import static ru.shemplo.pluses.network.message.AppMessage.MessageDirection.STC;
import static ru.shemplo.pluses.network.message.ControlMessage.ControlType.ERROR;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.lang.RandomStringUtils;

import ru.shemplo.pluses.db.MySQLAdapter;
import ru.shemplo.pluses.log.Log;
import ru.shemplo.pluses.network.message.AppMessage;
import ru.shemplo.pluses.network.message.ControlMessage;
import ru.shemplo.pluses.network.message.ControlMessage.ControlType;
import ru.shemplo.pluses.network.message.ListMessage;
import ru.shemplo.pluses.network.message.Message;
import ru.shemplo.pluses.network.pool.AppConnection;
import ru.shemplo.pluses.struct.OrganizationHistory;
import ru.shemplo.pluses.struct.Pair;
import ru.shemplo.pluses.struct.Trio;
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
                
            case "INFO":
                runSelectInfo (tokens, message, connection);
                break;
            
            case "TOPICS":
                runSelectTopics (tokens, message, connection);
                break;
                
            case "STUDENTS":
                runSelectStudents (tokens, message, connection);
                break;
                
            case "TASKS":
                runSelectTasks (tokens, message, connection);
                break;
                
            case "KEY":
                String key = RandomStringUtils.random (32 + 32, true, true);
                Message answer = new ControlMessage (message, STC, ControlType.INFO, 0, key);
                connection.sendMessage (answer);
                break;
                
            case "PROGRESS":
                runSelectProgress (tokens, message, connection);
                break;
            
            default:
                String content = "Failed to select `" + type + "` (unknown type)";
                AppMessage error = new ControlMessage (STC, ERROR, 0, content);
                Log.error (CommandHandler.class.getSimpleName (), content);
                connection.sendMessage (error);
        }
    }
    
    private static void runSelectProgress (StringTokenizer tokens, AppMessage message,
            AppConnection connection) {
        Map <String, String> params = new HashMap <> ();
        params = Arguments.parse (params, tokens, null);
        
        if (!params.containsKey ("student")) {
            Message error = new ControlMessage (message, STC, ERROR, 0, 
                "Select progress failed, parameter missed: [student]");
            connection.sendMessage (error);
            return;
        }
        
        int studentID = Integer.parseInt (params.get ("student"));
        List <Trio <Integer, Integer, Boolean>> results 
            = OrganizationHistory.getProgress (studentID);
        Message list = new ListMessage <> (message, STC, results);
        connection.sendMessage (list);
    }
    
    private static void runSelectTasks (StringTokenizer tokens, AppMessage message,
            AppConnection connection) {
        Map <String, String> params = new HashMap <> ();
        params = Arguments.parse (params, tokens, null);
        
        if (!params.containsKey ("topic")) {
            Message error = new ControlMessage (message, STC, ERROR, 0, 
                "Select tasks failed, parameter missed: [topic]");
            connection.sendMessage (error);
            return;
        }
        
        int topicID = Integer.parseInt (params.get ("topic"));
        List <Pair <Integer, String>> tasks = OrganizationHistory.getTasks (topicID);
        Message list = new ListMessage <> (message, STC, tasks);
        connection.sendMessage (list);
    }
    
    private static void runSelectGroups (StringTokenizer tokens, AppMessage message,
            AppConnection connection) {
        Map <String, String> params = new HashMap <> ();
        params = Arguments.parse (params, tokens, null);
        
        if (params.containsKey ("id")) {
            int studentID = Integer.parseInt (params.get ("id"));
            List <Integer> groups = OrganizationHistory.getStudentGroups (studentID);
            Message list = new ListMessage <> (message, STC, groups);
            connection.sendMessage (list);
        } else {
            List <Integer> groups = OrganizationHistory.getGroups (params);
            Message list = new ListMessage <> (message, STC, groups);
            connection.sendMessage (list);
        }
    }
    
    private static void runSelectStudents (StringTokenizer tokens, AppMessage message,
            AppConnection connection) {
        Map <String, String> params = new HashMap <> ();
        params = Arguments.parse (params, tokens, null);
        
        List <Pair <Integer, Integer>> students = OrganizationHistory.getStudents (params);
        Message list = new ListMessage <> (message, STC, students);
        connection.sendMessage (list);
    }
    
    private static void runSelectTopics (StringTokenizer tokens, AppMessage message,
            AppConnection connection) {
        Map <String, String> params = new HashMap <> ();
        params = Arguments.parse (params, tokens, null);
        
        List <Pair <Integer, Integer>> topics = OrganizationHistory.getToics (params);
        Message list = new ListMessage <> (message, STC, topics);
        connection.sendMessage (list);
    }
    
    private static void runSelectInfo (StringTokenizer tokens, AppMessage message,
            AppConnection connection) {
        Map <String, String> params = new HashMap <> ();
        params = Arguments.parse (params, tokens, null);
        
        if (!params.containsKey ("about")) {
            Message error = new ControlMessage (message, STC, ERROR, 0, 
                "Select info failed, parameter missed: [about]");
            connection.sendMessage (error);
            return;
        } else if (!params.containsKey ("id")) {
            Message error = new ControlMessage (message, STC, ERROR, 0, 
                "Select info failed, parameter missed: [id]");
            connection.sendMessage (error);
            return;
        }
        
        String type = params.get ("about") + "s", id = params.get ("id");
        String query = "SELECT * FROM `%s` WHERE `id` = '%s' LIMIT 1";
        query = String.format (query, type, id);
        
        try {
            MySQLAdapter adapter = MySQLAdapter.getInstance ();
            Statement statement  = adapter.getDB ().createStatement ();
            ResultSet answer     = statement.executeQuery (query);
            if (answer.next ()) {
                int columns = answer.getMetaData ().getColumnCount ();
                List <String> values = new ArrayList <> ();
                for (int i = 0; i < columns; i++) {
                    values.add (answer.getString (i + 1));
                }
                
                Message info = new ListMessage <> (message, STC, values);
                connection.sendMessage (info);
            } else {
                Message error = new ControlMessage (message, STC, ERROR, 0, 
                    "Select info failed, no such id " + id + " for " + type);
                connection.sendMessage (error);
            }
        } catch (SQLException sqle) {
            Log.error (CommandHandler.class.getSimpleName (), sqle);
            Message error = new ControlMessage (message, STC, ERROR, 0, 
                "Select info failed for " + type + ":\n" + sqle);
            connection.sendMessage (error);
        }
    }
    
}
