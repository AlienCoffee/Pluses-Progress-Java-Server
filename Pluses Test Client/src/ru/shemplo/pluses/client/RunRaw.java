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

public class RunRaw {
    
    public static void main (String ... args) throws UnknownHostException, 
            IOException, ClassNotFoundException {
        Socket socket = new Socket ("localhost", 1999);
        OutputStream os = socket.getOutputStream ();
        InputStream is = socket.getInputStream ();
        
        Reader r = new InputStreamReader (System.in, StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader (r);
        
        String line = null;
        while ((line = br.readLine ()) != null) {            
            byte [] data = line.getBytes ();
            System.out.println (data.length);
            byte [] length = {
                (byte) (data.length >> 24 & 0xff),
                (byte) (data.length >> 16 & 0xff),
                (byte) (data.length >> 8  & 0xff),
                (byte) (data.length       & 0xff)
            };
            os.write (length);
            os.write (data);
            os.flush ();
            
            byte [] bufferLen = new byte [4];
            is.read (bufferLen, 0, bufferLen.length);
            int read = (bufferLen [0] << 24) 
                        | (bufferLen [1] << 16) 
                        | (bufferLen [2] << 8) 
                        | bufferLen [3];
            System.out.println (read);
            data = new byte [read];
            is.read (data, 0, data.length);
            
            System.out.println (new String (data, StandardCharsets.UTF_8));
        }
        
        socket.close ();
    }
    
}
