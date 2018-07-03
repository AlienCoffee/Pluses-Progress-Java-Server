package ru.shemplo.pluses.network.pool;

import java.util.HashSet;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import ru.shemplo.pluses.Run;

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
						
						String [] input = null;
						// Handling all not processed inputs
						while ((input = connection.getInput ()) != null) {
							System.out.println (input [0]);
						}
						
						continue; // Connection died
					}
					
					tasks += connection.getInputSize ();
					String [] input = connection.getInput ();
					if (!Objects.isNull (input)) {
						System.out.println (input [0]);
					}
				}
				
				if (tasks == 0) {
					try {
						Random r = Run.RANDOM;
						// Sleeping because nothing to do now
						Thread.sleep (10000 + r.nextInt (100));
						System.out.println ("Connections: " + CONNECTIONS.size ());
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
