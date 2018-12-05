package ru.shemplo.pluses.network.pool;

import ru.shemplo.pluses.network.message.AppMessage;
import ru.shemplo.pluses.network.message.Message;

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
	public AppMessage getInput ();
	
	public void update ();
	
	public long getLastUpdated ();
	
	public long getLastActivity ();
	
	public void sendMessage (Message message);
	
}
