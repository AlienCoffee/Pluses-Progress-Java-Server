package ru.shemplo.pluses.network.message.app;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import ru.shemplo.pluses.network.message.Message;
import ru.shemplo.pluses.struct.Pair;
import ru.shemplo.pluses.util.BitManip;
import ru.shemplo.pluses.util.ByteManip;

public class CommandMessage extends AbsAppMessage {
    
    /**
     * 
     */
    private static final long serialVersionUID = -2048599100176345202L;

    public static enum CommandWord {
        CREATE, INSERT, MOVE, SELECT, UPDATE
    }
    
    public static enum TypeWord {
        GROUP, INFO, PROGRESS, TASK, TOPIC, TRY, STUDENT
    }
    
    /*
     *  0               1              
     *  0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
     * +-------+-------+---+-----------+
     * |  COM  |  TYP  | x |   PARAM   |
     * +-------+-------+---+-----------+
     * |     NSIZE     |  PARAM  NAME  :
     * +---------------+ - - - - - - - +
     * :                               |
     * +-------------------------------+
     * |          VALUE  SIZE          |
     * +-------------------------------+
     * :          PARAM VALUE          :
     * + - - - - - - - - - - - - - - - +
     * :                               :
     *  . . . . . . . . . . . . . . . .
     * :                               :
     * +-------------------------------+
     * 
     * =============================================
     * COM        | command word       | 4 bits
     * TYP        | type word          | 4 bits
     * PARAM      | parameters         | 6 bits
     * NSIZE      | size of name       | 8 bits      } REPEATS
     * PARAM NAME | name of parameter  | NSIZE bytes }
     * VALUE SIZE | size of value      | 16 bits     }
     * VALUE      | value of parameter | VSIZE bytes } 
     * -----------+--------------------+------------
     * TOTAL                           | 14 bits + D
     * =============================================
     * 
     * @ Command word (COM) - integer that refers to ordinal
     * number of some command word in CommandWord enumeration
     * 
     * @ Type word (TYP) - integer that refers to ordinal
     * number of some type word in TypeWord enumeration
     * 
     * @ Parameters (PARAM) - number of parameters in 
     * received message. Each parameter entry has format 
     * that is described bellow
     * 
     * @ Size of name (NSIZE) - size of name in bytes of
     * parameter's name. For UTF-8 strings max length of
     * name is 128 chars (= 2^8 / 2)
     * 
     * @ Name of parameter (PARAM NAME) - string that
     * represents name of parameter (recommended to use UTF-8)
     * 
     * @ Size of value (VSIZE) - size of value in bytes of
     * parameter's value. It can contains up to 65,535 bytes
     * 
     * @ Value of parameter (VALUE) - value of parameter in
     * raw bytes
     * 
     * D - dynamic size that is not exactly calculated now.
     * Formally it's equals to 8 * sum (NSIZE_i + VSIZE_i) bits
     * 
     */
    
    protected final List <Pair <String, String>> PARAMS;
    protected final CommandWord COM;
    protected final TypeWord TYP;
    
    @SuppressWarnings ("unchecked")
    public CommandMessage (Message reply, DirectionWord direction, 
            boolean repeated, CommandWord command, TypeWord type, 
            Pair <String, String>... params) {
        super (reply, direction, reply != null, true, repeated);
        this.COM = command;
        this.TYP = type;
        
        if (params.length > 0) {
            this.PARAMS = Arrays.asList (params);
        } else {
            this.PARAMS = new ArrayList <> ();
        }
    }
    
    @SafeVarargs
    public CommandMessage (DirectionWord direction, boolean repeated, CommandWord command, 
            TypeWord type, Pair <String, String>... params) {
        this (null, direction, repeated, command, type, params);
    }
    
    public CommandMessage (byte [] header, InputStream is) throws IOException {
        super (header, is);
        
        byte [] lheader = new byte [2];
        is.read (lheader, 0, lheader.length);
        
        int commandOrdinal = BitManip.getBits (lheader [0], 4, 4);
        this.COM = CommandWord.values () [commandOrdinal];
        
        int typeOrdinal = BitManip.getBits (lheader [0], 0, 4);
        this.TYP = TypeWord.values () [typeOrdinal];
        
        if (DEBUG) {
            System.out.println ("Comand message COM: " + COM + ", TYP: " + TYP);
        }
        
        int parameters = BitManip.getBits (lheader [1], 0, 6);
        this.PARAMS = new ArrayList <> (parameters);
        byte [] buffer = new byte [1 << 16];
        for (int i = 0; i < parameters; i++) {
            int nameLength = is.read ();
            is.read (buffer, 0, nameLength);
            
            String paramName = new String (buffer, 0, nameLength, StandardCharsets.UTF_8);
            
            is.read (lheader, 0, lheader.length);
            int valueLength = ByteManip.B2I (lheader);
            is.read (buffer, 0, valueLength);
            
            String paramValue = new String (buffer, 0, valueLength, StandardCharsets.UTF_8);
            
            Pair <String, String> entry = Pair.mp (paramName, paramValue);
            PARAMS.add (entry);
            
            if (DEBUG) {
                System.out.println ("Parameter entry " + entry);  
            }
        }
    }
    
    /**
     * 
     * 
     * @return
     * 
     */
    public CommandWord getCommand () {
        return COM;
    }
    
    /**
     * 
     * 
     * @return
     * 
     */
    public TypeWord getType () {
        return TYP;
    }
    
    @Override
    public byte [] toByteArray () {
        try (
            ByteArrayOutputStream baos = new ByteArrayOutputStream ();
        ) {
            int messageOrdinal = MessageWord.COMMAND.ordinal ();
            baos.write (BitManip.genMessageHeader (DIRECTION, messageOrdinal, IS_REPLY, NEED_VERIFICATION, IS_REPEATED));
            baos.write (BitManip.genCommandMessageHeader (COM, TYP, PARAMS.size ()));
            for (Pair <String, String> pair : PARAMS) {
                byte [] name = pair.F.getBytes (StandardCharsets.UTF_8);
                baos.write (ByteManip.I2B (name.length) [3]);
                baos.write (name, 0, name.length);
                
                byte [] value = pair.S.getBytes (StandardCharsets.UTF_8);
                byte [] valueLength = ByteManip.I2B (value.length);
                baos.write (valueLength [2]); baos.write (valueLength [3]);
                baos.write (value, 0, value.length);
            }
            
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
