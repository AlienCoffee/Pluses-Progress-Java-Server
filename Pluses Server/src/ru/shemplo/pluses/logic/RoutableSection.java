package ru.shemplo.pluses.logic;

import ru.shemplo.pluses.network.message.Message;

public interface RoutableSection {
    
    /**
     * Route {@link RoutableBundle} to next section handler.
     * 
     * Information about destination section contains in {@link Message}
     * (in the first parameter of bundle), 
     * and can be reached with {@link Message#getCommand} method.
     * 
     * @param bundle
     * 
     */
    public void route (RoutableBundle bundle);
    
}
