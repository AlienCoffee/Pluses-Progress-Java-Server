package ru.shemplo.pluses.network.pool;

import static ru.shemplo.pluses.network.message.AppMessage.MessageDirection.*;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.json.JSONException;
import org.json.JSONObject;

import ru.shemplo.pluses.log.Log;
import ru.shemplo.pluses.network.message.AppMessage;
import ru.shemplo.pluses.network.message.JSONMessage;
import ru.shemplo.pluses.network.message.Message;
import ru.shemplo.pluses.util.json.BytesManip;

public class RawConnection extends AbsConnection {

    public RawConnection (String identifier, Socket socket) throws IOException {
        super (identifier, socket);
    }
    
    private volatile int reserved = -1;
    
    @Override
    protected void _readStreamData () {
        long start = System.currentTimeMillis ();
        try {
            long time = start;
            while (time - start < 1 * 1000 && IS.available () > 0) {
                if (reserved != -1 && IS.available () >= reserved) {
                    byte [] buffer = new byte [reserved];
                    IS.read (buffer, 0, buffer.length);
                    
                    String string = new String (buffer, StandardCharsets.UTF_8);
                    try {
                        JSONObject root = new JSONObject (string);
                        System.out.println (root.toString ());
                    } catch (JSONException jsone) {
                        String message = "Wrong input format of JSON message";
                        
                        JSONObject root = new JSONObject ();
                        root.append ("section", "error");
                        root.append ("content", message);
                        root.append ("timestamp", time);
                        
                        AppMessage error = new JSONMessage (SERVER_TO_CLIENT, root.toString ());
                        Log.error (RawConnection.class.getSimpleName (), message);
                        Log.error (RawConnection.class.getSimpleName (), jsone);
                        sendMessage (error);
                    }
                    
                    reserved = -1;
                } else if (reserved == -1 && IS.available () > 4) {
                    System.out.println ("Fetching length");
                    byte [] buffer = new byte [4];
                    IS.read (buffer, 0, buffer.length);
                    reserved = BytesManip.B2I (buffer);
                    System.out.println ("Reserved: " + reserved);
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
        
        
    }
    
}
