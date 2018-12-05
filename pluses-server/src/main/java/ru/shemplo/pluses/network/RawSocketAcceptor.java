package ru.shemplo.pluses.network;

import java.io.IOException;
import java.net.Socket;

import ru.shemplo.pluses.log.Log;
import ru.shemplo.pluses.network.pool.AppConnection;
import ru.shemplo.pluses.network.pool.ConnectionPool;
import ru.shemplo.pluses.network.pool.RawConnection;

public class RawSocketAcceptor extends AbsSocketAcceptor {

    public RawSocketAcceptor (int port, int threads) throws IOException {
        super (port, threads);
    }

    @Override
    public boolean handshake (String identifier, Socket socket) {
        System.out.println ("Handshake with " + identifier + " done");
        return true;
    }

    @Override
    public void onSocketReady (String identifier, Socket socket) {
        System.out.println ("Socket " + identifier + " is ready");
        ConnectionPool pool = ConnectionPool.getInstance ();
        AppConnection connection = null;
        
        try {
            connection = new RawConnection (identifier, socket);
            pool.registerConnection (connection);
        } catch (Exception e) {
            Log.error (JavaSocketAcceptor.class.getSimpleName (), 
                "Socket made handshake but failed to be wrapped into connection:\n" + e);
        }
    }
    
}
