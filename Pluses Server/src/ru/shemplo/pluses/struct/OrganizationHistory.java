package ru.shemplo.pluses.struct;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import ru.shemplo.pluses.db.MySQLAdapter;
import ru.shemplo.pluses.log.Log;
import ru.shemplo.pluses.util.BytesManip;
import ru.shemplo.pluses.util.FSCrawler;

public class OrganizationHistory {
    
    private static final ConcurrentMap <Integer, StudentHistory> 
        STUDENTS = new ConcurrentHashMap <> ();
    private static final ConcurrentMap <Integer, TopicEntry>
        TOPICS = new ConcurrentHashMap <> ();
    
    public synchronized static void init () {
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
            
            query = "SELECT `id` FROM `topics` ORDER BY `id` ASC";
            statement = mysql.createStatement ();
            answer    = statement.executeQuery (query);
            while (answer.next ()) {
                int topicID = answer.getInt ("id");
                
                TopicEntry topic = new TopicEntry (topicID);
                TOPICS.put (topic.getTopicID (), topic);
            }
        } catch (SQLException sqle) {
            sqle.printStackTrace ();
        }
    }
    
    public synchronized static void close () throws Exception {
        for (TopicEntry entry : TOPICS.values ()) {
            entry.close (); // Closing restore files
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
        
        @SuppressWarnings ("unused")
        public List <Pair <Integer, Integer>> getTopics () {
            List <Pair <Integer, Integer>> topics = new ArrayList <> ();
            List <StudentEntry> save = new ArrayList <> (ENTRIES);
            
            // Guaranteed that list of entries is not empty
            StudentEntry last = save.get (save.size () - 1);
            
            Set <Integer> keys = TOPICS.keySet ();
            topicI: for (Integer topicID : keys) {
                TopicEntry topic = TOPICS.get (topicID);
                Set <Integer> topicGroups = topic.getGroups ();
                if (topicGroups.contains (last.GROUP)) {
                    // BINGO! topic belongs to the same group as student
                    topics.add (Pair.mp (topic.TOPIC_ID, 0));
                    continue;
                }
                
                for (int i = 0; i < save.size () - 1; i++) {
                    StudentEntry entry = save.get (i);
                    
                    Time move = new Time (entry.TIMESTAMP);
                    int groupID = entry.GROUP;
                    
                    if (!topicGroups.contains (topicID)) { continue; }
                    
                    Time created = topic.getCreated (groupID),
                         expirated = topic.getExpired (groupID);
                    Time nextEntry = new Time (save.get (i + 1).TIMESTAMP);
                    
                    // This is situation when topic has unlimited time and important
                    // just a time when it was created
                    if (Objects.isNull (expirated) && created.before (nextEntry)) {
                        // BINGO! student was in group when topic was created
                        topics.add (Pair.mp (topicID, 1));
                        continue topicI;
                    // This is situation when topic has expirated time and student
                    // can miss this topic
                    } else if (!Objects.isNull (expirated) && created.before (nextEntry)
                            && expirated.before (move)) {
                        // BINGO! student was in group when topic was created
                        topics.add (Pair.mp (topicID, 2));
                        continue topicI;
                    }
                }
            }
            
            return topics;
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
    
    private static class TopicEntry implements AutoCloseable {
        
        public static final String CLASS_NAME = 
            OrganizationHistory.class.getSimpleName ();
        
        public final int TOPIC_ID;
        
        private File file;
        
        private final ConcurrentMap <Integer, Pair <Time, Time>> 
            PERIODS = new ConcurrentHashMap <> ();
        private final List <String> TASKS = new ArrayList <> ();
        
        public TopicEntry (int topicID) {
            this.TOPIC_ID = topicID;
            _restoreTopicFromFile ();
        }
        
        private void _restoreTopicFromFile () {
            File directory = new File ("topics");
            if (!directory.exists () || !directory.isDirectory ()) {
                FSCrawler.crawl (directory, f -> true, 
                    f -> f.delete (), f -> f.delete ());
                directory.mkdir ();
            }
            
            file = new File (directory, "topic_" + TOPIC_ID + ".bin");
            try {
                if (!file.exists ()) { file.createNewFile (); }
            } catch (IOException ioe) {
                Log.error (CLASS_NAME, ioe);
            }
            
            try (
                InputStream is = new FileInputStream (file);
            ) {
                if (is.available () < 4) { return; }
                byte [] bSize = new byte [4];
                is.read (bSize, 0, bSize.length);
                int tasks = BytesManip.B2I (bSize);
                for (int i = 0; i < tasks; i++) {
                    is.read (bSize, 0, bSize.length);
                    int length = BytesManip.B2I (bSize);
                    byte [] buffer = new byte [length];
                    is.read (buffer, 0, buffer.length);
                    
                    String name = new String (buffer, StandardCharsets.UTF_8);
                    TASKS.add (name);
                }
                
                byte [] bGroupID = new byte [4], bCreated = new byte [8], 
                        bExpired = new byte [8];
                int bytes = bGroupID.length + bCreated.length + bExpired.length;
                while (is.available () >= bytes) {
                    is.read (bGroupID, 0, bGroupID.length);
                    int groupID = BytesManip.B2I (bGroupID);
                    
                    is.read (bCreated, 0, bCreated.length);
                    long created = BytesManip.B2L (bCreated);
                    
                    is.read (bExpired, 0, bExpired.length);
                    long expired = BytesManip.B2L (bExpired);
                    
                    Pair <Time, Time> period = Pair.mp (new Time (created), new Time (expired));
                    PERIODS.put (groupID, period);
                }
            } catch (IOException ioe) {
                Log.error (CLASS_NAME, ioe);
            }
        }
        
        public int getTopicID () {
            return TOPIC_ID;
        }
        
        public Set <Integer> getGroups () {
            return new HashSet <> (PERIODS.keySet ());
        }
        
        // Special time that can't be BEFORE any other time
        private static final Time ENDLESS = new Time (Long.MAX_VALUE);
        
        @SuppressWarnings ("unused")
        public void addToGroup (int groupID) {
            if (PERIODS.containsKey (groupID)) {
                String message = "Topic " + TOPIC_ID + " is already in group " + groupID;
                throw new IllegalStateException (message);
            }
            
            Time time = new Time (System.currentTimeMillis ());
            Pair <Time, Time> period = Pair.mp (time, ENDLESS);
            PERIODS.putIfAbsent (groupID, period); 
            _writeTopicToFile ();
        }
        
        @SuppressWarnings ("unused")
        public void removeFromGroup (int groupID) {
            if (!PERIODS.containsKey (groupID)) {
                String message = "Group " + groupID + " doesn't have topic " + TOPIC_ID;
                throw new IllegalStateException (message);
            }
            
            PERIODS.remove (groupID);
            _writeTopicToFile ();
        }
        
        public Time getCreated (int groupID) {
            if (PERIODS.containsKey (groupID)) {
                return PERIODS.get (groupID).F;
            }
            
            return ENDLESS;
        }
        
        public Time getExpired (int groupID) {
            if (PERIODS.containsKey (groupID)) {
                return PERIODS.get (groupID).S;
            }
            
            return ENDLESS;
        }
        
        @SuppressWarnings ("unused")
        public void setExpired (int groupID, Time time) {
            if (!PERIODS.containsKey (groupID)) { return; }
            Pair <Time, Time> period = PERIODS.get (groupID);
            PERIODS.put (groupID, Pair.mp (period.F, time));
            _writeTopicToFile ();
        }
        
        @SuppressWarnings ("unused")
        public List <String> getTasks () {
            return new ArrayList <> (TASKS);
        }
        
        @SuppressWarnings ("unused")
        public synchronized void addTask (String task) {
            if (Objects.isNull (task) || task.length () == 0) {
                String message = "Name of task can't be emply";
                throw new IllegalArgumentException (message);
            }
            
            this.TASKS.add (task);
        }

        @Override
        public synchronized void close () throws Exception {
            _writeTopicToFile ();
        }
        
        private void _writeTopicToFile () {
            try (
                OutputStream os = new FileOutputStream (file, false);
            ) {
                List <String> tasks = new ArrayList <> (TASKS);
                byte [] bSize = BytesManip.I2B (tasks.size ());
                os.write (bSize, 0, bSize.length);
                
                for (String task : tasks) {
                    byte [] buffer = task.getBytes (StandardCharsets.UTF_8);
                    bSize = BytesManip.I2B (buffer.length);
                    os.write (bSize, 0, bSize.length);
                    os.write (buffer, 0, buffer.length);
                }
                os.flush ();
                
                Set <Integer> keys = PERIODS.keySet ();
                for (int groupID : keys) {
                    Pair <Time, Time> period = PERIODS.get (groupID);
                    byte [] bGroupID = BytesManip.I2B (groupID);
                    os.write (bGroupID, 0, bGroupID.length);
                    
                    byte [] bCreated = BytesManip.L2B (period.F.getTime ());
                    os.write (bCreated, 0, bCreated.length);
                    
                    byte [] bExpired = BytesManip.L2B (period.S.getTime ());
                    os.write (bExpired, 0, bExpired.length);
                }
                
                os.flush ();
            } catch (IOException ioe) {
                Log.error (CLASS_NAME, ioe);
            }
        }
        
    }
    
}
