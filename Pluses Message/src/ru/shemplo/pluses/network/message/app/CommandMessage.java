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
    
    @SuppressWarnings ("unchecked")
    public CommandMessage (DirectionWord direction, boolean repeated, CommandWord command, TypeWord type) {
        this (null, direction, repeated, command, type);
    }
    
    public CommandMessage (InputStream is) throws IOException {
        super (is);
        this.COM = null;
        this.TYP = null;
        
        this.PARAMS = new ArrayList <> ();
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
            baos.write (BitManip.genMessageHeader (DIRECTION, 0, IS_REPLY, NEED_VERIFICATION, IS_REPEATED));
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
            
            if (!Objects.isNull (REPLY)) {
                baos.write (REPLY.toByteArray ());
            }
            
            return baos.toByteArray ();
        } catch (IOException ioe) {
            ioe.printStackTrace ();
            return new byte [0];
        }
    }
    
}
