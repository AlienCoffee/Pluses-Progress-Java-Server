package ru.shemplo.pluses.network.pool;

import static ru.shemplo.pluses.network.message.AppMessage.MessageDirection.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Objects;

import ru.shemplo.pluses.log.Log;
import ru.shemplo.pluses.network.message.AppMessage;
import ru.shemplo.pluses.network.message.Message;
import ru.shemplo.pluses.network.message.PPMessage;
import ru.shemplo.pluses.network.message.PPMessage.Ping;
import ru.shemplo.pluses.util.BytesManip;

public class JavaAppConnection extends AbsConnection {
	
	public JavaAppConnection (String identifier, Socket socket) throws IOException {
		super (identifier, socket);
	}
	
	private volatile int reserved = -1;
	
	/**
	 * Must be guaranteed that this method works in single thread
	 * 
	 */
	@Override
	protected void _readStreamData () {
		long start = System.currentTimeMillis ();
		try {
			long time = start;
			while (time - start < 1 * 1000 && isConnected && IS.available () > 0) {
				if (reserved != -1 && IS.available () >= reserved) {
					byte [] buffer = new byte [reserved];
					IS.read (buffer, 0, buffer.length);
					
					// Assumed that full object transported
					InputStream is = new ByteArrayInputStream (buffer);
					ObjectInputStream ois = new ObjectInputStream (is);
					Object tmp = ois.readObject ();
					if (tmp instanceof AppMessage) {
						AppMessage message = (AppMessage) tmp;
						INPUT.add (message); // Adding to queue
					} else if (tmp instanceof PPMessage) {
					    PPMessage pong = (PPMessage) tmp;
					    if (Ping.PONG.equals (pong.VALUE)) {
					        active = time;
					    }
					}
					
					reserved = -1;
				} else if (reserved == -1 && IS.available () >= 4) {
					byte [] buffer = new byte [4];
					IS.read (buffer, 0, buffer.length);
					reserved = BytesManip.B2I (buffer);
				}
				
				time = System.currentTimeMillis ();
				isPending = false;
				active = time;
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
    public synchronized void sendMessage (Message message) {
	    // Can't send message: connection closed
        if (!isConnected ()) { return; }
        
        if (Objects.isNull (message)) {
            return; // Message is empty or has invalid direction
        } else if (message instanceof AppMessage) {
            AppMessage app = (AppMessage) message;
            if (!STC.equals (app.getDirection ())) {
                return;  // Message is empty or has invalid direction
            }
        }
        
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream ();
            ObjectOutputStream oos = new ObjectOutputStream (baos);
            oos.writeObject (message);
            oos.flush ();
            
            // Fetching serialized object to bytes array
            byte [] data   = baos.toByteArray ();
            byte [] length = BytesManip.I2B (data.length);
            
            OS.write (length);
            OS.write (data);
            OS.flush ();
        } catch (IOException ioe) {
            isConnected = false;
        }
    }

}
