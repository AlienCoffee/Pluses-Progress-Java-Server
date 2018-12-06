package ru.shemplo.pluses.log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

import ru.shemplo.pluses.util.FSCrawler;

public class Log {

	public static enum LogLevel {
		NORMAL, WARNING, ERROR,
		DEBUG, INFO
	}
	
	private static final PrintWriter [] logs 
		= new PrintWriter [LogLevel.values ().length];
	private static final DateFormat date;
	
	static {
		String logDirName = System.getProperty ("pluses.log.dir");
		if (Objects.isNull (logDirName) || logDirName.length () == 0) {
			System.out.println ("[WARNING][LOG] Log directory is not defined. "
				+ "Logs will not be written in files");
		} else {
			File directory = new File (".", logDirName);
			if (!directory.exists () || !directory.isDirectory ()) {
				FSCrawler.crawl (directory, f -> true, 
					f -> f.delete (), f -> f.delete ());
				directory.mkdir ();
			}
			
			int index = 0;
			for (LogLevel level : LogLevel.values ()) {
				String name = level.name ().toLowerCase () + ".log";
				File levelLog = new File (directory, name);
				
				try {
					if (!levelLog.exists ()) {
						levelLog.createNewFile ();
					}
				} catch (IOException ioe) {
					System.out.println ("[ERROR][LOG] Failed to create log for " + level
						+ " by the occured exception:\n" + ioe);
				}
				
				try {
					OutputStream os = new FileOutputStream (levelLog, true);
					Writer w = new OutputStreamWriter (os, StandardCharsets.UTF_8);
					logs [index++] = new PrintWriter (w);
				} catch (IOException ioe) {
					System.out.println ("[ERROR][LOG] Failed to create log for " + level
						+ " by the occured exception:\n" + ioe);
				}
			}
		}
		
		String dateFormat = System.getProperty ("pluses.log.date");
		date = new SimpleDateFormat (dateFormat);
	}
	
	public static PrintWriter rawLogger (LogLevel level) {
		if (Objects.isNull (level)) { return null; }
		synchronized (logs [level.ordinal ()]) {
			return logs [level.ordinal ()];
		}
	}
	
	public static void log (String parent, String message) {
		_log (LogLevel.NORMAL, parent, message);
	}
	
	public static void warning (String parent, String message) {
		_log (LogLevel.WARNING, parent, message);
	}
	
	public static void error (String parent, String message) {
		_log (LogLevel.ERROR, parent, message);
	}
	
	public static void error (String parent, Exception exception) {
        error (parent, "Exception occured (" + exception + ")");
    }
	
	private static synchronized final void _log (LogLevel level, String parent, String message) {
		if (Objects.isNull (message) || message.length () == 0) { return; }
		
		int log = level.ordinal ();
		StringBuilder sb = new StringBuilder (Log.date.format (new Date ()));
		
		sb.append (" ");
		if (!LogLevel.NORMAL.equals (level)) {
			sb.append ("[");
			sb.append (level.name ());
			sb.append ("]");
		}
		
		if (Objects.isNull (parent) || parent.length () == 0) {
			sb.append (" ");
			sb.append (message);
		} else {
			sb.append ("[");
			sb.append (parent);
			sb.append ("] ");
			sb.append (message);
		}
		
		if (sb.length () > 0) { 
			String print = sb.toString ();
			System.out.println (print);
			logs [log].println (print);
			logs [log].flush ();
		}
	}
	
	public static synchronized void close () {
		for (Writer writer : logs) {
			try {
				writer.flush ();
				writer.close ();
			} catch (Exception e) {}
		}
	}
	
}
