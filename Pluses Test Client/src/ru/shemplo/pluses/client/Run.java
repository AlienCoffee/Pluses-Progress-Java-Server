package ru.shemplo.pluses.client;

import java.io.BufferedReader;
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
		
		ObjectOutputStream bos = new ObjectOutputStream (os);
		
		String line = null;
		while ((line = br.readLine ()) != null) {
			MessageDirection dir = MessageDirection.CLIENT_TO_SERVER;
			JavaMessage message = new JavaMessage (dir, line);
			bos.writeObject (message);
			bos.flush ();
		}
		
		socket.close ();
	}
	
}
