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
import ru.shemplo.pluses.network.pool.ConnectionPool;

public class Run {

	public static final String CONFIG_NAME = "config.ini";
	
	public static final Random RANDOM = new Random ();
	
	public static final File RUN_DIRECTORY;
	static {
		RUN_DIRECTORY = new File (".").getAbsoluteFile ().getParentFile ();
		System.out.println ("[INFO][RUN] Server started in " + RUN_DIRECTORY);
	}
	
	public static boolean isRunning = true;
	public static int exitCode = 1;
	
	private static final Acceptor [] ACCEPTORS;
	static {
		ACCEPTORS = new Acceptor [2];
	}
	
	public static void main (String ... args) {
		Configuration.load (CONFIG_NAME);
		
		// Initialization of connection pool
		ConnectionPool.getInstance ();
		
		try {
			ACCEPTORS [0] = new JavaSocketAcceptor (1999, 2);
		} catch (IOException ioe) {
			String message = "Failed to initialize acceptor due to:\n" + ioe;
			Log.error (Run.class.getSimpleName (), message);
		}
		
		MySQLAdapter.getInstance ();
	}
	
	public static void stopApplication (int code, String comment) {
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
		
		isRunning = false;
		try {
			for (int i = 0; i < ACCEPTORS.length; i++) {
				Acceptor acceptor = ACCEPTORS [i];
				if (!Objects.isNull (acceptor)) {
					acceptor.close ();
				}
			}
			
			ConnectionPool.getInstance ().close ();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		Log.close ();
		System.exit (code);
	}
	
}
