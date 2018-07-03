package ru.shemplo.pluses.client;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import ru.shemplo.pluses.network.message.JavaMessage;
import ru.shemplo.pluses.network.message.JavaMessage.MessageDirection;

public class Run {

	public static void main (String ... args) throws UnknownHostException, IOException {
		Socket socket = new Socket ("localhost", 1999);
		OutputStream os = socket.getOutputStream ();
		InputStream is = socket.getInputStream ();
		
		Reader r = new InputStreamReader (System.in, StandardCharsets.UTF_8);
		BufferedReader br = new BufferedReader (r);
		
		String line = null;
		while ((line = br.readLine ()) != null) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream ();
			ObjectOutputStream oos = new ObjectOutputStream (baos);
			
			MessageDirection dir = MessageDirection.CLIENT_TO_SERVER;
			JavaMessage message = new JavaMessage (dir, line);
			oos.writeObject (message);
			oos.flush ();
			
			byte [] data = baos.toByteArray ();
			System.out.println (data.length);
			byte [] length = {
				(byte) (data.length >> 24 & 0xff),
				(byte) (data.length >> 16 & 0xff),
				(byte) (data.length >> 8 & 0xff),
				(byte) (data.length & 0xff)
			};
			os.write (length);
			os.write (data);
			os.flush ();
		}
		
		socket.close ();
	}
	
}
