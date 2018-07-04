package ru.shemplo.pluses.network.message;

public class JavaAppMessage implements Message {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8333593235874302637L;
	
	public final MessageDirection DIRECTION;
	public final String CONTENT;
	
	public JavaAppMessage (MessageDirection direction, String content) {
		this.DIRECTION = direction;
		this.CONTENT = content;
	}

	@Override
	public MessageDirection getDirection () {
		return DIRECTION;
	}

	@Override
	public String getSection () {
		return CONTENT;
	}

	@Override
	public String getCommand () {
		return CONTENT;
	}
	
}
