package ru.shemplo.pluses.db;

import java.sql.Statement;
import java.util.*;

public class DBAdapterMoq extends MySQLAdapter {

    private final static Map <String, Set <String>> 
        fakeColumns = new HashMap <> ();
    
    static {
        fakeColumns.put ("test", new HashSet <> (Arrays.asList (
            "id", "name", "lastname", "age", "sex"
        )));
    }
    
    public DBAdapterMoq () { super (null, fakeColumns); }
    
    @Override
    public boolean testConnection () {
        return true;
    }
    
    @Override
    public Optional <Statement> getStatement () {
        return Optional.empty ();
    }
    
    @Override
    public <R> List <R> runFetchFromArray (String query, List <R> list) {
        return new ArrayList <> ();
    }
    
    @Override
    public int runCountInArray (String query, List <?> list) {
        return -1;
    }
    
    @Override
    public void close () throws Exception {
        
    }
    
}
