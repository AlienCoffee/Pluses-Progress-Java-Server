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

	private final ConcurrentLinkedQueue <Socket> WAIT_HANDSHAKE = new ConcurrentLinkedQueue <> ();
	
	private final int THREADS_COUNT = 5;
	private final Set <Thread> THREADS = new HashSet <> ();
	private final Runnable TASK;
	
	
	public AbsSocketAcceptor (int port, int threads) throws IOException {
		this.LISTENER = new ServerSocket (port, 10);
		LISTENER.setSoTimeout (10000); // 10 seconds
		
		this.TASK = () -> {
			while (true) {
				try {
					Socket socket = LISTENER.accept();
					
					if (socket != null) {
						WAIT_HANDSHAKE.add (socket);
						System.out.println ("New connection accepted: " + socket);
						continue; 
					}
				} catch (SocketTimeoutException ste) {
					if (Thread.interrupted ()) {
						Thread.currentThread ().interrupt ();
						return; 
					}
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
				
				
				Socket socket = WAIT_HANDSHAKE.poll();
				if (socket != null) {
					String id = identifier();
					if (handshake(id, socket)) {
						onSocketReady(id, socket);
					}
				}
			}
		};
		
		synchronized (THREADS) {
			for (int i = 0; i < THREADS_COUNT; i++) {
				String name = "Connections-Acceptor-Thread-" + (i + 1);
				Thread thread = new Thread (TASK, name);
				THREADS.add(thread);
				thread.start ();
			}
		}
	}
	
	protected String identifier () {
		return RandomStringUtils.random (32, true, true);
	}
	
	@Override
	public void close () throws Exception {
		synchronized (THREADS) {
			for (Thread thread : THREADS) {
				if (thread == null) { continue; }
				
				thread.interrupt ();
			}
			
			for (Thread thread : THREADS) {
				if (thread == null) { continue; }
				
				try {
					thread.join (5000); // 5 seconds
					THREADS.remove (thread);
					System.out.println ("Thread " + thread.getName () + " closed");
				} catch (InterruptedException ie) {
					System.err.println ("Thread " + thread.getName () 
						+ " is not closed: " + ie.getMessage ());
				}
			}
		}
	}
	
}
