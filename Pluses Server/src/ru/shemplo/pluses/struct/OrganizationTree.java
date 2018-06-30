package ru.shemplo.pluses.struct;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class OrganizationTree {
	
	public static enum ModifyAction {
		INSERT, DELETE, IMPULSE
	}
	
	public static OrganizationTree buildFromFile (File backup) {
		return null;
	}
	
	private final RootNode ROOT;
	
	public OrganizationTree () {
		this.ROOT = new RootNode ();
	}
	
	public void writeToFile () {
		File backup = new File ("backup.bin");
		
		try (
			OutputStream os = new FileOutputStream (backup);
		) {
			
		} catch (IOException ioe) {
			ioe.printStackTrace ();
		}
	}
	
	public RootNode root () {
		return ROOT;
	}
	
	/* METHODS FOR THE LATEST VERSION */
	
	public List <ClassNode> getClasses () {
		return ROOT.classes ();
	}
	
	public ClassNode getClass (int index) {
		return getClasses ().get (index);
	}
	
	public List <GroupNode> getGroups (int classIndex) {
		return getClass (classIndex).groups ();
	}
	
	public GroupNode getGroup (int classIndex, int groupIndex) {
		return getGroups (classIndex).get (groupIndex);
	}
	
	public List <StudentNode> getStudents (int classIndex, int groupIndex) {
		return getGroup (classIndex, groupIndex).students ();
	}
	
	/* SUPPORT CLASSES OF NODES */
	
	public static final class RootNode {
		
		private final List <VersionNode <ClassNode>> CLASSES;
		
		public RootNode () {
			this.CLASSES = new ArrayList <> ();
		}
		
		public int getVersionsNumber () {
			return CLASSES.size ();
		}
		
		public void modify (ModifyAction action, int classID) {
			List <ClassNode> updated = new ArrayList <> (classes ());
			if (ModifyAction.INSERT.equals (action)) {
				boolean canInsert = true;
				for (int i = 0; i < updated.size () && canInsert; i++) {
					canInsert &= updated.get (i).CLASS_ID != classID;
				}
				
				if (canInsert) {
					updated.add (new ClassNode (classID));
					
					long timestamp = System.currentTimeMillis ();
					CLASSES.add (new VersionNode <> (timestamp, updated));
				}
			} else if (ModifyAction.DELETE.equals (action)) {
				int beforeSize = updated.size ();
				for (int i = 0; i < updated.size (); i++) {
					if (updated.get (i).CLASS_ID == classID) {
						updated.remove (i);
						break;
					}
				}
				
				if (beforeSize != updated.size ()) {
					long timestamp = System.currentTimeMillis ();
					CLASSES.add (new VersionNode <> (timestamp, updated));
				}
			}
		}
		
		public List <ClassNode> classes () {
			if (CLASSES.isEmpty ()) { return new ArrayList <> (); }
			return CLASSES.get (CLASSES.size () - 1).refrences ();
		}
		
	}
	
	public static final class VersionNode <T> {
		
		private final List <T> REFRENCES;
		
		public final long TIMESTAMP;
		
		public VersionNode (long timestamp, List <T> values) {
			this.REFRENCES = Collections.unmodifiableList (values);
			this.TIMESTAMP = timestamp;
		}
		
		public List <T> refrences () {
			return REFRENCES;
		}
		
	}
	
	public static final class ClassNode {
	
		private final List <VersionNode <GroupNode>> GROUPS;
		
		public final int CLASS_ID;
		
		public ClassNode (int classID) {
			this.GROUPS = new ArrayList <> ();
			this.CLASS_ID = classID;
		}
		
		public void modify (ModifyAction action, int groupID) {
			List <GroupNode> updated = new ArrayList <> (groups ());
			if (ModifyAction.INSERT.equals (action)) {
				boolean canInsert = true;
				for (int i = 0; i < updated.size () && canInsert; i++) {
					canInsert &= updated.get (i).GROUP_ID != groupID;
				}
				
				if (canInsert) {
					updated.add (new GroupNode (groupID));
					
					long timestamp = System.currentTimeMillis ();
					GROUPS.add (new VersionNode <> (timestamp, updated));
				}
			} else if (ModifyAction.DELETE.equals (action)) {
				int beforeSize = updated.size ();
				for (int i = 0; i < updated.size (); i++) {
					if (updated.get (i).GROUP_ID == groupID) {
						updated.remove (i);
						break;
					}
				}
				
				if (beforeSize != updated.size ()) {
					long timestamp = System.currentTimeMillis ();
					GROUPS.add (new VersionNode <> (timestamp, updated));
				}
			}
		}
		
		public List <GroupNode> groups () {
			if (GROUPS.isEmpty ()) { return new ArrayList <> (); }
			return GROUPS.get (GROUPS.size () - 1).refrences ();
		}
		
	}
	
	public static final class GroupNode {
		
		private final List <VersionNode <StudentNode>> STUDENTS;
		private final List <VersionNode <TopicNode>> TOPICS;
		
		public final int GROUP_ID;
		
		public GroupNode (int groupID) {
			this.STUDENTS = new ArrayList <> ();
			this.TOPICS = new ArrayList <> ();
			this.GROUP_ID = groupID;
		}
		
		public void modifyStrudents (ModifyAction action, int studentID) {
			List <StudentNode> updated = new ArrayList <> (students ());
			if (ModifyAction.INSERT.equals (action)) {
				boolean canInsert = true;
				for (int i = 0; i < updated.size () && canInsert; i++) {
					canInsert &= updated.get (i).STUDENT_ID != studentID;
				}
				
				if (canInsert) {
					updated.add (new StudentNode (studentID));
					
					long timestamp = System.currentTimeMillis ();
					STUDENTS.add (new VersionNode <> (timestamp, updated));
				}
			} else if (ModifyAction.DELETE.equals (action)) {
				int beforeSize = updated.size ();
				for (int i = 0; i < updated.size (); i++) {
					if (updated.get (i).STUDENT_ID == studentID) {
						updated.remove (i);
						break;
					}
				}
				
				if (beforeSize != updated.size ()) {
					long timestamp = System.currentTimeMillis ();
					STUDENTS.add (new VersionNode <> (timestamp, updated));
				}
			}
		}
		
		public List <StudentNode> students () {
			if (STUDENTS.isEmpty ()) { return new ArrayList <> (); }
			return STUDENTS.get (STUDENTS.size () - 1).refrences ();
		}
		
		public void modifyTopics (ModifyAction action, int topicID) {
			List <TopicNode> updated = new ArrayList <> (topics ());
			if (ModifyAction.INSERT.equals (action)) {
				boolean canInsert = true;
				for (int i = 0; i < updated.size () && canInsert; i++) {
					canInsert &= updated.get (i).TOPIC_ID != topicID;
				}
				
				if (canInsert) {
					updated.add (new TopicNode (topicID));
					
					long timestamp = System.currentTimeMillis ();
					TOPICS.add (new VersionNode <> (timestamp, updated));
				}
			} else if (ModifyAction.DELETE.equals (action)) {
				int beforeSize = updated.size ();
				for (int i = 0; i < updated.size (); i++) {
					if (updated.get (i).TOPIC_ID == topicID) {
						updated.remove (i);
						break;
					}
				}
				
				if (beforeSize != updated.size ()) {
					long timestamp = System.currentTimeMillis ();
					TOPICS.add (new VersionNode <> (timestamp, updated));
				}
			}
		}
		
		public List <TopicNode> topics () {
			if (TOPICS.isEmpty ()) { return new ArrayList <> (); }
			return TOPICS.get (TOPICS.size () - 1).refrences ();
		}
		
	}
	
	public static final class StudentNode {
		
		private final int STUDENT_ID;
		
		public StudentNode (int studentID) {
			this.STUDENT_ID = studentID;
		}
		
	}
	
	public static final class TopicNode {
		
		private final List <String> TASKS;
		private List <File> topicFiles;
		
		public final int  TOPIC_ID;
		
		public TopicNode (int topicID) {
			this.TASKS = new ArrayList <> ();
			this.TOPIC_ID = topicID;
		}
		
	}
	
}
