package ru.shemplo.pluses.struct;

import static java.util.Collections.*;

import java.util.ArrayList;
import java.util.List;

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
