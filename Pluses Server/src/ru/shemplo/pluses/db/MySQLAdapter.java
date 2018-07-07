package ru.shemplo.pluses.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import ru.shemplo.pluses.log.Log;

public class MySQLAdapter implements AutoCloseable {
 
    private static MySQLAdapter ADAPTER;
    
    public static MySQLAdapter getInstance () {
        if (Objects.isNull (ADAPTER)) {
            synchronized (MySQLAdapter.class) {
                if (Objects.isNull (ADAPTER)) {
                    try {
                        ADAPTER = new MySQLAdapter ();
                    } catch (SQLException sqle) {
                        sqle.printStackTrace();
                    }
                }
            }
        }
        
        return ADAPTER;
    }
    
    private final Connection CONNECTION;
    
    private MySQLAdapter () throws SQLException {
        final String host = System.getProperty ("pluses.db.host"),
                     base = System.getProperty ("pluses.db.base"),
                     user = System.getProperty ("pluses.db.user"),
                     pass = System.getProperty ("pluses.db.pass");
        String url = "jdbc:mysql://" + host + "/" + base 
                     + "?useSSL=true&serverTimezone=UTC";
        
        CONNECTION = DriverManager.getConnection (url, user, pass);
        Log.log (MySQLAdapter.class.getSimpleName (), 
            "Connection to database established");
    }
    
    public Connection getDB () {
        return CONNECTION;
    }

    public int runCountInArray (String query, List <?> list) {
        if (Objects.isNull (CONNECTION)) {
            String message = "Database is not connected";
            throw new IllegalStateException (message);
        }
        
        // convert list to ('1', '2', 'a', '#')
        StringBuilder sb = new StringBuilder ("(");
        for (int i = 0; i < list.size (); i++) {
            sb.append ("'");
            sb.append ("" + list.get (i));
            sb.append ("'");
            if (i < list.size () - 1) {
                sb.append (",");
            }
        }
        sb.append (")");
        
        try {
            query = query.replace ("?", sb.toString ().trim ());
            PreparedStatement statement = CONNECTION.prepareStatement (query);
            ResultSet answer = statement.executeQuery ();
            if (answer.next ()) {
                // Query in format: SELECT COUNT(*) AS '' FROM ...
                // That's why it's safe to return 1 column
                // WARN: 1 because the numeration from 1
                return answer.getInt (1);
            }
        } catch (SQLException sqle) {
            System.out.println (sqle);
            return -1;
        }
        
        return -1;
    }
    
    public int runCountInArray (String query, Object... array) {
        if (Objects.isNull (array)) { return -1; }
        return runCountInArray (query, Arrays.asList (array));
    }
    
    @Override
    public void close () throws Exception {
        CONNECTION.close ();
    }
    
}
