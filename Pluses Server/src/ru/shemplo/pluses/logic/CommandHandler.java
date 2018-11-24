package ru.shemplo.pluses.logic;

import static ru.shemplo.pluses.network.message.AppMessage.MessageDirection.*;
import static ru.shemplo.pluses.network.message.ControlMessage.ControlType.*;

import java.util.Objects;
import java.util.StringTokenizer;
import java.util.function.Consumer;

import ru.shemplo.pluses.Run;
import ru.shemplo.pluses.log.Log;
import ru.shemplo.pluses.network.message.CommandMessage;
import ru.shemplo.pluses.network.message.ControlMessage;
import ru.shemplo.pluses.network.message.Message;
import ru.shemplo.pluses.network.pool.AppConnection;
import ru.shemplo.pluses.struct.GroupsService;

public class CommandHandler {
    
    // XXX: non-static is impossible (try to fix this)
    private static GroupsService groupsService;
    
    @SuppressWarnings ("unused")
    private enum RouteSection implements RoutableSection {
        
        CREATE_GROUP   (groupsService::createGroup),
        CREATE_STUDENT (__ -> {}),
        CREATE_TASK    (__ -> {}),
        CREATE_TOPIC   (__ -> {}),
        
        CREATE (
            CREATE_GROUP, CREATE_STUDENT, CREATE_TASK, CREATE_TOPIC
        );
        
        private RouteSection () {
            
        }
        
        private RouteSection (RouteSection ... sections) {
            
        }
        
        private RouteSection (Consumer <RoutableBundle> consumer) {
            
        }

        @Override
        public void route (RoutableBundle bundle) {
            
        }
        
    }
    
	public static void run (CommandMessage mes, AppConnection connection) {
		if (Objects.isNull (mes) || !CTS.equals (mes.getDirection ())) { 
			return; // Message is empty or has invalid direction
		}
		
		StringTokenizer st = new StringTokenizer (mes.getCommand ());
		if (!st.hasMoreTokens ()) { return; /* Nothing to run */ }
		String command = st.nextToken ().toUpperCase ();
		try {
		    switch (command) {
	            case "CREATE": // command to create something
	                CreateHandler.runCreate (st, mes, connection);
	                break;
	            
	            case "INSERT": // command to add something
	                InsertHandler.runInsert (st, mes, connection);
	                break;
	                
	            case "SELECT": // command to get something
	                SelectHandler.runSelect (st, mes, connection);
	                break;
	                
	            case "MOVE":   // command to move something
	                MoveHandler.runMove (st, mes, connection);
	                break;
	                
	            case "UPDATE": // command to update something
	                UpdateHandler.runCreate (st, mes, connection);
	                break;
	                
	            case "DELETE":
	                break;
	                
	            case "EXIT":
	            case "QUIT":
	                try    { connection.close (); } 
	                catch (Exception e) { /**/ }
	                break;
	                
	            case "PING":
	                Message pong = new ControlMessage (STC, INFO, 0, "PONG");
	                connection.sendMessage (pong);
	                break;
	                
	            case "HELP":
	                // XXX: it's not optimized at all (don't watch on this crap)
	                String commandsHelp = "Available commands: \n"
	                    + "\n"
	                    + "How to understand commands bellow:\n"
	                    + "* first word       - command key word\n"
	                    + "* second (as rule) - type of affecting object\n"
	                    + "* -[some text]     - parameter name\n"
	                    + "* -?[some text]    - optional parameter name\n"
	                    + "\n"
	                    + "Value of parameter is substring that is between \n"
	                    + "two parameters' names (or end of line)\n"
	                    + "\n"
	                    + "In brackets after parameter name his type\n"
	                    + "S - string, I - integer, D - datetime\n"
	                    + "\n"
	                    + "[section CREATE]\n"
	                    + "1. create group -title(S) -?comment(S) -?created(D)\n"
	                    + "2. create student -name.first(S)\n"
	                    + "3. create topic -title(S) -?comment(S) -?author(I)\n"
	                    + "4. create task -topic(I) -title(S)\n"
	                    + "\n"
	                    + "[section INSERT]\n"
	                    + "1. insert student -group(I) -student(I)\n"
	                    + "2. insert topic -group(I) -topic(I)\n"
	                    + "3. insert try -student(I) -group(I) -topic(I) -task(I) -verdict(I) -teacher(I)\n"
	                    + "\n"
	                    + "[section SELECT]\n"
	                    + "1. select groups -?id(I)\n"
	                    + "2. select info -about(S) -id(I)\n"
	                    + "3. select topics -?id(I)\n"
	                    + "4. select students -?id(I)\n"
	                    + "5. select tasks -topic(I)\n"
	                    + "\n"
	                    + "[section MOVE]\n"
	                    + "1. move student -from(I) -to(I) -id(I)\n"
	                    + "\n"
	                    + "[section UPDATE]\n"
	                    + "1. update task -topic(I) -id(I) -title(S)\n"
	                    + "\n"
	                    + "Also available commands: exit, ping, stop, help\n";
	                Message help = new ControlMessage (STC, INFO, 0, commandsHelp);
	                connection.sendMessage (help);
	                break;
	                
	            case "STOP":
	                Run.stopApplication (0, "Command from client");
	                break;
	                
	            default:
	                String content = "Command `" + command + "` can't be run (unknown command)";
	                Message error = new ControlMessage (STC, ERROR, 0, content);
	                Log.error (CommandHandler.class.getSimpleName (), content);
	                connection.sendMessage (error);
	        }
		// Handling all possible exceptions and sending error message to client
		} catch (Exception e) {
		    Message error = new ControlMessage (mes, STC, ERROR, 0, 
		        "Unhandled (unexpected) error:\n" + e);
		    connection.sendMessage (error);
		}
	}
	
}
