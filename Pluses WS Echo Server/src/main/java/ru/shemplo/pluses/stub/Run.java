package ru.shemplo.pluses.stub;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Objects;
import java.util.StringTokenizer;
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
	
	private static final File FILE = new File ("common.txt");
	
	private static boolean isRunning = true;
	
	public static void main (String ... args) throws IOException {
		@SuppressWarnings ("resource")
		ServerSocket sever = new ServerSocket (1999);
		System.out.println ("Server started");
		
		FILE.createNewFile ();
		
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
				
				for (String name : SOCKETS.keySet ()) {
					if (WAIT_HANDSHAKE.contains (name)
						|| WAIT_REMOVED.contains (name)) {
						continue;
					}
					
					Socket socket = SOCKETS.get (name);
					
					try {
						InputStream is = socket.getInputStream ();
						if (is.available () == 0) { continue; }
						
						OutputStream os = socket.getOutputStream ();
						handleInput (name, is, os);
					} catch (IOException ioe) {
						WAIT_REMOVED.add (name);
					}
				}
				
				while (!WAIT_REMOVED.isEmpty ()) {
					String name = WAIT_REMOVED.poll ();
					SOCKETS.remove (name);
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
	
	public static void handleInput (String name, InputStream is, OutputStream os) throws IOException {
		byte first = (byte) is.read ();
		@SuppressWarnings ("unused")
		int fin     = bit (first, 7);
		@SuppressWarnings ("unused")
		int [] rsvs = {bit (first, 6), bit (first, 5), bit (first, 4)};
		@SuppressWarnings ("unused")
		int opcode  = first & 0b1111;
		// first byte parsed
		
		byte second = (byte) is.read ();
		int mask    = bit (second, 7);
		long length = second & 0b01111111;
		// second byte parsed
		
		if (length == 126) {
			byte [] buffer = new byte [2];
			is.read (buffer, 0, buffer.length);
			
			length = (buffer [0] << 8) | buffer [1];
		} else if (length == 127) {
			byte [] buffer = new byte [8];
			is.read (buffer, 0, buffer.length);
			
			length = buffer [0];
			for (int i = 1; i < buffer.length; i++) {
				length = (length << 8) | buffer [i];
			}
		}
		
		byte [] maskA = new byte [4];
		if (mask == 1) {
			is.read (maskA, 0, maskA.length);
		}
		
		byte [] content = new byte [(int) length];
		for (int i = 0; i < content.length; i++) {
			content [i] = (byte) is.read ();
			if (mask == 1) { 
				content [i] ^= maskA [i % maskA.length]; 
			}
		}
		
		String message = new String (content, 0, content.length);
		StringTokenizer tokens = new StringTokenizer (message);
		String command = tokens.nextToken ();
		if ("exit".equals (command)) {
			System.out.println ("Stopping server by command");
			System.exit (0);
			return;
		} else if ("read".equals (command)) {
			try (
				InputStream read = new FileInputStream (FILE);
			) {
				content = new byte [read.available ()];
				read.read (content, 0, content.length);
				sendMessage (os, content);
			}
		} else if ("write".equals (command)) {
			try (
				OutputStream write = new FileOutputStream (FILE);
			) {
				while (tokens.hasMoreTokens ()) {
					write.write (tokens.nextToken ().getBytes ());
					write.write (' '); // Whitespace delimiter
				}
			}
		} else {
			System.out.println (message);
		}
	}
	
	private static int bit (byte number, int index) {
		return (number & (1 << index)) >> index;
	}
	
	private static void sendMessage (OutputStream os, byte [] content) throws IOException {
		ByteArrayOutputStream builder = new ByteArrayOutputStream ();
		builder.write (0b10000001); // FIN + RSV + Opcode
		
		if (content.length < 126) {
			builder.write (content.length & 0b01111111);
		} else if (content.length < (1 << 16)) {
			builder.write (126);
			builder.write ((content.length >> 8) & 0b01111111);
			builder.write (content.length & 0b01111111);
		} else {
			// not supported
		}
		
		builder.write (content, 0, content.length);
		os.write (builder.toByteArray ());
		os.flush ();
	}
	
}
