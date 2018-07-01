package ru.shemplo.pluses.stub;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

public class Run {

	private static final ConcurrentMap <String, Socket> 
		SOCKETS = new ConcurrentHashMap <> ();
	
	private static final ConcurrentLinkedQueue <String>
		WAIT_HANDSHAKE = new ConcurrentLinkedQueue <> ();
	private static final ConcurrentLinkedQueue <String>
		WAIT_REMOVED = new ConcurrentLinkedQueue <> ();
	
	private static boolean isRunning = true;
	
	public static void main (String ... args) throws IOException {
		@SuppressWarnings ("resource")
		ServerSocket sever = new ServerSocket (1999);
		System.out.println ("Server started");
		
		Thread acceptor = new Thread (() -> {
			Socket income = null;
			int index = 0;
			
			while (isRunning) {
				try {
					System.out.println ("Waiting for new one");
					income = sever.accept ();
				} catch (IOException ioe) {
					ioe.printStackTrace ();
				}
				
				if (Objects.isNull (income)) {
					System.out.println ("Fatal stop");
					System.exit (0);
					return;
				}
				
				String name = "connection #" + (index++);
				SOCKETS.put (name, income);
				WAIT_HANDSHAKE.add (name);
				
				System.out.println ("Added to sockets: " + name);
			}
		}, "Acceptor-Thread");
		acceptor.start ();
		
		Thread intelligence = new Thread (() -> {
			while (isRunning) {
				while (!WAIT_HANDSHAKE.isEmpty ()) {
					String name = WAIT_HANDSHAKE.poll ();
					Socket wait = SOCKETS.get (name);
					makeHandshake (name, wait);
				}
			}
		}, "Intelligence-Thread");
		intelligence.start ();
	}
	
	private static void makeHandshake (String name, Socket socket) {
		System.out.println ("Starting handshake with " + name);
		
		try {
			InputStream is = socket.getInputStream ();
			int length = is.available ();
			
			byte [] buffer = new byte [length];
			is.read (buffer, 0, length);
			
			String text = new String (buffer, 0, length);
			
			final String KEY_WORD = "Sec-WebSocket-Key";
			int anchor = text.indexOf (KEY_WORD) + KEY_WORD.length () + 1;
			int keyBound = text.indexOf ('\n', anchor);
			String key = text.substring (anchor, keyBound).trim ();
			key += "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
			
			MessageDigest digest = MessageDigest.getInstance ("SHA-1");
			digest.update (key.getBytes ()); // Updating hash
			String base = Base64.getEncoder ()
						  .encodeToString (digest.digest ());
			
			StringBuilder answer = new StringBuilder ();
			answer.append ("HTTP/1.1 101 Switching Protocols\n");
			answer.append ("Upgrade: websocket\n");
			answer.append ("Connection: Upgrade\n");
			answer.append ("Sec-WebSocket-Accept: ");
			answer.append (base);
			answer.append ("\n");
			answer.append ("\n");
			
			OutputStream os = socket.getOutputStream ();
			os.write (answer.toString ().getBytes ());
			os.flush ();
			
			System.out.println ("Handshake done for " + name);
		} catch (IOException | NoSuchAlgorithmException es) {
			WAIT_REMOVED.add (name);
		}
	}
	
}
