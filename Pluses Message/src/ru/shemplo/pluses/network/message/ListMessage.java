package ru.shemplo.pluses.network.message;

import java.io.Serializable;
import java.util.List;

public class ListMessage <T extends Serializable> extends AbsAppMessage {

    /**
     * 
     */
    private static final long serialVersionUID = -7424196682452827318L;

    protected final List <T> LIST;
    
    public ListMessage (Message reply, MessageDirection direction, List <T> list) {
        super (reply, direction);
        
        this.LIST = list;
    }
    
    public ListMessage (MessageDirection direction, List <T> list) {
        this (null, direction, list);
    }
    
    public List <T> getList () {
        return LIST;
    }
    
}
