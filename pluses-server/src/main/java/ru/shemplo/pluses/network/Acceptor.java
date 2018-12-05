package ru.shemplo.pluses.network;

import java.net.Socket;

public interface Acceptor extends AutoCloseable {

	public boolean handshake (String identifier, Socket socket);
	
	public void onSocketReady (String identifier, Socket socket);
	
}
