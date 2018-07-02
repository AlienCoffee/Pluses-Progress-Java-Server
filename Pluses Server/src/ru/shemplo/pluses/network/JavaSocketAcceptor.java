package ru.shemplo.pluses.network;

import java.io.IOException;
import java.net.Socket;

public class JavaSocketAcceptor extends AbsSocketAcceptor {

	public JavaSocketAcceptor (int port, int threads) throws IOException {
		super (port, threads);
	}

	@Override
	public void handshake (String identifier, Socket socket) {
		System.out.println ("Greeting for " + identifier);
	}
	
	@Override
	public void onSocketReady (String identifier, Socket socket) {
		
	}

}
