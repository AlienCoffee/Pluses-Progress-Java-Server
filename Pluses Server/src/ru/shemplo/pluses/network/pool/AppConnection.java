package ru.shemplo.pluses.network.pool;

import ru.shemplo.pluses.network.message.JavaAppMessage;

public interface AppConnection extends AutoCloseable {

	public String getIdentifier ();
	
	public boolean isConnected ();
	
	public int getInputSize ();
	
	default
	public boolean hasInput () {
		return getInputSize () > 0;
	}
	
	/**
	 * 
	 * @return one message from the stream
	 * 
	 */
	public JavaAppMessage getInput ();
	
	public void update ();
	
	public long getLastUpdated ();
	
	public long getLastActivity ();
	
}
