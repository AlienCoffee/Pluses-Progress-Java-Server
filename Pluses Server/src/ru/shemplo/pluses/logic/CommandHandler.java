package ru.shemplo.pluses.logic;

import static ru.shemplo.pluses.network.message.Message.MessageDirection.*;

import java.util.Objects;
import java.util.StringTokenizer;

import ru.shemplo.pluses.network.message.Message;
import ru.shemplo.pluses.network.pool.AppConnection;

public class CommandHandler {
	
	public static void run (Message mes, AppConnection connection) {
		if (Objects.isNull (mes) || !CLIENT_TO_SERVER.equals (mes.getDirection ())) { 
			return; // Message is empty or has invalid direction
		}
		
		StringTokenizer st = new StringTokenizer (mes.getCommand ());
		switch (st.nextToken ().toLowerCase ()) {
			case "add": // command to add something
				runAdd (st, mes, connection);
				break;
				
			default:
				System.out.println ("Unknown command");
		}
	}
	
	public static void runAdd (StringTokenizer tokens, Message message, 
			AppConnection connection) {
		System.out.println ("Add command " + tokens.countTokens ());
	}
	
}
