package ru.shemplo.pluses.network.message.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

import ru.shemplo.pluses.network.message.Message;
import ru.shemplo.pluses.network.message.Message.DirectionWord;
import ru.shemplo.pluses.network.message.app.CommandMessage;
import ru.shemplo.pluses.network.message.app.CommandMessage.CommandWord;
import ru.shemplo.pluses.network.message.app.CommandMessage.TypeWord;
import ru.shemplo.pluses.struct.Pair;
import ru.shemplo.pluses.util.BitManip;
import ru.shemplo.pluses.util.Messenger;

public class Run {
    
    public static void main (String... args) throws IOException {
        byte [] header = BitManip.genMessageHeader (DirectionWord.CtC, 9, true, false, true);
        System.out.println (Integer.toBinaryString (Byte.toUnsignedInt (header [0])) 
                            + " " + Integer.toBinaryString (Byte.toUnsignedInt (header [1])));
        
        System.out.println ("== Test of decomposition ==");
        Pair <String, String> arg = Pair.mp ("AЯB", "ABЫЫQ");
        Message mes = new CommandMessage (DirectionWord.CtC, false, CommandWord.CREATE, TypeWord.INFO, arg);
        byte [] array = mes.toByteArray ();
        System.out.println ("Array (" + array.length + " bytes): " + Arrays.toString (array));

        ByteArrayInputStream bais = new ByteArrayInputStream (array);
        CommandMessage cmes = Messenger.readMessage (bais);
        System.out.println ("D: " + cmes.getDirection ());
    }
    
}
