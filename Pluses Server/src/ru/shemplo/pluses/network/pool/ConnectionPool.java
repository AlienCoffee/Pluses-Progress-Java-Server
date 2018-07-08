package ru.shemplo.pluses.network.pool;

import java.util.HashSet;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import ru.shemplo.pluses.Run;
import ru.shemplo.pluses.log.Log;
import ru.shemplo.pluses.logic.CommandHandler;
import ru.shemplo.pluses.network.message.AppMessage;

public class ConnectionPool implements AutoCloseable {

	private static ConnectionPool POOL;
	
	public static ConnectionPool getInstance () {
		if (Objects.isNull (POOL)) {
			synchronized (ConnectionPool.class) {
				if (Objects.isNull (POOL)) {
					POOL = new ConnectionPool (2);
				}
			}
		}
		
		return POOL;
	}
	
	private final ConcurrentMap <String, AppConnection> 
		CONNECTIONS = new ConcurrentHashMap <> ();
	
	private final Set <Thread> THREADS;
	private final Runnable TASK;
	
	private ConnectionPool (int threads) {
		this.THREADS = new HashSet <> ();
		this.TASK = () -> {
			while (Run.isRunning) {
				Set <String> keys = CONNECTIONS.keySet ();
				int tasks = 0;
				
				for (String key : keys) {
					AppConnection connection = CONNECTIONS.get (key);
					if (Objects.isNull (connection)) { continue; }
					
					connection.update ();
					if (!connection.isConnected ()) {
						CONNECTIONS.remove (key);
						
						AppMessage input = null;
						// Handling all not processed inputs
						while ((input = connection.getInput ()) != null) {
							CommandHandler.run (input, connection);
						}
						
						// Connection is wasting resources
                        try {
                            // Dropping connection
                            connection.close ();
                        } catch (Exception e) {}
						
						Log.log (ConnectionPool.class.getSimpleName (), 
						    "Connection " + connection.getIdentifier () + " closed");
						continue; // Connection died -> going to the next
					}
					
					tasks += connection.getInputSize ();
					AppMessage input = connection.getInput ();
					if (!Objects.isNull (input)) {
						CommandHandler.run (input, connection);
					}
				}
				
				if (tasks == 0) {
					try {
						Random r = Run.RANDOM;
						// Sleeping because nothing to do now
						Thread.sleep (250 + r.nextInt (100));
					} catch (InterruptedException ie) {
						return;
					}
				}
			}
		};
		
		for (int i = 0; i < threads; i++) {
			Thread thread = new Thread (TASK);
			THREADS.add (thread);
			thread.start ();
		}
	}
	
	public void registerConnection (AppConnection connection) {
		CONNECTIONS.put (connection.getIdentifier (), connection);
	}

	@Override
	public void close () throws Exception {
		
	}
	
}
