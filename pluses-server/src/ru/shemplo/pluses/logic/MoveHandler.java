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

public class MoveHandler {
    
    public static void runMove (StringTokenizer tokens, AppMessage message,
            AppConnection connection) {
        if (!tokens.hasMoreTokens ()) { return; }
        String type = tokens.nextToken ().toUpperCase ();
        switch (type) {
            case "STUDENT":
                runMoveStudent (tokens, message, connection);
                break;
            
            default:
                String content = "Failed to move `" + type + "` (unknown type)";
                AppMessage error = new ControlMessage (STC, ERROR, 0, content);
                Log.error (CommandHandler.class.getSimpleName (), content);
                connection.sendMessage (error);
        }
    }
    
    private static final Set <String> MOVE_STUDENT_PARAMS = new HashSet <> (
        Arrays.asList ("from", "to", "id")
    );
    
    private static void runMoveStudent (StringTokenizer tokens, AppMessage message,
            AppConnection connection) {
        Map <String, String> params = new HashMap <> ();
        params = Arguments.parse (params, tokens, null);
        
        for (String name : MOVE_STUDENT_PARAMS) {
            if (params.containsKey (name)) { continue; }
            Message error = new ControlMessage (message, STC, ERROR, 0, 
                "Move student failed, parameter missed: [" + name + "]");
            connection.sendMessage (error);
            return;
        }
        
        int studentID = Integer.parseInt (params.get ("id"));
        int from = Integer.parseInt (params.get ("from"));
        int to   = Integer.parseInt (params.get ("to"));
        
        Timestamp time = new Timestamp (System.currentTimeMillis ());
        params.put ("time", SQLUtil.getDatetime (time.getTime ()));
        params.put ("student", "" + studentID);
        
        try {
            OrganizationHistory.moveStudent (studentID, from, to, time);
            
            MySQLAdapter adapter = MySQLAdapter.getInstance ();
            Statement statement  = adapter.getDB ().createStatement ();
            
            String query = SQLUtil.makeInsertQuery ("movements", params);
            statement.execute (query);
        } catch (SQLException | IllegalStateException es) {
            Log.error (CommandHandler.class.getSimpleName (), es);
            Message error = new ControlMessage (message, STC, ERROR, 0, 
                "Move student failed for " + studentID + ":\n" + es);
            connection.sendMessage (error);
        }
    }
    
}