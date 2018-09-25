package ru.shemplo.pluses.network.message;

import java.util.Objects;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import ru.shemplo.pluses.util.BitManip;
import ru.shemplo.pluses.util.ByteManip;

public class ControlMessage extends AbsMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1370495391125654449L;

	public static enum TypeWord {
        PING, PONG, BUY
    }
	
	/*
	 * Message frame
	 * 
	 *  0                        
     *  0 1 2 3 4 5 6 7
     * +-----+---------+
     * | TYP | xxxxxxx |
     * +-----+---------+
     * 
     * ========================
     * TYP | type word | 3 bits
     * ----+-----------+-------
     * TOTAL           | 3 bits
     * ========================
     * 
     * @ Type word (TYP) - integer that refers to ordinal
     * number of some type word in TypeWord enumeration
	 * 
	 */
	
	public  final TypeWord TYP;
	
	public ControlMessage (Message reply, DirectionWord direction, boolean repeated, TypeWord type) {
		super (reply, direction, type != TypeWord.PING, type == TypeWord.PING, repeated);
		
		this.TYP = type;
	}
	
	public ControlMessage (byte [] header, InputStream is) throws IOException {
		super (header);
		
		int read = is.read ();
		this.TYP = TypeWord.values () [read];
	}

	@Override
	public byte [] toByteArray () {
		try (
            ByteArrayOutputStream baos = new ByteArrayOutputStream ();
        ) {
            int messageOrdinal = MessageWord.CONTROL.ordinal ();
            baos.write (BitManip.genMessageHeader (DIRECTION, messageOrdinal, 
            						IS_REPLY, NEED_VERIFICATION, IS_REPEATED));
            
            byte [] buffer = ByteManip.I2B (TYP.ordinal ());
    		baos.write (new byte [] { buffer [buffer.length - 1] });
            
            final Message reply = getReply ();
            if (!Objects.isNull (reply)) {
                baos.write (reply.toByteArray ());
            }
            
            return baos.toByteArray ();
        } catch (IOException ioe) {
            if (DEBUG) { System.err.println (ioe); }
            return null;
        }
	}
	
}
