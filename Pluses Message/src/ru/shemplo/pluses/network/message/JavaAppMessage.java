package ru.shemplo.pluses.network.message;

public class JavaAppMessage extends AbsAppMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8333593235874302637L;
	
	private final String CONTENT;
	
	public JavaAppMessage (MessageDirection direction, String content) {
	    super (direction);
		this.CONTENT = content;
	}

	@Override
	public String getContent () {
		return CONTENT;
	}
	
}
