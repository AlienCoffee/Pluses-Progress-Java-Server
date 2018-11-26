package ru.shemplo.pluses.logic;

import static ru.shemplo.pluses.network.message.AppMessage.MessageDirection.*;
import static ru.shemplo.pluses.network.message.ControlMessage.ControlType.*;
import static ru.shemplo.pluses.Run.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicLong;

import ru.shemplo.pluses.network.message.ControlMessage;
import ru.shemplo.pluses.network.message.Message;
import ru.shemplo.pluses.network.pool.AppConnection;


public class HelpHandler {
	static private AtomicLong lastUpdated = new AtomicLong(0);
	static private String commandsHelp;
	
	public static void runHelp(AppConnection connection) throws IOException {
		tryUpdateHelpContent();
		Message help = new ControlMessage (STC, INFO, 0, commandsHelp);
        connection.sendMessage (help);
	}
	
	private static void tryUpdateHelpContent() throws IOException {
		File hf = new File(HELP_NAME);
		Long modified = hf.lastModified(), updated = lastUpdated.get();

		if (updated >= modified) return;
		
		try(BufferedReader br = Files.newBufferedReader(hf.toPath(), StandardCharsets.UTF_8)) {
		    StringBuilder sb = new StringBuilder();
		    String line = br.readLine();

		    while (line != null) {
		        sb.append(line);
		        sb.append(System.lineSeparator());
		        line = br.readLine();
		    }
		    
		    if (lastUpdated.compareAndSet(updated, modified)) {
		    	commandsHelp = sb.toString();
		    }
		} 
	}
}
