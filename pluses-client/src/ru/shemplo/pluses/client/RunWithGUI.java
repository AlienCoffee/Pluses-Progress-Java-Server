package ru.shemplo.pluses.client;

import java.util.ArrayList;
import java.util.List;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import java.net.Socket;
import java.sql.Timestamp;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.json.JSONObject;

import ru.shemplo.pluses.network.message.AppMessage;
import ru.shemplo.pluses.network.message.AppMessage.MessageDirection;
import ru.shemplo.pluses.network.message.CommandMessage;
import ru.shemplo.pluses.network.message.Message;

public class RunWithGUI extends Application {

	public static void main (String [] args) {
		launch (args);
	}
	
	private static long backvert (byte [] bytes, int length) {
        int limit = Math.min (length, bytes.length);
        long result = 0;
        
        for (int i = 0; i < limit; i++) {
            result = (result << 8) | (bytes [i] & 0xffL);
        }
        
        return result;
    }
	
	private final List <String> MESSAGES = new ArrayList <> ();
	
	private TextField hostInput, portInput;
	private ListView <String> histView;
	private TextArea messageArea;
	
	private volatile Socket socket;
	
	private final Runnable TASK = () -> {
		byte [] capacer = new byte [4];
		
		while (true) {
			Socket socket = this.socket;
			if (socket == null) {
				try   { Thread.sleep (100); } 
				catch (InterruptedException ie) {
					return;
				}
				
				continue;
			}
			
			try {
				Timestamp t1 = new Timestamp (System.currentTimeMillis ());
				List <String> history = histView.getItems ();
				InputStream is = socket.getInputStream ();
				
				if (is.read (capacer, 0, capacer.length) == -1) {
					Platform.runLater (() -> {
						String message = t1.toString () 
	                			+ "\n disconnected from host";
	                	messageArea.setText (message);
	                	MESSAGES.add (message);
	                	
	                    history.add (t1.toString () 
	                    			+ " connection closed");
	                });
					
                    socket = null;
                    return;
	            }
				
				Timestamp t2 = new Timestamp (System.currentTimeMillis ());
				int read = (int) backvert (capacer, 4);
                byte [] data = new byte [read];
                if (is.read (data, 0, data.length) == -1) {
                	Platform.runLater (() -> {
                		String message = t2.toString () 
                    			+ "\n disconnected from host";
                    	messageArea.setText (message);
                    	MESSAGES.add (message);
                    	
                        history.add (t2.toString () 
                        			+ " connection closed");
                    });
                	
                    socket = null;
                    return;
                }
                
                ByteArrayInputStream bais = new ByteArrayInputStream (data);
                ObjectInputStream bis = new ObjectInputStream (bais);
                Object tmp = bis.readObject ();
                
                Timestamp t3 = new Timestamp (System.currentTimeMillis ());
                Platform.runLater (() -> {
                	if (tmp instanceof Message) {
                		Message m = (Message) tmp;
                		String message = t3.toString () + "\n" 
                				+ m.toJSON (new JSONObject ());
                    	messageArea.setText (message);
                    	MESSAGES.add (message);
                	} else {
                		String message = t3.toString () + "\n" + tmp;
                    	messageArea.setText (message);
                    	MESSAGES.add (message);
                	}
                	
                	String message = t3.toString () + " (<) " 
                			+ tmp.getClass ().getSimpleName ();
                    history.add (message);
                });
			} catch (IOException | ClassNotFoundException ioe) {
				messageArea.setText ("Failed to read message:\n" + ioe.toString ());
				
				try   { Thread.sleep (100); } 
				catch (InterruptedException ie) {
					return;
				}
				
				continue;
			}
		}
	};
	
