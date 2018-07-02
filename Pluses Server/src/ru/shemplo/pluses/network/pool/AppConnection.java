package ru.shemplo.pluses.network.pool;


public interface AppConnection {

	public String getIdentifier ();
	
	public boolean isConnected ();
	
	public int getInputSize ();
	
	default
	public boolean hasInput () {
		return getInputSize () > 0;
	}
	
	public String getInput ();
	
	public void update ();
	
	public long getLastUpdated ();
	
	public long getLastActivity ();
	
}
