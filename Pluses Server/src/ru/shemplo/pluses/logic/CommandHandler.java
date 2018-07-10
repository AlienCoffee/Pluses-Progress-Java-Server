package ru.shemplo.pluses.logic;

import static ru.shemplo.pluses.network.message.AppMessage.MessageDirection.*;
import static ru.shemplo.pluses.network.message.ControlMessage.ControlType.*;

import java.util.Objects;
import java.util.StringTokenizer;

import ru.shemplo.pluses.Run;
import ru.shemplo.pluses.log.Log;
import ru.shemplo.pluses.network.message.CommandMessage;
import ru.shemplo.pluses.network.message.ControlMessage;
import ru.shemplo.pluses.network.message.Message;
import ru.shemplo.pluses.network.pool.AppConnection;

public class CommandHandler {
    
	public static void run (CommandMessage mes, AppConnection connect) {
		if (Objects.isNull (mes) || !CTS.equals (mes.getDirection ())) { 
			return; // Message is empty or has invalid direction
		}
		
		StringTokenizer st = new StringTokenizer (mes.getCommand ());
		if (!st.hasMoreTokens ()) { return; /* Nothing to run */ }
		String command = st.nextToken ().toUpperCase ();
		try {
		    switch (command) {
	            case "CREATE": // command to create something
	                CreateHandler.runCreate (st, mes, connect);
	                break;
	            
	            case "INSERT": // command to add something
	                InsertHandler.runInsert (st, mes, connect);
	                break;
	                
	            case "DELETE":
	                break;
	                
	            case "EXIT":
	            case "QUIT":
	                try    { connect.close (); } 
	                catch (Exception e) { /**/ }
	                break;
	                
	            case "PING":
	                Message pong = new ControlMessage (STC, INFO, 0, "PONG");
	                connect.sendMessage (pong);
	                break;
	                
	            case "STOP":
	                Run.stopApplication (0, "Command from client");
	                break;
	                
	            default:
	                String content = "Command `" + command + "` can't be run (unknown command)";
	                Message error = new ControlMessage (STC, ERROR, 0, content);
	                Log.error (CommandHandler.class.getSimpleName (), content);
	                connect.sendMessage (error);
	        }
		// Handling all possible exceptions and sending error message to client
		} catch (Exception e) {
		    Message error = new ControlMessage (mes, STC, ERROR, 0, 
		        "Unhandled (unexpected) error:\n" + e);
		    connect.sendMessage (error);
		}
	}
	
}
