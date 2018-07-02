package ru.shemplo.pluses.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.lang.RandomStringUtils;

import ru.shemplo.pluses.Run;
import ru.shemplo.pluses.log.Log;

public abstract class AbsSocketAcceptor implements Acceptor {

	private final ServerSocket LISTENER;
	private final Thread ACCEPTOR;

	private final ConcurrentLinkedQueue <Socket> WAIT_HANDSHAKE;
	private final Set <Thread> THREADS;
	private final Runnable TASK;
	
	public AbsSocketAcceptor (int port, int threads) throws IOException {
		this.LISTENER = new ServerSocket (port, 10);
		LISTENER.setSoTimeout (10000); // 10 seconds
		
		this.WAIT_HANDSHAKE = new ConcurrentLinkedQueue <> ();
		this.THREADS = new HashSet <> ();
		this.TASK = () -> {
			int tries = 0;
			while (Run.isRunning) {
				Socket socket = WAIT_HANDSHAKE.poll ();
				if (Objects.isNull (socket)) {
					try {
						Thread.sleep ((tries + 1) * 1000); // n * 10 seconds
						
						synchronized (THREADS) {
							if (WAIT_HANDSHAKE.size () == 0 
								&& THREADS.size () > 1 && tries >= 5) {
								// Thread has nothing to do -> remove it
								THREADS.remove (Thread.currentThread ());
								return;
							}
							
							tries += 1; // One more useless loop
							if (THREADS.size () == 1) {
								tries = 1;
							}
						}
					} catch (InterruptedException ie) {
						return;
					}
				} else {
					String identifier = identifier ();
					handshake (identifier, socket);
					
					// Socket processing is over
					onSocketReady (identifier, socket);
					
					// Keeping this thread alive
					tries = 0;
				}
			}
		};
		
		synchronized (THREADS) {
			Thread first = new Thread (TASK);
			THREADS.add (first);
			first.start ();
			
			Thread second = new Thread (TASK);
			THREADS.add (second);
			second.start ();
		}
		
		this.ACCEPTOR = new Thread (() -> {
			while (Run.isRunning) {
				try {
					// Trying to accept as much as possible
					// and then make a handshake...
					WAIT_HANDSHAKE.add (LISTENER.accept ());
					
					// Help threads can die and here is an
					// opportunity to add new if it's needed
					synchronized (THREADS) {
						if (WAIT_HANDSHAKE.size () > 3 
							&& THREADS.size () < threads) {
							THREADS.add (new Thread (TASK));
						}
					}
				} catch (SocketTimeoutException ste) {
					// nothing to do -> go to new loop
				} catch (IOException ioe) {
					if (LISTENER.isClosed () || !LISTENER.isBound ()) {
						String message = "Main acceptor thread is stoped";
						Log.error (getClass ().getSimpleName (), message);
						return;
					}
					
					try {
						Thread.sleep (1000);
					} catch (InterruptedException ie) {
						return;
					}
				}
			}
		});
		ACCEPTOR.setName ("Main-Acceptor-Thread");
		ACCEPTOR.start ();
	}
	
	protected String identifier () {
		return RandomStringUtils.random (32, true, true);
	}
	
	@Override
	public void close () throws Exception {
		
	}
	
}
