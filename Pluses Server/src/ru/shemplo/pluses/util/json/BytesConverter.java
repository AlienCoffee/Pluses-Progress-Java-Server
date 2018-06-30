package ru.shemplo.pluses.util.json;


public class BytesConverter {

	public static byte [] L2B (long value) {
		return convert (value, 8);
	}
	
	public static byte [] I2B (int value) {
		return convert (value, 4);
	}
	
	public static byte [] S2B (short value) {
		return convert (value, 2);
	}
	
	public static byte [] C2B (char value) {
		return convert (value, 2);
	}
	
	private static byte [] convert (long value, int length) {
		byte [] array = new byte [length];
		for (int i = 0; i < array.length; i++) {
			int move = (array.length - i - 1) * 8;
			array [i] = (byte) (0xff & ((value & (0xff << move)) >> move));
		}
		
		return array;
	}
	
}
