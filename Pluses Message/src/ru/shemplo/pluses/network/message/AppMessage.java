package ru.shemplo.pluses.network.message;

public interface AppMessage extends Message {

	public static enum MessageDirection {
	    // Server to client
		STC, 
		// Client to server
		CTS
	}
	
	public MessageDirection getDirection ();
	
	public Message getReplyMessage ();
	
}
