package ru.shemplo.pluses.stub;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class RunRaw {
    
    private static final File FILE = new File ("common.txt");
    
    private static boolean isRunning = true;
    
    public static void main (String ... args) throws IOException {
        @SuppressWarnings ("resource")
        ServerSocket server = new ServerSocket (1999);
        System.out.println ("Server started");
        
        FILE.createNewFile ();
        
        while (isRunning) {
            Socket socket = server.accept ();
            OutputStream os = socket.getOutputStream ();
            InputStream is = socket.getInputStream ();
            
            byte [] capacer = new byte [4];
            while (true) {
                try {
                    is.read (capacer, 0, capacer.length);
                    int length = (capacer [0] << 24) 
                                  | (capacer [1] << 16) 
                                  | (capacer [2] << 8) 
                                  | capacer  [3];
                    byte [] buffer = new byte [length];
                    is.read (buffer, 0, buffer.length);
                    
                    String input = new String (buffer, StandardCharsets.UTF_8);
                    System.out.println ("Input (size: " + length + "): " + input);
                    if (input.indexOf ("read") != -1) {
                        try (
                            InputStream read = new FileInputStream (FILE);
                        ) {
                            buffer = new byte [read.available ()];
                            read.read (buffer, 0, buffer.length);
                            
                            capacer [0] = (byte) (buffer.length >> 24 & 0xffL);
                            capacer [1] = (byte) (buffer.length >> 16 & 0xffL);
                            capacer [2] = (byte) (buffer.length >> 8  & 0xffL);
                            capacer [3] = (byte) (buffer.length       & 0xffL);
                            
                            os.write (capacer, 0, capacer.length);
                            os.write (buffer);
                            os.flush ();
                        }
                    } else {
                        buffer = ("some income string: " + input).getBytes ();
                        
                        capacer [0] = (byte) (buffer.length >> 24 & 0xffL);
                        capacer [1] = (byte) (buffer.length >> 16 & 0xffL);
                        capacer [2] = (byte) (buffer.length >> 8  & 0xffL);
                        capacer [3] = (byte) (buffer.length       & 0xffL);
                        
                        os.write (capacer, 0, capacer.length);
                        os.write (buffer);
                        os.flush ();
                    }
                } catch (IOException ioe) {
                    System.out.println ("Connection closed ... waiting for new one");
                    break;
                }
            }
        }
    }
    
}
