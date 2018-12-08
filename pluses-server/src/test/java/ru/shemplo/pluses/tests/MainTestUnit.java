package ru.shemplo.pluses.tests;

import static org.junit.jupiter.api.Assertions.*;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ru.shemplo.pluses.db.DBAdapterMoq;
import ru.shemplo.pluses.db.MySQLAdapter;
import ru.shemplo.pluses.util.SQLUtil;

public class MainTestUnit {
    
    private static final DBAdapterMoq DBMoq; 
    
    static { // Because @BeforeAll doesn't works for nested classes only
        MySQLAdapter.setInstance (DBMoq = new DBAdapterMoq ());
    }
    
    @Nested
    public class TestDatabase {
        
        private MySQLAdapter db;
        
        @BeforeEach
        public void init () {
            db = MySQLAdapter.getInstance ();
        }
        
        @Test
        public void testInstance () {
            assertEquals (DBMoq, db);
        }
        
        @Test
        public void testGettingColumns () {
            Set <String> set = db.getTableColumns ("test");
            
            assertNotNull (set);
            assertTrue (set.size () == 5);
            assertTrue (set.contains ("lastname"));
        }
        
        @Nested
        public class TestDatabaseUtils { 
            
            @Test
            public void testInsQueryAppears () {
                assertNotNull (SQLUtil.makeInsertQuery ("test", 
                                           new HashMap <> ()));
                
            }
            
            @Test
            public void testInsQueryCorrectness () {
                Map <String, String> params = new HashMap <> ();
                params.put ("parameter1", "value1");
                params.put ("name", "Stub name");
                
                String result = SQLUtil.makeInsertQuery ("test", params);
                assertEquals ("INSERT INTO `test` (`name`) "
                              + "VALUES ('Stub name')", result);
                
                params.put ("id", "12");
                result = SQLUtil.makeInsertQuery ("test", params);
                assertEquals ("INSERT INTO `test` (`name`, `id`) "
                              + "VALUES ('Stub name', '12')", result);
            }
            
            @Test
            public void testSpecialToArray () {
                List <String> smth = Arrays.asList ("gf", "4up4", "Cds0", "*(EQ");
                assertEquals ("($gf$, $4up4$, $Cds0$, $*(EQ$)", SQLUtil.toArray (smth, '$'));
            }
            
            @Test
            public void testDateFormat () throws ParseException {
                assertNotNull (SQLUtil.getDatetime ());
                
                long time = Instant.now ().toEpochMilli ();
                String result = SQLUtil.getDatetime (time);
                
                DateFormat format = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss");
                long tmp = format.parse (result).toInstant ().getEpochSecond ();
                assertEquals (tmp, time / 1000);
            }
            
        }
        
    }
    
}
