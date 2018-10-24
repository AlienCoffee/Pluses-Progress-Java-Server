package ru.shemplo.pluses.network;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import javafx.util.Pair;

import org.apache.commons.lang.RandomStringUtils;

import ru.shemplo.pluses.log.Log;

public abstract class AbsSocketAcceptor implements Acceptor {
	private final int ACCEPTOR_THREADS_COUNT = 1;
	private final int HANDSHAKE_THREADS_COUNT = 1;
	private final int SERVER_TIMEOUT = 1000;
	private final int HANDSHAKE_TIMEOUT = 10000;
	
	private final ServerSocket LISTENER;
	private final LinkedBlockingQueue <Pair<Socket, Long>> WAIT_HANDSHAKE = new LinkedBlockingQueue <> ();

	private final Set <Thread> THREADS = new HashSet <> ();
	private final Runnable ACCEPTOR_TASK, HANDSHAKE_TASK;
	
	public AbsSocketAcceptor (int port, int threads) throws IOException {
		this.LISTENER = new ServerSocket (port, 10);
		LISTENER.setSoTimeout (SERVER_TIMEOUT); // 10 seconds
		
		this.ACCEPTOR_TASK = () -> {
			while (true) {
				try {
					Socket socket = LISTENER.accept();
					
					if (socket != null) {
						WAIT_HANDSHAKE.add (new Pair<>(socket, null));
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
			}
		};
		
		this.HANDSHAKE_TASK = () -> {
			while (true) {
				Pair<Socket, Long> entry = null;
				try {
					entry = WAIT_HANDSHAKE.take();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				if (entry == null) continue;
				
				Socket socket = entry.getKey();
				try {
					String hash = "" + socket.hashCode();
					if (handshake(hash, socket)) {
						//Handshake completed
						onSocketReady(hash, socket);
					} else {
						//not yet completed
						long time = entry.getValue() != null ? entry.getValue() : System.nanoTime();
						long currentTime = System.nanoTime(), 
							 overTime = (currentTime - time) / 1_000_000;
						
						if (overTime < HANDSHAKE_TIMEOUT) {
							//we can wait
							WAIT_HANDSHAKE.add(new Pair<> (socket, time));
						} else {
							//Timeout exceeded
							throw new SocketTimeoutException ("Handshake not finished");
						}
					}
					
				} catch (IOException ioe) {
					//Handshake failed for some reason, dropping
					try {
						socket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		};
		
		
		for (int i = 0; i < ACCEPTOR_THREADS_COUNT; i++) {
			String name = "Connections-Acceptor-Thread-" + (i + 1);
			addThread(ACCEPTOR_TASK, name);
		}
		
		for (int i = 0; i < HANDSHAKE_THREADS_COUNT; i++) {
			String name = "Connections-Handshake-Thread-" + (i + 1);
			addThread(HANDSHAKE_TASK, name);
		}
	}
	
	private void addThread(Runnable task, String name) {
		synchronized (THREADS) {
			Thread thread = new Thread (task, name);
			THREADS.add(thread);
			thread.start ();
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
		
		while (!WAIT_HANDSHAKE.isEmpty()) {
			Pair<Socket, Long> entry = WAIT_HANDSHAKE.poll();
			if (entry == null) return;
			
			Socket socket = entry.getKey();
			
			OutputStreamWriter osw = new OutputStreamWriter(socket.getOutputStream(), "UTF-8");
	        osw.write("0", 0, 1);
			socket.close();
		}
	}
	
}
