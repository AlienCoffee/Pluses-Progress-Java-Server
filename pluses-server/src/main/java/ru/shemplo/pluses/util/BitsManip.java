package ru.shemplo.pluses.util;


public class BitsManip {

	public static int bit (long value, int index) {
		return (int) ((value >> index) & 1);
	}
	
	/* Optimization for integer numbers */
	public static int bit (int value, int index) {
		return (value >> index) & 1;
	}
	
}
