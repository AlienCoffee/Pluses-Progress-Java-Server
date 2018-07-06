package ru.shemplo.pluses.network.message;

public class JavaAppMessage implements AppMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8333593235874302637L;
	
	private final MessageDirection DIRECTION;
	private final String CONTENT;
	private final long TIMESTAMP;
	
	public JavaAppMessage (MessageDirection direction, String content) {
	    this.TIMESTAMP = System.currentTimeMillis ();
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
	public String getContent () {
		return CONTENT;
	}

    @Override
    public long getTimestamp () {
        return TIMESTAMP;
    }
	
}
