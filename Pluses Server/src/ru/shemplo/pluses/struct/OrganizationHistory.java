package ru.shemplo.pluses.struct;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import ru.shemplo.pluses.db.MySQLAdapter;

public class OrganizationHistory {
    
    private static final ConcurrentMap <Integer, StudentHistory> 
        STUDENTS = new ConcurrentHashMap <> ();
    
    public static void init () {
        Connection mysql = MySQLAdapter.getInstance ().getDB ();
        try {
            String query = "SELECT `id` FROM `students` ORDER BY `id` ASC";
            Statement statement = mysql.createStatement ();
            ResultSet answer    = statement.executeQuery (query);
            while (answer.next ()) {
                int id = answer.getInt (1);
                
                StudentHistory student = new StudentHistory (id);
                STUDENTS.put (student.getStudentID (), student);
            }
            
            query = "SELECT * FROM `movements` ORDER BY `time` ASC";
            statement = mysql.createStatement ();
            answer    = statement.executeQuery (query);
            while (answer.next ()) {
                // Object is necessary because student can be just removed
                // (in this situation value in table will be NULL)
                Integer student = answer.getInt ("student"),
                        from = (Integer) answer.getObject ("from"),
                        to = (Integer) answer.getObject ("to");
                Time time = answer.getTime ("time");
                if (!STUDENTS.containsKey (student)) { continue; }
                
                // Here try/catch is not necessary because it's just single thread
                STUDENTS.get (student).addMovement (from, to, time.getTime ());
            }
        } catch (SQLException sqle) {
            sqle.printStackTrace ();
        }
    }
    
    private static class StudentHistory {
        
        public final int STUDENT_ID;
        
        private final AtomicReference <Integer> CURRENT;
        private final List <StudentEntry> ENTRIES;
        
        public StudentHistory (int studentID) {
            this.CURRENT = new AtomicReference <> (null);
            this.STUDENT_ID = studentID;
            
            this.ENTRIES = new ArrayList <> ();
            // When student just created it moves to
            // undefined group with value NULL
            ENTRIES.add (new StudentEntry (null, 0));
        }
        
        public int getStudentID () {
            return STUDENT_ID;
        }
        
        public void addMovement (Integer from, Integer to, long timestamp)
                throws NoSuchElementException {
            // Critical zone just for one thread
            // Value -1 is necessary for preventing situation when comes
            // two requests (a -> b) and (b -> c) in a row, but first
            // wasn't completed
            // The value -1 is impossible to be the is of group in MySQL
            // it can be broken just in case of bad request (! WARNING !)
            if (CURRENT.compareAndSet (from, -1)) {
                StudentEntry last = ENTRIES.get (ENTRIES.size () - 1);
                ENTRIES.add (new StudentEntry (to, timestamp));
                if (last.TIMESTAMP > timestamp) {
                    ENTRIES.sort ((a, b) -> 
                        Long.compare (a.TIMESTAMP, b.TIMESTAMP)
                    );
                }
                
                CURRENT.set (to);
            } else {
                String message = "Student " + STUDENT_ID + " isn't in group " + from;
                // This is unchecked exception for concurrent access
                // This method doesn't receive reference to connection
                // but still need to notify user about the error
                throw new NoSuchElementException (message);
            }
        }
        
        private static class StudentEntry {
            
            public final long TIMESTAMP;
            public final Integer GROUP;
            
            public StudentEntry (Integer group, long timestamp) {
                this.TIMESTAMP = timestamp;
                this.GROUP = group;
            }
            
        }
        
    }
    
}
