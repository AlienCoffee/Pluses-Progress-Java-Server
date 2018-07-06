package ru.shemplo.pluses.struct;

import static java.util.Collections.*;
import static java.util.stream.Collectors.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OrganizationTree {
    
    public static enum OrgTreeAction {
        INSERT, DELETE, SELECT
    }
    
    public static enum OrgTreeLayer {
        CLASS, GROUP, STUDENT, TOPIC
    }
    
    private final OrganizationNode ROOT;
    
    public OrganizationTree () {
        this.ROOT = new OrganizationNode ();
    }
    
    @Override
    public String toString () {
        StringBuilder sb = new StringBuilder ();
        sb.append ("Tree for organization 0\n");
        sb.append (ROOT.toString ());
        return sb.toString ();
    }
    
    public OrganizationNode getOrganization () {
        return this.ROOT;
    }
    
    public ClassNode getClass (int id, int age) {
        return (ClassNode) 
            getOrganization ().getChild (id, age);
    }
    
    public GroupNode getGroup (int id, int age, int classID, int classAge) {
        ClassNode clazz = getClass (classID, classAge);
        return (GroupNode) clazz.getChild (id, age);
    }
    
    private static interface Node {
        
        public int getID ();
        
        public Node makeChild (int id);
        
        public List <Node> getChildren (int age);
        
        default 
        public List <Node> getChildren () {
            return getChildren (0);
        }
        
        default
        public Node getChild (int id, int age) {
            for (Node node : getChildren (age)) {
                if (node.getID () == id) {
                    return node;
                }
            }
            
            return null;
        }
        
        default 
        public Node getChild (int id) {
            return getChild (id, 0);
        }
        
        public void commitAll (OrgTreeAction action, List <Integer> ids);
        
        default
        public void commitAll (List <Integer> ids) {
            List <Integer> list = ids.stream ()
                .filter (id -> id < 0).collect (toList ());
            commitAll (OrgTreeAction.DELETE, list);
            
            list = ids.stream ()
                .filter (id -> id > 0).collect (toList ());
            commitAll (OrgTreeAction.INSERT, list);
        }
        
        default
        public void commit (OrgTreeAction action, int id) {
            List <Integer> list = new ArrayList <> ();
            list.add (id); commitAll (action, list);
        }
        
    }
    
    private abstract static class AbsNode <T> implements Node {
        
        protected final List <VNode <T>> VERSIONS;
        
        protected final int ID;
        
        public AbsNode (int id) {
            this.VERSIONS = new ArrayList <> ();
            this.ID = id;
        }
        
        @Override
        public String toString () {
            StringBuilder sb = new StringBuilder ();
            sb.append ("> ");
            sb.append (this.getClass ().getSimpleName ());
            sb.append (" ");
            sb.append (getID ());
            sb.append ("\n");
            for (Node node : getChildren ()) {
                sb.append (">> ");
                sb.append (node.toString ());
                sb.append ("\n");
            }
            
            return sb.toString ();
        }
        
        public int getID () {
            return ID;
        }
        
        @Override
        public List <Node> getChildren (int age) {
            if (VERSIONS.isEmpty ()) {
                return new ArrayList <> ();
            }
            
            int index = VERSIONS.size () - age - 1;
            VNode <T> version = VERSIONS.get (index);
            
            @SuppressWarnings ("unchecked")
            List <Node> children = 
                (List <Node>) version.getChildren ();
            return children;
        }
        
        @Override
        public void commitAll (OrgTreeAction action, List <Integer> ids) {
            Set <Integer> income = new HashSet <> (ids);
            List <Node> updated = new ArrayList <> ();
            List <Node> current = getChildren ();
            if (OrgTreeAction.INSERT.equals (action)) {
                current.stream ().forEach (n -> income.remove (n.getID ()));
                updated.addAll (current);
                
                List <Node> extra = income.stream ()
                    .map (this::makeChild).collect (toList ());
                updated.addAll (extra);
            } else if (OrgTreeAction.DELETE.equals (action)) {
                updated = current.stream ().filter (
                    n -> !income.contains (n.getID ())
                ).collect (toList ());
            }
            
            @SuppressWarnings ("unchecked")
            VNode <T> node = (VNode <T>) new VNode <> (updated);
            VERSIONS.add (node);
        }
        
    }
    
    private static class VNode <T> {
        
        private final List <T> REFERENCES;
        
        public VNode (List <T> refs) {
            this.REFERENCES = unmodifiableList (refs);
        }
        
        public List <T> getChildren () {
            return REFERENCES;
        }
        
    }
    
    public static class OrganizationNode extends AbsNode <ClassNode> {
        
        public OrganizationNode () {
            super (0);
        }
        
        @Override
        public Node makeChild (int id) {
            return new ClassNode (id);
        }
        
    }
    
    public static class ClassNode extends AbsNode <GroupNode> {

        public ClassNode (int id) {
            super (id);
        }

        @Override
        public Node makeChild (int id) {
            return new GroupNode (id);
        }
        
    }
    
    public static class GroupNode extends AbsNode <TopicNode> {

        private final List <VNode <StudentNode>> STUDENTS;
        
        public GroupNode (int id) {
            super (id);
            
            this.STUDENTS = new ArrayList <> ();
        }

        @Override
        public Node makeChild (int id) {
            return new TopicNode (id);
        }
        
        public void commitOnStudents (OrgTreeAction action, List <Integer> ids) {
            List <StudentNode> list = 
                STUDENTS.get (STUDENTS.size () - 1).getChildren ();
            List <StudentNode> updated = new ArrayList <> ();
            Set <Integer> income = new HashSet <> (ids);
            if (OrgTreeAction.INSERT.equals (action)) {
                list.stream ().forEach (n -> income.remove (n.getID ()));
                updated.addAll (list);
                
                List <StudentNode> extra = income.stream ()
                    .map (StudentNode::new).collect (toList ());
                updated.addAll (extra);
            } else if (OrgTreeAction.DELETE.equals (action)) {
                updated = list.stream ().filter (
                    n -> !income.contains (n.getID ())
                ).collect (toList ());
            }
            
            VNode <StudentNode> node = new VNode <> (updated);
            STUDENTS.add (node);
        }
        
        public void commitOnStudents (OrgTreeAction action, int id) {
            List <Integer> list = new ArrayList <> (); list.add (id); 
            commitOnStudents (action, list);
        }
        
    }
    
    public static class StudentNode extends AbsNode <StudentNode> {
        
        public StudentNode (int id) {
            super (id);
        }

        @Override
        public Node makeChild (int id) {
            return this;
        }
        
    }
    
    public static class TopicNode extends AbsNode <TopicNode> {

        public TopicNode (int id) {
            super (id);
        }

        @Override
        public Node makeChild (int id) {
            return this;
        }
        
    }
    
}
