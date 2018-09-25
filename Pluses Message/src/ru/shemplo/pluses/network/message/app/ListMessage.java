package ru.shemplo.pluses.network.message.app;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import ru.shemplo.pluses.network.message.Message;
import ru.shemplo.pluses.util.BitManip;

public class ListMessage <T> extends AbsAppMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = -330029103157304427L;

	/*
	 * Message frame
	 * 
	 *  0               1              
     *  0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
     * +-------------------------------+
     * |           LIST SIZE           :
     * + - - - - - - - - - - - - - - - +
     * :     LIST SIZE (CONTINUE).     |
     * +-------------------------------+
	 * |            COMMENT            |
	 * +-------------------------------+
	 * |          VALUE  SIZE          |
     * +-------------------------------+
	 * :          LIST  VALUE          :
     * + - - - - - - - - - - - - - - - +
     * :                               :
     *  . . . . . . . . . . . . . . . .
     * :                               :
     * +-------------------------------+
     * 
     * ============================================
     * LIST SIZE  | list size     | 32 bits
     * COMMENT    | commentary    | 16 bits
     * VALUE SIZE | size of value | 16 bits         } REPEATS
     * LIST VALUE | list value    | VALUE SIZE bits }
     * -----------+---------------+----------------
     * TOTAL                      |     64 bits + D
     * ============================================
     * 
     * @ List size (LIST SIZE) - length of sending list or array
     * 
     * @ Commentary (COMMENT) - 2 bytes string commentary
     * to sending data (can be used for set up type of data)
     * 
     * @ Size of value (VALUE SIZE) - size of value in bytes of
     * parameter's value. It can contains up to 65,535 bytes
     * 
     * @ List value (LIST VALUE) - value of list or array in
     * raw bytes
     * 
     * D - dynamic size that is not exactly calculated now.
     * Formally it's equals to 8 * sum (NSIZE_i + VSIZE_i) bits
	 * 
	 */
	
	protected final List <T> VALUES;
	
	public ListMessage (Message reply, DirectionWord direction, 
            boolean replied, boolean repeated, List <T> list) {
        super (reply, direction, replied, false, repeated);
        
        this.VALUES = list;
    }
	
	public ListMessage (Message reply, DirectionWord direction, boolean replied, 
			boolean repeated, T [] array) {
        this (reply, direction, replied, repeated, Arrays.asList (array));
    }
	
	public ListMessage (byte [] header, InputStream is) throws IOException {
		super (header, is);
		
		this.VALUES = null;
	}

	@Override
	public byte [] toByteArray () {
		try (
            ByteArrayOutputStream baos = new ByteArrayOutputStream ();
        ) {
            int messageOrdinal = MessageWord.CONTROL.ordinal ();
            baos.write (BitManip.genMessageHeader (DIRECTION, messageOrdinal, 
            						IS_REPLY, NEED_VERIFICATION, IS_REPEATED));
            
            // TODO: insert implementation
            
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
