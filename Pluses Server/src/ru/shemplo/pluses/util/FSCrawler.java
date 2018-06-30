package ru.shemplo.pluses.util;

import java.io.File;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class FSCrawler {

	public static void crawl (File from, Predicate <File> visit, 
			Consumer <File> onFile, Consumer <File> onDirectory) {
		if (Objects.isNull (from) || !from.exists ()) { return; }
		
		if (from.isDirectory ()) {
			for (File child : from.listFiles ()) {
				if (!visit.test (child)) { continue; }
				crawl (child, visit, onFile, onDirectory);
			}
			
			onDirectory.accept (from);
		} else {
			onFile.accept (from);
		}
	}
	
}
