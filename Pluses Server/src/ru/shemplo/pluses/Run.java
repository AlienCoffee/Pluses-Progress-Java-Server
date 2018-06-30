package ru.shemplo.pluses;

import static java.util.Objects.*;

import ru.shemplo.pluses.config.Configuration;
import ru.shemplo.pluses.log.Log;
import ru.shemplo.pluses.struct.OrganizationTree;
import ru.shemplo.pluses.struct.OrganizationTree.ClassNode;
import ru.shemplo.pluses.struct.OrganizationTree.ModifyAction;

public class Run {

	public static final String CONFIG_NAME = "config.ini";
	
	public static int exitCode = 1;
	
	public static void main (String ... args) {
		Configuration.load (CONFIG_NAME);
		
		OrganizationTree tree = new OrganizationTree ();
		for (int i = 0; i < 11; i ++) {
			tree.root ().modify (ModifyAction.INSERT, i + 5);
			for (int j = 0; j < 5; j++) {
				ClassNode node = tree.root ().classes ().get (i);
				node.modify (ModifyAction.INSERT, j + 50);
				for (int k = 0; k < 25; k++) {
					tree.root ().classes ().get (i).groups ().get (j)
						.modifyStrudents (ModifyAction.INSERT, k + 500);
				}
				
				for (int k = 0; k < 20; k++) {
					tree.root ().classes ().get (i).groups ().get (j)
						.modifyTopics (ModifyAction.INSERT, k + 5000);
				}
			}
		}
		
		tree.writeToFile ();
		//new Thread (() -> { while (true) {} }).start ();
	}
	
	public static void stopApplication (int code, String comment) {
		if (code == 0) {
			String message = isNull (comment) || comment.length () == 0
							 ? "Application stopped normally"
							 : "Normal stop: " + comment;
			System.out.println (message);
		} else {
			String message = isNull (comment) || comment.length () == 0
							 ? "Application stopped with uncommented reason"
							 : "Fatal stop (" + code + "): " + comment;
			System.out.flush ();
			System.err.println (message);
			System.err.flush ();
		}
		
		Log.close ();
		System.exit (code);
	}
	
}
