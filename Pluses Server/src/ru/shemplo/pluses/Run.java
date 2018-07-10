package ru.shemplo.pluses;

import static java.util.Objects.*;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Random;

import ru.shemplo.pluses.config.Configuration;
import ru.shemplo.pluses.db.MySQLAdapter;
import ru.shemplo.pluses.log.Log;
import ru.shemplo.pluses.network.Acceptor;
import ru.shemplo.pluses.network.JavaSocketAcceptor;
import ru.shemplo.pluses.network.RawSocketAcceptor;
import ru.shemplo.pluses.network.pool.ConnectionPool;
import ru.shemplo.pluses.struct.OrganizationHistory;

public class Run {

	public static final String CONFIG_NAME = "config.ini";
	
	public static final Random RANDOM = new Random ();
	
	public static final File RUN_DIRECTORY;
	static {
		RUN_DIRECTORY = new File (".").getAbsoluteFile ().getParentFile ();
		System.out.println ("[INFO][RUN] Server started in " + RUN_DIRECTORY);
	}
	
	private static boolean isRunning = true;
	public static int exitCode = 1;
	
	private static final Acceptor [] ACCEPTORS;
	static {
		ACCEPTORS = new Acceptor [2];
	}
	
	public static void main (String ... args) {
		// Loading configuration from file
	    Configuration.load (CONFIG_NAME);
		
		// Initialization of connection pool
		ConnectionPool.getInstance ();
		// Initializing of MySQL driver
		MySQLAdapter.getInstance ();
		// Initializing history structure
		OrganizationHistory.init ();
		
		////////////////////////////////////
		Log.log (Run.class.getSimpleName (), 
		    "Initialization of necessary components completed");
		////////////////////////////////////////////////////////
		try {
		    ACCEPTORS [0] = new JavaSocketAcceptor (1999, 2);
			ACCEPTORS [1] = new RawSocketAcceptor (2000, 2);
		} catch (IOException ioe) {
			String message = "Failed to initialize acceptor due to:\n" + ioe;
			Log.error (Run.class.getSimpleName (), message);
		}
		
		Log.log (Run.class.getSimpleName (), "Server successfully started :)");
	}
	
	public static void stopApplication (int code, String comment) {
		isRunning = false;
		
		// Stopping server in special thread to avoid deadlock
		// in case when processing thread will wail itself in
		// `Thread.join ()` method
		//
		// This process isn't instant, it takes about 5 seconds
		Thread stop = new Thread (() -> {
		    Log.log (Run.class.getSimpleName (), "Stopping server...");
	        
	        if (code == 0) {
	            String message = isNull (comment) || comment.length () == 0
	                             ? "Application stopped normally"
	                             : "Normal stop: " + comment;
	            System.out.println (message);
	        } else {
	            String message = isNull (comment) || comment.length () == 0
	                             ? "Application stopped with uncommented reason"
	                             : "Fatal stop (" + code + "): " + comment;
	            System.out.flush ();
	            System.err.println (message);
	            System.err.flush ();
	        }
	        
		    try {
		        // Stopping server in accepting new sockets
	            for (int i = 0; i < ACCEPTORS.length; i++) {
	                Acceptor acceptor = ACCEPTORS [i];
	                if (!Objects.isNull (acceptor)) {
	                    acceptor.close ();
	                }
	            }
	            
	            // Stopping server in processing commands
	            ConnectionPool.getInstance ().close ();
	            // Closing all file descriptors
	            OrganizationHistory.close ();
	            // Closing connection with database
	            MySQLAdapter.getInstance ().close ();
	            // Stopping server in logging events
	            Log.close ();
	        } catch (Exception e) {}
		    
		    System.out.println ("Server stopped");
		    // Exit from application with code
	        System.exit (code);
		});
		stop.start ();
	}
	
	public static boolean isRunning () {
	    return isRunning;
	}
	
}
