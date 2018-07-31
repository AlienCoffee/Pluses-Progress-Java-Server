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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    private static final ConcurrentMap <Integer, GroupEntry>
        GROUPS = new ConcurrentHashMap <> ();
    
    public synchronized static void init () {
        STUDENTS.clear (); TOPICS.clear ();
        
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
            
            query = "SELECT * FROM `groups` ORDER BY `id` ASC";
            statement = mysql.createStatement ();
            answer    = statement.executeQuery (query);
            while (answer.next ()) {
                int groupID = answer.getInt ("id");
                
                GroupEntry group = new GroupEntry (groupID);
                GROUPS.put (group.getGroupID (), group);
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
                
                Calendar calendar = Calendar.getInstance ();
                Timestamp timestamp = answer.getTimestamp ("time", calendar);
                if (!STUDENTS.containsKey (student)) { continue; }
                
                // Here try/catch is not necessary because it's just single thread
                STUDENTS.get (student).addMovement (from, to, timestamp);
            }
            
            query = "SELECT `id` FROM `topics` ORDER BY `id` ASC";
            statement = mysql.createStatement ();
            answer    = statement.executeQuery (query);
            while (answer.next ()) {
                int topicID = answer.getInt ("id");
                
                TopicEntry topic = new TopicEntry (topicID);
                TOPICS.put (topic.getTopicID (), topic);
            }
            
            query = "SELECT * FROM `tries` ORDER BY `time` ASC";
            statement = mysql.createStatement ();
            answer    = statement.executeQuery (query);
            while (answer.next ()) {
                int studentID = answer.getInt ("student");
                StudentHistory student = STUDENTS.get (studentID);
                
                int teacherID = answer.getInt ("teacher");
                int groupID = answer.getInt ("group");
                int topicID = answer.getInt ("topic");
                int taskID = answer.getInt ("task");
                boolean verdict = answer.getInt ("verdict") == 1;
                
                Calendar calendar = Calendar.getInstance ();
                Timestamp timestamp = answer.getTimestamp ("time", calendar);
                student.addTry (groupID, topicID, taskID, teacherID, verdict, timestamp);
            }
        } catch (SQLException sqle) {
            sqle.printStackTrace ();
        }
    }
    
    //////////////////
    // ------------ //
    // FOR STUDENTS //
    // ------------ //
    //////////////////
    
    public static boolean existsStudent (int studentID) {
        return STUDENTS.containsKey (studentID);
    }
    
    /**
     * <p>
     * <b>!! WARNING !!</b>
     * Don't call this method if you are not sure what you're doing.
     * The consequences of calling this method can be fatal for
     * current start of server. Correct state can be restored only
     * after calling method {@link #init() init} (or after restart)
     * </p>
     * 
     * @param studentID
     * 
     */
    public static void createStudent (int studentID) {
        if (existsStudent (studentID)) {
            String message = "Student " + studentID + " is already created";
            throw new IllegalStateException (message);
        }
        
        // Adding instance of new student to control structure
        STUDENTS.putIfAbsent (studentID, new StudentHistory (studentID));
    }
    
    public static List <Pair <Integer, Integer>> getStudents (Map <String, String> params) {
        List <Pair <Integer, Integer>> students = new ArrayList <> ();
        if (params.containsKey ("id")) {
            int groupID = Integer.parseInt (params.get ("id"));
            if (!existsGroup (groupID)) {
                String message = "Group " + groupID + " doesn't exist";
                throw new IllegalStateException (message);
            }
            
            students.addAll (GROUPS.get (groupID).getStudents ());
        } else {
            Set <Integer> tmp = new HashSet <> (STUDENTS.keySet ());
            for (int student : tmp) {
                // Here is not important the visibility of student
                students.add (Pair.mp (student, 0));
            }
        }
        
        students.sort ((a, b) -> Integer.compare (a.F, b.F));
        return students;
    }
    
    public static void insertStudent (int studentID, int groupID, Timestamp timestamp) {
        if (!existsStudent (studentID) || !existsGroup (groupID)) {
            String message = "Student " + studentID + " or group " 
                             + groupID + " doesn't exist";
            throw new IllegalStateException (message);
        }
        
        StudentHistory student = STUDENTS.get (studentID);
        student.addMovement (null, groupID, timestamp);
    }
    
    public static void moveStudent (int studentID, Integer fromID, 
            Integer toID, Timestamp timestamp) {
        if (!existsStudent (studentID) || !existsGroup (toID)) {
            String message = "Student " + studentID + " or group " 
                             + toID + " doesn't exist";
            throw new IllegalStateException (message);
        }
        
        StudentHistory student = STUDENTS.get (studentID);
        student.addMovement (fromID, toID, timestamp);
    }
    
    public static void insertTry (int groupID, int studentID, int topicID, 
            int taskID, int teacherID, boolean verdict, Timestamp timestamp) {
        if (!existsGroup (groupID)) {
            String message = "Group " + groupID + " doesn't exist";
            throw new IllegalStateException (message);
        }
        
        if (!existsStudent (studentID)) {
            String message = "Student " + studentID + " doesn't exist";
            throw new IllegalStateException (message);
        }
        
        if (!existsTopic (topicID)) {
            String message = "Topic " + topicID + " doesn't exist";
            throw new IllegalStateException (message);
        }
        
        StudentHistory student = STUDENTS.get (studentID);
        
        // STRICT AREA // IF IT'S NECESSARY THEN COMMENT //
        int currentGroupID = student.getGroups ().F;
        if (currentGroupID != groupID) {
            String message = "Student " + studentID + " now in group " + currentGroupID
                    + " (request was for group " + groupID + ")";
            throw new IllegalStateException (message);
        }
        
        List <Pair <Integer, Integer>> topics = student.getTopics ();
        boolean isTopicCorrect = false;
        for (int i = 0; i < topics.size (); i++) {
            if (topics.get (i).F == topicID 
                && topics.get (i).S == 0) {
                isTopicCorrect = true; 
                break;
            }
        }
        
        if (!isTopicCorrect) {
            String message = "Student " + studentID + " doesn't know topic " + topicID;
            throw new IllegalStateException (message);
        }
        
        List <Pair <Integer, String>> tasks = TOPICS.get (topicID).getTasks ();
        boolean isTaskCorrect = false;
        for (int i = 0; i < tasks.size (); i++) {
            
        }
        
        if (!isTaskCorrect) {
            String message = "Task " + taskID + " doesn't exist in topic " + topicID;
            throw new IllegalStateException (message);
        }
        // END OF STRICT AREA // TILL THIS LINE NECCESSARY TO COMMENT //
        
        student.addTry (groupID, topicID, taskID, teacherID, verdict, timestamp);
    }
    
    ////////////////
    // ---------- //
    // FOR TOPICS //
    // ---------- //
    ////////////////
    
    public static boolean existsTopic (int topicID) {
        return TOPICS.containsKey (topicID);
    }
    
    /**
     * <p>
     * <b>!! WARNING !!</b>
     * Don't call this method if you are not sure what you're doing.
     * The consequences of calling this method can be fatal for
     * current start of server. Correct state can be restored only
     * after calling method {@link #init() init} (or after restart)
     * </p>
     * 
     * @param topicID
     * 
     */
    public static void createTopic (int topicID) {
        if (existsTopic (topicID)) {
            String message = "Topic " + topicID + " is already created";
            throw new IllegalStateException (message);
        }
        
        // Adding instance of new student to control structure
        TOPICS.putIfAbsent (topicID, new TopicEntry (topicID));
    }
    
    public static void insertTopic (int topicID, int groupID, long timestamp) {
        if (!existsTopic (topicID) || !existsGroup (groupID)) {
            String message = "Topic " + topicID + " or group " 
                             + groupID + " doesn't exist";
            throw new IllegalStateException (message);
        }
        
        TOPICS.get (topicID).addToGroup (groupID);
    }
    
    public static List <Pair <Integer, Integer>> getToics (Map <String, String> params) {
        List <Pair <Integer, Integer>> topics = new ArrayList <> ();
        if (params.containsKey ("id")) {
            int studentID = Integer.parseInt (params.get ("id"));
            if (!existsStudent (studentID)) {
                String message = "Student " + studentID + " doesn't exist";
                throw new IllegalStateException (message);
            }
            
            topics.addAll (STUDENTS.get (studentID).getTopics ());  
        } else {
            Set <Integer> tmp = new HashSet <> (TOPICS.keySet ());
            for (int topic : tmp) {
                // Here is not important the visibility of topic
                topics.add (Pair.mp (topic, 0));
            }
        }
        
        topics.sort ((a, b) -> Integer.compare (a.F, b.F));
        return topics;
    }
    
    public static List <Pair <Integer, Integer>> getToics () {
        return getToics (new HashMap <> ());
    }
    
    public static void createTask (int topicID, String title) {
        if (!existsTopic (topicID)) {
            String message = "Topic " + topicID + " doesn't exist";
            throw new IllegalStateException (message);
        }
        
        TopicEntry topic = TOPICS.get (topicID);
        topic.createTask (title);
    }
    
    public static void renameTask (int topicID, int taskID, String title) {
        if (!existsTopic (topicID)) {
            String message = "Topic " + topicID + " doesn't exist";
            throw new IllegalStateException (message);
        }
        
        TopicEntry topic = TOPICS.get (topicID);
        topic.renameTask (taskID, title);
    }
    
    public static List <Pair <Integer, String>> getTasks (int topicID) {
        if (!existsTopic (topicID)) {
            String message = "Topic " + topicID + " doesn't exist";
            throw new IllegalStateException (message);
        }
        
        TopicEntry topic = TOPICS.get (topicID);
        return topic.getTasks ();
    }
    
    ////////////////
    // ---------- //
    // FOR GROUPS //
    // ---------- //
    ////////////////
    
    public static boolean existsGroup (int groupID) {
        return GROUPS.containsKey (groupID);
    }
    
    /**
     * <p>
     * <b>!! WARNING !!</b>
     * Don't call this method if you are not sure what you're doing.
     * The consequences of calling this method can be fatal for
     * current start of server. Correct state can be restored only
     * after calling method {@link #init() init} (or after restart)
     * </p>
     * 
     * @param groupID
     * 
     */
    public static void createGroup (int groupID) {
        if (existsGroup (groupID)) {
            String message = "Group " + groupID + " is already created";
            throw new IllegalStateException (message);
        }
        
        // Adding instance of new student to control structure
        GROUPS.putIfAbsent (groupID, new GroupEntry (groupID));
    }
    
    public static List <Integer> getGroups (Map <String, String> params) {
        List <Integer> groups = new ArrayList <> (GROUPS.keySet ());
        groups.sort (Integer::compare);
        return groups;
    }
    
    public static List <Integer> getStudentGroups (int studentID) {
        if (!existsStudent (studentID)) {
            String message = "Student " + studentID + " doesn't exist";
            throw new IllegalStateException (message);
        }
        
        Pair <Integer, Set <Integer>> 
            tmp = STUDENTS.get (studentID).getGroups ();
        tmp.S.remove (null);
        
        List <Integer> groups = new ArrayList <> ();
        groups.add (tmp.F); groups.addAll (tmp.S);
        
        return groups;
    }
    
    public static List <Integer> getGroups () {
        return getGroups (new HashMap <> ());
    }
    
    ///////////////////////////////////////////////////////////
    // ----------------------------------------------------- //
    ///////////////////////////////////////////////////////////
    
    public synchronized static void close () throws Exception {
        for (TopicEntry entry : TOPICS.values ()) {
            entry.close (); // Closing restore files
        }
    }
    
    // Support classes //
    
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
            ENTRIES.add (new StudentEntry (null, new Timestamp (0)));
            
            // -------------------------------------
            this.PROGRESS = new ConcurrentHashMap <> ();
        }
        
        public int getStudentID () {
            return STUDENT_ID;
        }
        
        public Pair <Integer, Set <Integer>> getGroups () {
            Set <Integer> groups = new HashSet <> ();
            
            int size = ENTRIES.size (); // To prevent data-race
            for (int i = 0; i < size - 1; i++) {
                StudentEntry entry = ENTRIES.get (i);
                groups.add (entry.GROUP);
            }

            groups.remove (null);
            return Pair.mp (ENTRIES.get (size - 1).GROUP, groups);
        }
        
        public void addMovement (Integer from, Integer to, Timestamp timestamp)
                throws NoSuchElementException {
            // Critical zone just for one thread
            // Value -1 is necessary for preventing situation when come
            // two requests (a -> b) and (b -> c) in a row, but first
            // wasn't completed
            // The value -1 is impossible to be the id of group in MySQL
            // it can be broken just in case of bad request (! WARNING !)
            if (CURRENT.compareAndSet (from, -1)) {
                StudentEntry last = ENTRIES.get (ENTRIES.size () - 1);
                ENTRIES.add (new StudentEntry (to, timestamp));
                if (last.TIMESTAMP.after (timestamp)) {
                    ENTRIES.sort ((a, b) -> a.TIMESTAMP.compareTo (b.TIMESTAMP));
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
        
        public List <Pair <Integer, Integer>> getTopics () {
            List <Pair <Integer, Integer>> topics = new ArrayList <> ();
            List <StudentEntry> save = new ArrayList <> (ENTRIES);
            
            // Guaranteed that list of entries is not empty
            StudentEntry last = save.get (save.size () - 1);
            
            Set <Integer> keys = TOPICS.keySet ();
            topicI: for (Integer topicID : keys) {
                TopicEntry topic = TOPICS.get (topicID);
                Set <Integer> groups = topic.getGroups ();
                
                if (groups.contains (last.GROUP)) {
                    // This topic is inserted to this group
                    // That's why if student is in some group, so ->
                    // all topics of such group must be related to him
                    topics.add (Pair.mp (topicID, 0));
                } else {
                    // Here is topics that was available to student
                    // when he/she was in another group(s)
                    for (int i = 0; i < ENTRIES.size () - 1; i++) {
                        StudentEntry student = ENTRIES.get (i);
                        if (Objects.isNull (student.GROUP)) {
                            // This is a period when student was
                            // only added to system and wasn't
                            // inserted to some group
                            continue;
                        }
                        
                        int groupID = student.GROUP;
                        Timestamp created = topic.getCreated (groupID),
                                  expired = topic.getExpired (groupID);
                        Timestamp nextMove = ENTRIES.get (i + 1).TIMESTAMP;
                        
                        if (student.TIMESTAMP.after (expired) || nextMove.before (created)) {
                            // These are cases when student didn't get information about this topic
                            // 
                            // Schema presentation:
                            // Time line   |------------------------------------------------------------------>|
                            // User group  |                       ... | group N | ...                         |
                            // Bad example |  ... | topic T1 | ...                       ... | topic T2 | ...  |
                            // 
                            // In this case if student in `group N` then he can't see topics T1 and T2
                            continue;
                        }
                        
                        topics.add (Pair.mp (topicID, 1));
                        continue topicI;
                    }
                }
            }
            
            return topics;
        }
        
        private static class StudentEntry {
            
            public final Timestamp TIMESTAMP;
            public final Integer GROUP;
            
            public StudentEntry (Integer group, Timestamp timestamp) {
                this.TIMESTAMP = timestamp;
                this.GROUP = group;
            }
            
        }
        
        private final ConcurrentMap <Trio <Integer, Integer, Integer>, TaskProgress> PROGRESS;
        
        public void addTry (int groupID, int topicID, int taskID,
                int teacherID, boolean verdict, Timestamp timestamp) {
            Trio <Integer, Integer, Integer> key = Trio.mt (groupID, topicID, taskID);
            PROGRESS.putIfAbsent (key, new TaskProgress (groupID, topicID, taskID));
            PROGRESS.get (key).addTry (teacherID, verdict, timestamp);
        }
        
        private class TaskProgress {
            
            @SuppressWarnings ("unused")
            public final int GROUP_ID, TOPIC_ID, TASK_ID;
            
            private final List <Trio <Integer, Boolean, Timestamp>> 
                TRIES = new ArrayList <> ();
            
            public TaskProgress (int groupID, int topicID, int taskID) {
                this.GROUP_ID = groupID;
                this.TOPIC_ID = topicID;
                this.TASK_ID = taskID;
            }
            
            public synchronized void addTry (int teacherID, 
                    boolean verdict, Timestamp time) {
                TRIES.add (Trio.mt (teacherID, verdict, time));
                if (TRIES.size () > 1) {
                    Trio <?, ?, Timestamp> prev = TRIES.get (TRIES.size () - 2);
                    if (time.before (prev.T)) {
                        TRIES.sort ((a, b) -> -a.T.compareTo (b.T));
                    }
                }
            }
            
            @SuppressWarnings ("unused")
            public synchronized int getVerdict () {
                int verdict = 0;
                for (int i = 0; i < TRIES.size (); i++) {
                    if (!TRIES.get (i).S) { 
                        verdict = -Math.abs (verdict) - 1; 
                    } else { 
                        verdict = Math.abs (verdict);
                    }
                }
                
                return verdict;
            }
            
        }
        
    }
    
    private static class TopicEntry implements AutoCloseable {
        
        public static final String CLASS_NAME = 
            TopicEntry.class.getSimpleName ();
        
        public final int TOPIC_ID;
        
        private File file;
        
        private final ConcurrentMap <Integer, Pair <Timestamp, Timestamp>> 
            PERIODS = new ConcurrentHashMap <> ();
        private final List <Pair <Integer, String>> 
            TASKS = new ArrayList <> ();
        
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
                    
                    is.read (bSize, 0, bSize.length);
                    int id = BytesManip.B2I (bSize);
                    
                    byte [] buffer = new byte [length - 4];
                    is.read (buffer, 0, buffer.length);
                    
                    String name = new String (buffer, StandardCharsets.UTF_8);
                    TASKS.add (Pair.mp (id, name));
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
                    
                    Pair <Timestamp, Timestamp> period = 
                        Pair.mp (new Timestamp (created), new Timestamp (expired));
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
        private static final Timestamp ENDLESS = new Timestamp (Long.MAX_VALUE);
        
        public void addToGroup (int groupID) {
            if (PERIODS.containsKey (groupID)) {
                String message = "Topic " + TOPIC_ID + " is already in group " + groupID;
                throw new IllegalStateException (message);
            }
            
            Timestamp time = new Timestamp (System.currentTimeMillis ());
            Pair <Timestamp, Timestamp> period = Pair.mp (time, ENDLESS);
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
        
        public Timestamp getCreated (int groupID) {
            if (PERIODS.containsKey (groupID)) {
                return PERIODS.get (groupID).F;
            }
            
            return ENDLESS;
        }
        
        public Timestamp getExpired (int groupID) {
            if (PERIODS.containsKey (groupID)) {
                return PERIODS.get (groupID).S;
            }
            
            return ENDLESS;
        }
        
        @SuppressWarnings ("unused")
        public void setExpired (int groupID, Timestamp time) {
            if (!PERIODS.containsKey (groupID)) { return; }
            Pair <Timestamp, Timestamp> period = PERIODS.get (groupID);
            PERIODS.put (groupID, Pair.mp (period.F, time));
            _writeTopicToFile ();
        }
        
        public List <Pair <Integer, String>> getTasks () {
            return new ArrayList <> (TASKS);
        }
        
        public synchronized void createTask (String task) {
            if (Objects.isNull (task) || task.length () == 0) {
                String message = "Name of task can't be emply";
                throw new IllegalArgumentException (message);
            }
            
            int id = TASKS.stream ().mapToInt (p -> p.F)
                          .max ().getAsInt () + 1;
            this.TASKS.add (Pair.mp (id, task));
            _writeTopicToFile ();
        }
        
        public void renameTask (int taskID, String title) {
            int size = TASKS.size (); // to prevent concurrent
            for (int i = 0; i < size; i++) {
                Pair <Integer, String> task = TASKS.get (i);
                if (task.F != taskID) { continue; }
                
                synchronized (task) {
                    TASKS.set (i, Pair.mp (task.F, title));
                }
                
                _writeTopicToFile ();
                return;
            }
            
            String message = 
                "Topic " + TOPIC_ID + " doesn't have task " + taskID;
            throw new IllegalStateException (message);
        }

        @Override
        public synchronized void close () throws Exception {
            _writeTopicToFile ();
        }
        
        private void _writeTopicToFile () {
            try (
                OutputStream os = new FileOutputStream (file, false);
            ) {
                List <Pair <Integer, String>> tasks = new ArrayList <> (TASKS);
                byte [] bSize = BytesManip.I2B (tasks.size ());
                os.write (bSize, 0, bSize.length);
                
                for (Pair <Integer, String> task : tasks) {
                    byte [] buffer = task.S.getBytes (StandardCharsets.UTF_8);
                    bSize = BytesManip.I2B (buffer.length + 4);
                    os.write (bSize, 0, bSize.length);
                    
                    byte [] bID = BytesManip.I2B (task.F);
                    os.write (bID, 0, bID.length);        // 4 bytes for id
                    os.write (buffer, 0, buffer.length);  // other bytes for name
                }
                os.flush ();
                
                Set <Integer> keys = PERIODS.keySet ();
                for (int groupID : keys) {
                    Pair <Timestamp, Timestamp> period = PERIODS.get (groupID);
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
    
    private static class GroupEntry {
        
        @SuppressWarnings ("unused")
        public static final String CLASS_NAME = 
            GroupEntry.class.getSimpleName ();
            
        public final int GROUP_ID;
        
        public GroupEntry (int groupID) {
            this.GROUP_ID = groupID;
        }
        
        public Collection <Pair <Integer, Integer>> getStudents () {
            List <Pair <Integer, Integer>> students = new ArrayList <> ();
            Set <Integer> save = new HashSet <> (STUDENTS.keySet ());
            for (Integer studentID : save) {
                StudentHistory student = STUDENTS.get (studentID);
                Pair <Integer, Set <Integer>> pair = student.getGroups ();
                if (Objects.isNull (pair.F)) { continue; }
                
                if (Integer.compare (GROUP_ID, pair.F) == 0) {
                    // Visibility 0 means that it's current state
                    students.add (Pair.mp (studentID, 0));
                } else if (pair.S.contains (GROUP_ID)) {
                    // Visibility 1 means that was some time ago
                    students.add (Pair.mp (studentID, 1));
                }
            }
            
            return students;
        }

        public int getGroupID () {
            return GROUP_ID;
        }
        
    }
    
}
