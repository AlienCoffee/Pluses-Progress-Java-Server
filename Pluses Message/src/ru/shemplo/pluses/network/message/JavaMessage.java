package ru.shemplo.pluses.network.message;

import java.io.Serializable;

public class JavaMessage implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8333593235874302637L;

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
