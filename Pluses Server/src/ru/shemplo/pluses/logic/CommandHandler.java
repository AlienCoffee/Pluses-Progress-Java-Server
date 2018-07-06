package ru.shemplo.pluses.logic;

import static ru.shemplo.pluses.network.message.AppMessage.MessageDirection.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;

import ru.shemplo.pluses.log.Log;
import ru.shemplo.pluses.network.message.AppMessage;
import ru.shemplo.pluses.network.message.JavaAppError;
import ru.shemplo.pluses.network.message.JavaAppMessage;
import ru.shemplo.pluses.network.message.Message;
import ru.shemplo.pluses.network.pool.AppConnection;
import ru.shemplo.pluses.struct.OrganizationTree;

public class CommandHandler {
	
    // IMPORTANT // This is temporal solution // It will be removed in future
    private static final OrganizationTree TREE = new OrganizationTree ();
    
	public static void run (AppMessage mes, AppConnection connect) {
		if (Objects.isNull (mes) || !CLIENT_TO_SERVER.equals (mes.getDirection ())) { 
			return; // Message is empty or has invalid direction
		}
		
		StringTokenizer st = new StringTokenizer (mes.getContent ());
		if (!st.hasMoreTokens ()) { return; /* Nothing to run */ }
		String command = st.nextToken ().toUpperCase ();
		switch (command) {
			case "INSERT": // command to add something
				runInsert (st, mes, connect);
				break;
				
			case "DELETE":
			    break;
				
			default:
			    String content = "Command `" + command + "` can't be run (unknown command)";
				Message error = new JavaAppError (SERVER_TO_CLIENT, content);
				Log.error (CommandHandler.class.getSimpleName (), content);
				connect.sendMessage (error);
		}
	}
	
	private static void runInsert (StringTokenizer tokens, AppMessage message, 
			AppConnection connection) {
	    if (!tokens.hasMoreTokens ()) { return; }
		String type = tokens.nextToken ().toUpperCase ();
		switch (type) {
		    case "CLASS":
		        runInsertClass (tokens, message, connection);
		        break;
		    
		    default:
		        String content = "Failed to insert `" + type + "` (unknown type)";
                AppMessage error = new JavaAppError (SERVER_TO_CLIENT, content);
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
	    
	    // TODO : here must be check that ids are valid
	    
	    TREE.getOrganization ().commitAll (ids);
	    System.out.println (TREE.toString ());
	    
	    Message accept = new JavaAppMessage (SERVER_TO_CLIENT, "ok");
	    connection.sendMessage (accept);
	}
	
}
