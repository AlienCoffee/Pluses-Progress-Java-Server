package ru.shemplo.pluses.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import org.json.JSONObject;

public class RunJSON {
    
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
        Socket socket = new Socket ("localhost", 2000);
        OutputStream os = socket.getOutputStream ();
        InputStream is = socket.getInputStream ();
        
        Reader r = new InputStreamReader (System.in, StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader (r);
        
        new Thread (() -> {
            byte [] capacer = new byte [4];
            
            while (socket.isConnected ()) {
                try {
                    is.read (capacer, 0, capacer.length);
                    int read = (int) backvert (capacer, 4);
                    System.out.println ("To read: " + read);
                    byte [] data = new byte [read];
                    read = is.read (data, 0, data.length);
                    if (read == -1) { System.exit (0); }
                    
                    String input = new String (data, 0, data.length);
                    System.out.println (input);
                } catch (IOException ioe) {}
            }
        }).start ();
        
        String line = null;
        while ((line = br.readLine ()) != null && socket.isConnected ()) {
            JSONObject root = new JSONObject ();
            root.put ("section", "message");
            root.put ("timestamp", System.currentTimeMillis ());
            root.put ("content", line.trim ());
            
            byte [] data = root.toString ().getBytes (StandardCharsets.UTF_8);
            System.out.println (root.toString ());
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
