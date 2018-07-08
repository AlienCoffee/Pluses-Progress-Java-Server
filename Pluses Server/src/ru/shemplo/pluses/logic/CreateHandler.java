package ru.shemplo.pluses.logic;

import static ru.shemplo.pluses.network.message.AppMessage.MessageDirection.*;
import static ru.shemplo.pluses.network.message.ControlMessage.ControlType.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

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
            case "CLASS":
                runCreateClass (tokens, message, connection);
                break;
            
            default:
                String content = "Failed to create `" + type + "` (unknown type)";
                AppMessage error = new ControlMessage (STC, ERROR, 0, content);
                Log.error (CommandHandler.class.getSimpleName (), content);
                connection.sendMessage (error);
        }
    }
    
    private static final Set <String> CLASS_COLUMNS = new HashSet <> (
        Arrays.asList ("title", "comment", "created", "active", "headteacher")
    );
    
    private static void runCreateClass (StringTokenizer tokens, AppMessage message,
            AppConnection connection) {
        List <String> args = new ArrayList <> ();
        while (tokens.hasMoreTokens ()) {
            args.add (tokens.nextToken ());
        }
        
        if (args.size () < 1) {
            Message error = new ControlMessage (STC, ERROR, 0, 
                "Missed argument: title of new class");
            connection.sendMessage (error);
            return;
        }
        
        String datetime = new SimpleDateFormat ("YYYY-LL-dd HH:mm:ss")
            .format (new Date (System.currentTimeMillis ()));
        
        Map <String, String> params = new HashMap <> ();
        params.put ("created", datetime);
        params.put ("title", args.size () == 1 ? args.get (0) : null);
        if (args.size () > 1) {
            params = Arguments.parse (params, args);  
        }
        
        String title = params.get ("title");
        if (Objects.isNull (title) || title.length () == 0) {
            Message error = new ControlMessage (STC, ERROR, 0, 
                "Title of new class can't be empty");
            connection.sendMessage (error);
            return;
        }
        
        Set <String> paramKeys = params.keySet ();
        paramKeys.retainAll (CLASS_COLUMNS);
        
        List <String> paramLine = new ArrayList <> (paramKeys),
            valueLine = paramLine.stream ()
                .map (params::get).collect (Collectors.toList ());
        String paramArray = SQLUtil.toArray (paramLine, '`'),
            valueArray = SQLUtil.toArray (valueLine, '\'');
        
        MySQLAdapter adapter = MySQLAdapter.getInstance ();
        String queryTemplate = "INSERT INTO `classes` %s VALUES %s";
        String query = String.format (queryTemplate, paramArray, valueArray);
        try {
            PreparedStatement statement = adapter.getDB ()
                .prepareStatement (query, Statement.RETURN_GENERATED_KEYS);
            if (statement.executeUpdate () == 1) {
                ResultSet keys = statement.getGeneratedKeys ();
                if (!keys.next ()) { 
                    Message error = new ControlMessage (STC, ERROR, 0, 
                        "Class created successfully but `id` is unknown");
                    connection.sendMessage (error);
                    return;
                }
                
                int id = keys.getInt (1);
                Message success = new ControlMessage (STC, ID, id, "");
                connection.sendMessage (success);
            } else {
                Message error = new ControlMessage (STC, ERROR, 0, 
                    "Class wasn't created by unknown reason");
                connection.sendMessage (error);
                return;
            }
        } catch (SQLException sqle) {
            Log.error (CommandHandler.class.getSimpleName (), sqle);
        }
    }
    
}
