package ru.shemplo.pluses.network.message;

import java.io.Serializable;
import java.util.List;

import org.json.JSONObject;

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
    
    @Override
    public JSONObject toJSON (JSONObject root) {
        JSONObject tmp = super.toJSON (root);
        tmp.put ("type", "listmessage");
        tmp.put ("list", LIST);
        return tmp;
    }
    
    public List <T> getList () {
        return LIST;
    }
    
}
