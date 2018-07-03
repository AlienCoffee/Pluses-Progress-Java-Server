package ru.shemplo.pluses.network.pool;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import ru.shemplo.pluses.network.message.JavaMessage;
import ru.shemplo.pluses.util.json.BytesManip;

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
			System.out.println ("Update in " + Thread.currentThread ().getId ());
			_readStreamData ();
			
			// Finishing updating
			updated.compareAndSet (max, now);
		}
	}
	
	private volatile int reserved = -1;
	
	private void _readStreamData () {
		long start = System.currentTimeMillis ();
		try {
			long time = start;
			while (time - start < 1 * 1000 && IS.available () > 0) {
				if (reserved != -1 && IS.available () >= reserved) {
					byte [] buffer = new byte [reserved];
					IS.read (buffer, 0, buffer.length);
					
					// Assumed that full object transported
					InputStream is = new ByteArrayInputStream (buffer);
					ObjectInputStream ois = new ObjectInputStream (is);
					Object tmp = ois.readObject ();
					if (tmp instanceof JavaMessage) {
						JavaMessage message = (JavaMessage) tmp;
						System.out.println (message.CONTENT);
					}
					
					reserved = -1;
				} else if (reserved == -1 && IS.available () > 4) {
					byte [] buffer = new byte [4];
					IS.read (buffer, 0, buffer.length);
					reserved = BytesManip.B2I (buffer);
				}
				
				time = System.currentTimeMillis ();
			}
		} catch (IOException ioe) {
			isConnected = false;
		} catch (ClassNotFoundException cnfe) {
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
