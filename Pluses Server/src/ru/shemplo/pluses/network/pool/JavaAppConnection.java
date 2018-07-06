package ru.shemplo.pluses.network.pool;

import static ru.shemplo.pluses.network.message.AppMessage.MessageDirection.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import ru.shemplo.pluses.log.Log;
import ru.shemplo.pluses.network.message.AppMessage;
import ru.shemplo.pluses.network.message.JavaAppMessage;
import ru.shemplo.pluses.network.message.Message;
import ru.shemplo.pluses.util.json.BytesManip;

public class JavaAppConnection implements AppConnection {

	private final ConcurrentLinkedQueue <JavaAppMessage> 
		INPUT = new ConcurrentLinkedQueue <> ();
	private final String IDENTIFIER;
	
	private final OutputStream OS;
	private final InputStream IS;
	private final Socket SOCKET;
	
	private AtomicLong updated = new AtomicLong ();
	private volatile boolean isConnected = true;
	private long actived = Long.MAX_VALUE;
	
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
	public JavaAppMessage getInput () {
		JavaAppMessage message = INPUT.poll ();
		if (Objects.isNull (message)) {
			return null;
		}
		
		return message;
	}

	@Override
	public void update () {
		// Nothing to update: connection closed
		if (!isConnected ()) { return; }
		
		long now = System.currentTimeMillis (), prev = getLastUpdated ();
		long max = Long.MAX_VALUE; // This is necessary to prevent the
		// situation when update would take more time than impulse period
		if (now - prev > 3 * 1000 && updated.compareAndSet (prev, max)) {
			// This section is available for one thread only, that's why
			// here can be called method below
			_readStreamData ();
			
			// Finishing updating
			updated.compareAndSet (max, now);
		}
	}
	
	private volatile int reserved = -1;
	
	/**
	 * Must be guaranteed that this method works in single thread
	 * 
	 */
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
					if (tmp instanceof JavaAppMessage) {
						JavaAppMessage message = (JavaAppMessage) tmp;
						INPUT.add (message); // Adding to queue
					}
					
					reserved = -1;
				} else if (reserved == -1 && IS.available () > 4) {
					byte [] buffer = new byte [4];
					IS.read (buffer, 0, buffer.length);
					reserved = BytesManip.B2I (buffer);
				}
				
				time = System.currentTimeMillis ();
				actived = time;
			}
		} catch (Exception es) {
			Log.error (JavaAppConnection.class.getSimpleName (), 
				"In connection " + IDENTIFIER + " something went wrong:\n" + es);
			try {
				// Something failed in connection
				// Closing this connection
				OS.close (); IS.close ();
				SOCKET.close ();
				
				// This done not throw `close` method
				// to prevent endless recursion
			} catch (IOException ioe) {} finally {
				// Mark connection as dropped
				isConnected = false;
			}
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
    public void sendMessage (Message message) {
	    // Can't send message: connection closed
        if (!isConnected ()) { return; }
        
        if (Objects.isNull (message)) {
            if (message instanceof AppMessage) {
                AppMessage app = (AppMessage) message;
                if (!SERVER_TO_CLIENT.equals (app.getDirection ())) {
                    return;  // Message is empty or has invalid direction
                }
            }
            
            return; // Message is empty or has invalid direction
        }
        
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream ();
            ObjectOutputStream oos = new ObjectOutputStream (baos);
            oos.writeObject (message);
            oos.flush ();
            
            // Fetching serialized object to bytes array
            byte [] data = baos.toByteArray ();
            byte [] length = { // 4 bytes of length of object (length in bytes)
                (byte) (data.length >> 24 & 0xff), // 31 30 29 28 27 26 25 24
                (byte) (data.length >> 16 & 0xff), // 23 22 21 20 19 18 17 16
                (byte) (data.length >> 8  & 0xff), // 15 14 13 12 11 10 9  8
                (byte) (data.length       & 0xff)  // 7  6  5  4  3  2  1  0
            };
            
            OS.write (length);
            OS.write (data);
            OS.flush ();
        } catch (IOException ioe) {
            
        }
    }

	@Override
	public synchronized void close () throws Exception {
		_readStreamData (); // Reading to prevent lose of data
		// Mark connection as dropped
		isConnected = false;
		
		// Finishing closing of connection
		OS.close (); IS.close ();
		SOCKET.close ();
	}

}
