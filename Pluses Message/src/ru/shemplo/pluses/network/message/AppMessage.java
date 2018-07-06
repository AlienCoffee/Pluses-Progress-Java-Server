package ru.shemplo.pluses.network.message;

public interface AppMessage extends Message {

	public static enum MessageDirection {
		SERVER_TO_CLIENT, 
		CLIENT_TO_SERVER
	}
	
	public MessageDirection getDirection ();
	
	default
	public String getSection () {
	    return "message";
	}
	
	public String getContent ();
	
}
