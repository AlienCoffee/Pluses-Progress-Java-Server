package ru.shemplo.pluses.logic;

import ru.shemplo.pluses.network.message.Message;
import ru.shemplo.pluses.network.pool.AppConnection;
import ru.shemplo.pluses.struct.Pair;

public class RoutableBundle extends Pair <Message, AppConnection> {

    public RoutableBundle (Message F, AppConnection S) {
        super (F, S);
    }
    
    public static RoutableBundle valueOf (Message message, 
                                 AppConnection connection) {
        return new RoutableBundle (message, connection);
    }
    
}
