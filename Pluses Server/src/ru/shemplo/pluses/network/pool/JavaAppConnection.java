package ru.shemplo.pluses.network.pool;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import ru.shemplo.pluses.network.message.JavaMessage;

public class JavaAppConnection implements AppConnection {

	private final ConcurrentLinkedQueue <JavaMessage> 
		INPUT = new ConcurrentLinkedQueue <> ();
	private final String IDENTIFIER;
	
	private final OutputStream OS;
	private final InputStream IS;
	private final Socket SOCKET;
	
	private AtomicLong updated = new AtomicLong ();
	private boolean isConnected = true;
	private long actived = 0;
	
	public JavaAppConnection (String identifier, Socket socket) throws IOException {
		this.OS = socket.getOutputStream ();
		this.IS = socket.getInputStream ();
		this.IDENTIFIER = identifier;
		this.SOCKET = socket;
	}
	
	@Override
	public String getIdentifier () {
		return IDENTIFIER;
	}

	@Override
	public boolean isConnected () {
		return this.isConnected;
	}

	@Override
	public int getInputSize () {
		return INPUT.size ();
	}

	@Override
	public String [] getInput () {
		return null;
	}

	@Override
	public void update () {
		long now = System.currentTimeMillis (), prev = getLastUpdated ();
		long max = Long.MAX_VALUE; // This is necessary to prevent the
		// situation when update would take more time than impulse period
		if (now - prev > 5 * 1000 && updated.compareAndSet (prev, max)) {
			_readStreamData ();
		}
	}
	
	private void _readStreamData () {
		try {
			ObjectInputStream bis = new ObjectInputStream (IS);
			Object tmp = null;
			
			while ((tmp = bis.readObject ()) != null) {
				if (!(tmp instanceof JavaMessage)) {
					continue; // Some strange object
				}
				
				JavaMessage message = (JavaMessage) tmp;
				System.out.println (message.DIRECTION);
				System.out.println (message.CONTENT);
				// INPUT.add (message);
			}
		} catch (IOException ioe) {
			isConnected = false;
		} catch (ClassNotFoundException e) {
			isConnected = false;
		}
	}

	@Override
	public long getLastUpdated () {
		return updated.get ();
	}

	@Override
	public long getLastActivity () {
		return actived;
	}

	@Override
	public synchronized void close () throws Exception {
		_readStreamData (); // Reading to prevent lose of data
		OS.close (); IS.close ();	
		SOCKET.close ();
	}

}
