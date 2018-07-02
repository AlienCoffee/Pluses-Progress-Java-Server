package ru.shemplo.pluses.network.message;


public class JavaMessage {

	public static enum MessageDirection {
		SERVER_TO_CLIENT, 
		CLIENT_TO_SERVER
	}
	
	public final MessageDirection DIRECTION;
	public final String CONTENT;
	
	public JavaMessage (MessageDirection direction, String content) {
		this.DIRECTION = direction;
		this.CONTENT = content;
	}
	
}
