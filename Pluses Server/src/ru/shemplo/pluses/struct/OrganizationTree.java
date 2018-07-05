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
    
    public OrganizationTree () {
        
    }
    
    private static interface Node {
        
        public int getID ();
        
        public Node makeChild (int id);
        
        public List <Node> getChildren (int age);
        
        default public List <Node> getChildren () {
            return getChildren (0);
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
        
        public int getID () {
            return ID;
        }
        
        @Override
        public List <Node> getChildren (int age) {
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
        
        public OrganizationNode (int id) {
            super (id);
        }
        
        @Override
        public Node makeChild (int id) {
            return null;
        }
        
    }
    
    public static class ClassNode {
        
    }
    
}
