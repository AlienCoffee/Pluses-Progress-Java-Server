package ru.shemplo.pluses.network.message;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.json.JSONObject;

public class ListMessage <T> extends AbsAppMessage {

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
        if (!Objects.isNull (LIST) && LIST.size () > 0) {
            List <Object> out = new ArrayList <> ();
            if (LIST.get (0) instanceof HasJSON) {
                for (int i = 0; i < LIST.size (); i++) {
                    HasJSON object = (HasJSON) LIST.get (i);
                    out.add (object.toJSON (new JSONObject ()));
                }
            } else {
                for (Object object : LIST) {
                    if (!(object instanceof Number)) {
                        out.add ("" + object);
                    } else {
                        out.add (object);
                    }
                }
            }
            
            tmp.put ("list", out);
        } else {
            tmp.put ("list", LIST);
        }
        
        return tmp;
    }
    
    public List <T> getList () {
        return LIST;
    }
    
}
