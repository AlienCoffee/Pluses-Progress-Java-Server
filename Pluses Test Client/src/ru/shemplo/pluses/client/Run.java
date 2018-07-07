package ru.shemplo.pluses.client;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import ru.shemplo.pluses.network.message.AppMessage;
import ru.shemplo.pluses.network.message.AppMessage.MessageDirection;
import ru.shemplo.pluses.network.message.JavaAppMessage;
import ru.shemplo.pluses.network.message.PPMessage;

public class Run {

    private static long backvert (byte [] bytes, int length) {
        int limit = Math.min (length, bytes.length);
        long result = 0;
        
        for (int i = 0; i < limit; i++) {
            result = (result << 8) | (bytes [i] & 0xffL);
        }
        
        return result;
    }
    
	public static void main (String ... args) throws UnknownHostException, 
	        IOException, ClassNotFoundException {
		Socket socket = new Socket ("localhost", 1999);
		OutputStream os = socket.getOutputStream ();
		InputStream is = socket.getInputStream ();
		
		Reader r = new InputStreamReader (System.in, StandardCharsets.UTF_8);
		BufferedReader br = new BufferedReader (r);
		
		new Thread (() -> {
		    byte [] capacer = new byte [4];
		    
		    while (true) {
		        try {
		            is.read (capacer, 0, capacer.length);
	                int read = (int) backvert (capacer, 4);
	                System.out.println ("To read: " + read);
	                byte [] data = new byte [read];
	                is.read (data, 0, data.length);  
	                
	                ByteArrayInputStream bais = new ByteArrayInputStream (data);
	                ObjectInputStream bis = new ObjectInputStream (bais);
	                Object tmp = bis.readObject ();
	                
	                if (tmp instanceof AppMessage) {
	                    AppMessage income = (AppMessage) tmp;
	                    System.out.println ("Section: " + income.getSection ());
	                    System.out.println ("Time: " + income.getTimestamp ());
	                    System.out.println (income.getContent ());
	                } else if (tmp instanceof PPMessage) {
	                    PPMessage ping = (PPMessage) tmp;
	                    System.out.println ("Value: " + ping.VALUE);
	                } else {
	                    System.out.println (tmp);
	                }
		        } catch (IOException | ClassNotFoundException es) {}
		    }
		}).start ();
		
		String line = null;
        while ((line = br.readLine ()) != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream ();
            ObjectOutputStream oos = new ObjectOutputStream (baos);
            
            MessageDirection dir = MessageDirection.CLIENT_TO_SERVER;
            AppMessage message = new JavaAppMessage (dir, line);
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
