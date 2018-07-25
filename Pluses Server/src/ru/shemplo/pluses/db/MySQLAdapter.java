package ru.shemplo.pluses.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import ru.shemplo.pluses.log.Log;

public class MySQLAdapter implements AutoCloseable {
 
    private static MySQLAdapter ADAPTER;
    
    public static MySQLAdapter getInstance () {
        if (Objects.isNull (ADAPTER)) {
            synchronized (MySQLAdapter.class) {
                if (Objects.isNull (ADAPTER) 
                    || !ADAPTER.testConnection ()) {
                    try {
                        ADAPTER = new MySQLAdapter ();
                    } catch (SQLException sqle) {
                        sqle.printStackTrace();
                    } catch (Exception e) {
                        System.out.println (e);
                    }
                }
            }
        }
        
        return ADAPTER;
    }
    
    private final Map <String, Set <String>> COLUMNS;
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
        
        this.COLUMNS = new HashMap <> ();
        Statement statement = CONNECTION.createStatement ();
        ResultSet answer = statement.executeQuery ("SHOW TABLES");
        while (answer.next ()) {
            String table = answer.getString (1);
            statement = CONNECTION.createStatement ();
            ResultSet columns = statement.executeQuery (
                "SHOW COLUMNS FROM `" + table + "`");
            Set <String> cols = new HashSet <> ();
            while (columns.next ()) {
                cols.add (columns.getString (1));
            }
            // Specific field that can't be set
            cols.remove ("id");

            COLUMNS.put (table, cols);
        }
        
    }
    
    public Connection getDB () {
        return CONNECTION;
    }
    
    public boolean testConnection () {
        try {
            Statement statement = CONNECTION.createStatement ();
            statement.execute ("SELECT 1");
        } catch (SQLException sqle) {
            return false;
        }
        
        return true;
    }
    
    public Set <String> getTableColumns (String table) {
        return COLUMNS.get (table);
    }
    
    public Optional <Statement> getStatement () {
        Statement out = null;
        try {
            out = CONNECTION.createStatement ();
        } catch (SQLException sqle) {
            Log.error (MySQLAdapter.class.getSimpleName (), sqle);
        }
        
        return Optional.of (out);
    }

    public <R> List <R> runFetchFromArray (String query, List <R> list) {
        if (Objects.isNull (CONNECTION)) {
            String message = "Database is not connected";
            throw new IllegalStateException (message);
        }
        
        Map <String, Integer> map = new HashMap <> ();
        StringBuilder sb = new StringBuilder ("(");
        for (int i = 0; i < list.size (); i++) {
            sb.append ("'");
            sb.append ("" + list.get (i));
            map.put ("" + list.get (i), i);
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
            List <R> fetch = new ArrayList <> ();
            if (answer.next ()) {
                Integer index = map.get (answer.getString (1));
                fetch.add (list.get (index));
            }
            
            return fetch;
        } catch (SQLException sqle) {
            System.out.println (sqle);
            return new ArrayList <> ();
        }
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