	@Override
	public void start (Stage stage) throws Exception {
		HBox mainHorz = new HBox ();
		
		// Left column
		
		VBox leftCol = new VBox ();
		mainHorz.getChildren ().add (leftCol);
		
		// // Connection
		
		Label connectLabel = new Label ("Connection to the host:");
		leftCol.getChildren ().add (connectLabel);
		VBox.setMargin (connectLabel, new Insets (10, 0, 0, 10));
		
		VBox connectCol = new VBox ();
		leftCol.getChildren ().add (connectCol);
		connectCol.setAlignment (Pos.CENTER_RIGHT);
		VBox.setMargin (connectCol, new Insets (10));
		
		HBox hostHorz = new HBox ();
		connectCol.getChildren ().add (hostHorz);
		hostHorz.setAlignment (Pos.CENTER_RIGHT);
		VBox.setMargin (hostHorz, new Insets (5));
		
		Label hostLabel = new Label ("Host: ");
		hostHorz.getChildren ().add (hostLabel);
		
		hostInput = new TextField ("shemplo.ru");
		hostHorz.getChildren ().add (hostInput);
		HBox.setMargin (hostInput, new Insets (0, 0, 0, 5));
		hostInput.setMinWidth (250);
		
		HBox portHorz = new HBox ();
		connectCol.getChildren ().add (portHorz);
		portHorz.setAlignment (Pos.CENTER_RIGHT);
		VBox.setMargin (portHorz, new Insets (5));
		
		Label portLabel = new Label ("Port: ");
		portHorz.getChildren ().add (portLabel);
		
		portInput = new TextField ("1999");
		portHorz.getChildren ().add (portInput);
		HBox.setMargin (portInput, new Insets (0, 0, 0, 5));
		portInput.setMinWidth (250);
		
		Button connectButton = new Button ("Connect");
		connectCol.getChildren ().add (connectButton);
		VBox.setMargin (connectButton, new Insets (5));
		
		// // Message history
		
		Label histLabel = new Label ("History: ");
		leftCol.getChildren ().add (histLabel);
		VBox.setMargin (histLabel, new Insets (0, 0, 0, 10));
		
		histView = new ListView <> ();
		leftCol.getChildren ().add (histView);
		VBox.setMargin (histView, new Insets (10));
		VBox.setVgrow (histView, Priority.ALWAYS);
		histView.setMinWidth (300);
		
		// Right column
		
		VBox rightCol = new VBox ();
		mainHorz.getChildren ().add (rightCol);
		HBox.setHgrow (rightCol, Priority.SOMETIMES);
		
		// // Message content
		
		messageArea = new TextArea ();
		rightCol.getChildren ().add (messageArea);
		VBox.setMargin (messageArea, new Insets (10));
		VBox.setVgrow (messageArea, Priority.ALWAYS);
		messageArea.setEditable (false);
		messageArea.setWrapText (true);
		
		// // Command horizontal'
		
		HBox comHorz = new HBox ();
		rightCol.getChildren ().add (comHorz);
		comHorz.setAlignment (Pos.CENTER_LEFT);
		VBox.setMargin (comHorz, new Insets (10));
		
		Label comLabel = new Label ("Comand: ");
		comHorz.getChildren ().add (comLabel);
		
		TextField comInput = new TextField ();
		comHorz.getChildren ().add (comInput);
		HBox.setHgrow (comInput, Priority.ALWAYS);
		
		// // // Handlers
		
		connectButton.setOnMouseClicked (me -> connect ());
		hostInput.setOnAction (ae -> connect ());
		portInput.setOnAction (ae -> connect ());
		
		histView.getSelectionModel ().selectedItemProperty ()
				.addListener (ce -> {
			int index = histView.getSelectionModel ().getSelectedIndex ();
			if (index < 0 || MESSAGES.size () <= index) { return; }
			
			messageArea.setText (MESSAGES.get (index));
		});
		
		comInput.setOnAction (ae -> {
			Socket socket = this.socket;
			if (socket == null) { return; }
			
			String line = comInput.getText ();
			if (line == null || line.length () == 0) {
				return;
			}
			line = line.trim ();
			
			try {
				OutputStream os = socket.getOutputStream ();
				
				ByteArrayOutputStream baos = new ByteArrayOutputStream ();
	            ObjectOutputStream oos = new ObjectOutputStream (baos);
	            
	            MessageDirection dir = MessageDirection.CTS;
	            AppMessage message = new CommandMessage (dir, line);
	            oos.writeObject (message);
	            oos.flush ();
	            
	            byte [] data = baos.toByteArray ();
	            byte [] length = {
	                (byte) (data.length >> 24 & 0xff),
	                (byte) (data.length >> 16 & 0xff),
	                (byte) (data.length >> 8 & 0xff),
	                (byte) (data.length & 0xff)
	            };
	            
	            os.write (length);
	            os.write (data);
	            os.flush ();
	            
	            Timestamp t = new Timestamp (System.currentTimeMillis ());
	            Platform.runLater (() -> {
	            	String string = t.toString () + " (>) " 
    	            	+ message.getClass ().getSimpleName ();
    	            List <String> list = histView.getItems ();
    				list.add (string);
    				
    				string = t.toString () + "\n" 
    						+ message.toJSON (new JSONObject ());
    				messageArea.setText (string);
    				MESSAGES.add (string);
	            });
	            
	            comInput.setText ("");
			} catch (IOException ioe) {
				Timestamp t = new Timestamp (System.currentTimeMillis ());
	            Platform.runLater (() -> {
	            	String string = t.toString () + " (>) " 
    	            	+ ioe.getClass ().getSimpleName ();
    	            List <String> list = histView.getItems ();
    				list.add (string);
    				
    				string = t.toString () + "\n " + ioe.toString ();
    				messageArea.setText (string);
    				MESSAGES.add (string);
	            });
			}
		});
		
		Scene scene = new Scene (mainHorz);
		
		stage.setTitle ("Test client");
		stage.setScene (scene);
		stage.sizeToScene ();
		stage.show ();
	}
	
	private void connect () {
		messageArea.setText ("");
		
		String host = hostInput.getText (), portIn = portInput.getText ();
		if (host == null || host.length () == 0) {
			messageArea.setText ("Connection failed: empty host address");
			return;
		} else {
			host = host.trim ().toLowerCase ();
		}
		
		int port = 0;
		try {
			port = Integer.parseInt (portIn);
		} catch (NumberFormatException nfe) {
			messageArea.setText ("Wrong port:\n" + nfe.toString ());
			return;
		}
		
		try {
			socket = new Socket (host, port);
			String message = "Connected to " + host + ":" + port;
			
			List <String> list = histView.getItems ();
			messageArea.setText (message);
			MESSAGES.add (message);
			list.add (message);
		} catch (IOException ioe) {
			messageArea.setText ("Connection failed:\n" + ioe.toString ());
			return;
		}
		
		Thread t = new Thread (TASK);
		t.setDaemon (true);
		t.start ();
	}
	
}
