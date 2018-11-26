package ru.shemplo.pluses.logic;

import static ru.shemplo.pluses.network.message.AppMessage.MessageDirection.*;
import static ru.shemplo.pluses.network.message.ControlMessage.ControlType.*;
import static ru.shemplo.pluses.Run.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import ru.shemplo.pluses.network.message.ControlMessage;
import ru.shemplo.pluses.network.message.Message;
import ru.shemplo.pluses.network.pool.AppConnection;


public class HelpHandler {
	static private Long lastUpdated = 0L;
	static private String commandsHelp;
	
	public static void runHelp(AppConnection connection) throws IOException {
		tryUpdateHelpContent();
		Message help = new ControlMessage (STC, INFO, 0, commandsHelp);
        connection.sendMessage (help);
	}
	
	private static void tryUpdateHelpContent() throws IOException {
		File hf = new File(HELP_NAME);
		Long modified = hf.lastModified();
		if (lastUpdated >= modified) return;
		
		try(BufferedReader br = new BufferedReader(new FileReader(hf))) {
		    StringBuilder sb = new StringBuilder();
		    String line = br.readLine();

		    while (line != null) {
		        sb.append(line);
		        sb.append(System.lineSeparator());
		        line = br.readLine();
		    }
		    
		    commandsHelp = sb.toString();
		    lastUpdated = modified;
		} 
	}
}
