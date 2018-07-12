package ru.shemplo.pluses.network.message;

import org.json.JSONObject;

public interface HasJSON {

    public JSONObject toJSON (JSONObject root);
    
}
