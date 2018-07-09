package ru.shemplo.pluses.network.pool;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import ru.shemplo.pluses.network.message.AppMessage;
import ru.shemplo.pluses.network.message.Message;
import ru.shemplo.pluses.network.message.PPMessage;
import ru.shemplo.pluses.network.message.PPMessage.Ping;

public abstract class AbsConnection implements AppConnection {
    
    protected final ConcurrentLinkedQueue <AppMessage> 
        INPUT = new ConcurrentLinkedQueue <> ();
    protected final String IDENTIFIER;
    
    protected final OutputStream OS;
    protected final InputStream IS;
    protected final Socket SOCKET;
    
    protected long active = System.currentTimeMillis ();
    protected AtomicLong updated = new AtomicLong ();
    protected volatile boolean isConnected = true;
    protected volatile boolean isPending = false;
    
    public AbsConnection (String identifier, Socket socket) throws IOException {
        this.OS = socket.getOutputStream ();
        this.IS = socket.getInputStream ();
        this.IDENTIFIER = identifier;
        this.SOCKET = socket;
    }
    
    @Override
    public String getIdentifier () {
        return IDENTIFIER;
    }

    @Override
    public boolean isConnected () {
        return this.isConnected;
    }

    @Override
    public int getInputSize () {
        return INPUT.size ();
    }
    
    @Override
    public AppMessage getInput () {
        AppMessage message = INPUT.poll ();
        if (Objects.isNull (message)) {
            return null;
        }
        
        return message;
    }
    
    @Override
    public long getLastUpdated () {
        return updated.get ();
    }

    @Override
    public long getLastActivity () {
        return active;
    }
    
    @Override
    public void update () {
        // Nothing to update: connection closed
        if (!isConnected ()) { return; }
        
        long now = System.currentTimeMillis (), prev = getLastUpdated ();
        long max = Long.MAX_VALUE; // This is necessary to prevent the
        // situation when update would take more time than impulse period
        if (now - prev > 5 * 100 && updated.compareAndSet (prev, max)) {
            // This section is available for one thread only, that's why
            // here can be called method below
            _readStreamData ();
            
            // Checking connection with a client 
            // sending him PING message
            if (now - active > 15 * 1000 && !isPending && isConnected) {
                Message ping = new PPMessage (Ping.PING);
                sendMessage (ping);
                
                isPending = true;
                active = now;
            // wait 10 seconds after PING message
            } else if (now - active > 30 * 1000 && isPending) {
                // Dropping connection by the reason of unused
                isConnected = false;
            }
            
            // Finishing updating
            updated.compareAndSet (max, now);
        }
    }
    
    protected abstract void _readStreamData ();
    
    @Override
    public synchronized void close () throws Exception {
        _readStreamData (); // Reading to prevent lose of data
        // Mark connection as disconnected
        isConnected = false;
        
        // Finishing closing of connection
        OS.close (); IS.close ();
        SOCKET.close ();
    }
    
}
