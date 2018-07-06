package ru.shemplo.pluses.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;

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
        final String host = "jdbc:mysql://shemplo.ru/db.test?useSSL=true&serverTimezone=UTC";
        CONNECTION = DriverManager.getConnection (host, "visitor", "wru4");
    }

    @Override
    public void close () throws Exception {
        CONNECTION.close ();
    }
    
}
