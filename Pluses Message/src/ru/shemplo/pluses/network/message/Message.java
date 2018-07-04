package ru.shemplo.pluses.network.message;

import java.io.Serializable;

public interface Message extends Serializable {

	public static enum MessageDirection {
		SERVER_TO_CLIENT, 
		CLIENT_TO_SERVER
	}
	
	public MessageDirection getDirection ();
	
	public String getSection ();
	
	public String getCommand ();
	
}
