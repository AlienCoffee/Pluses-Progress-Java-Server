package ru.shemplo.pluses.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import ru.shemplo.pluses.Run;

public class Configuration {

	public static void load (String filename) {
		if (Objects.isNull (filename) || filename.length () == 0) {
			System.out.println ("[WARNING][CONFIG] "
				+ "Given filename is empty! Default name will be used.");
			filename = Run.CONFIG_NAME;
		}
		
		File crawler = Run.RUN_DIRECTORY;
		while (!crawler.isDirectory () && crawler.exists ()) {
			crawler = crawler.getParentFile ();
		}
		
		if (!crawler.exists () || !crawler.isDirectory ()) {
			Run.stopApplication (1, "Failed to find config directory");
			return;
		}
		
		for (File child : crawler.listFiles ()) {
			if (filename.equals (child.getName ())) {
				System.out.println ("[INFO][CONFIG] Config file `" 
						+ filename + "` will be used");
				Map <String, String> parsed = parseFile (child);
				
				if (Objects.isNull (parsed)) {
					Run.stopApplication (2, "Config file is "
						+ "corrupted or invaliable for reading");
					return;
				}
				
				for (String key : parsed.keySet ()) {
					System.setProperty (key, parsed.get (key));
				}
			}
		}
	}
	
	private static Map <String, String> parseFile (File config) {
		if (Objects.isNull (config) || !config.canRead ()) {
			return null;
		}
		
		Map <String, String> properties = new HashMap <> ();
		try (
			InputStream is = new FileInputStream (config);
			Reader r = new InputStreamReader (is, StandardCharsets.UTF_8);
			BufferedReader br = new BufferedReader (r);
		) {
			String line, prefix = "";
			int lineNumber = 0;
			
			while ((line = br.readLine ()) != null) {
				lineNumber++;
				
				line = line.trim ();
				if (line.length () == 0) { continue; }
				
				if (line.charAt (0) == '[') {
					int close = line.indexOf (']');
					if (close == -1) {
						prefix = line.substring (1);
					} else {
						prefix = line.substring (1, close);
					}
					
					prefix = prefix.toLowerCase ();
				} else if (line.charAt (0) == '#') {
					continue; // this is a comment
				} else {
					int sep = line.indexOf ('=');
					if (sep == -1) {
						System.out.println ("[WARING][CONFIG] Parse warning: "
							+ "wrong format of line " + lineNumber);
					} else {
						String key = line.substring (0, sep).trim ().toLowerCase ();
						String value = line.substring (sep + 1);
						
						sep = value.indexOf (';');
						if (sep != -1) {
							value = value.substring (0, sep);
						}
						
						value = value.trim ();
						properties.put ("pluses." + prefix + "." + key, value);
					}
				}
			}
		} catch (IOException ioe) {
			System.out.println ("[ERROR][CONFIG] Config file was found "
				+ "but during the parsing some exception occured:\n" + ioe);
		}
		
		return properties;
	}
	
}
