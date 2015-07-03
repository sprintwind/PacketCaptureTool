package com.sprintwind.packetcapturetool;

public class ByteTranslater {

	public static final int BYTE_LEN_OF_INT  = 4;
	public static final int BYTE_LEN_OF_SHORT = 2;
	public static final int BYTE_LEN_OF_CHAR = 1;

	public static final int BYTE_BIT_NUM = 8;
	public static final int INT_BIT_NUM = 32;
	public static final int MIN_INT_BIT = 0;
	public static final int MAX_INT_BIT = 31;

	public static int hostIntFromByte(byte[] array, int start) {
	
		if(start + BYTE_LEN_OF_INT > array.length) {
			return ErrorCode.INVALID_LEN;
		}
		
		int result = 0;
		
		for(int i=0; i<4; i++) {
			result += (array[start+3-i]&0xff) << (24-8*i);
		}
	
		return result;
	}
	
	public static int netIntFromByte(byte[] array, int start) {
		
		if(start + BYTE_LEN_OF_INT > array.length) {
			return ErrorCode.INVALID_LEN;
		}
		
		int result = 0;
		
		for(int i=0; i<4; i++) {
			result += (array[start+3-i]&0xff) << (8*i);
		}
	
		return result;
	}

	public static short hostShortFromByte(byte[] array, int start) {
		
	if(start + BYTE_LEN_OF_SHORT > array.length) {
		return (short) ErrorCode.INVALID_LEN;
	}
	
	int result = 0;
	for(int i=0; i<2; i++) {
		result += (array[start+1-i]&0xff) << (8-8*i);
	}
	
	return (short) result;
	}
	
	public static short netShortFromByte(byte[] array, int start) {
		
		if(start + BYTE_LEN_OF_SHORT > array.length) {
			return (short) ErrorCode.INVALID_LEN;
		}
		
		int result = 0;
		for(int i=0; i<2; i++) {
			result += (array[start+1-i]&0xff) << (8*i);
		}
		
		return (short) result;
		}
	
	public static char charFromByte(byte[] array, int start){
		if(start + BYTE_LEN_OF_CHAR > array.length) {
			return (char) ErrorCode.INVALID_LEN;
		}
		
		return (char)array[start];
		
	}
	
	public static int hostBitFromByte(byte[] array, int start, int low, int high){
		if(start + BYTE_LEN_OF_INT > array.length) {
			return ErrorCode.INVALID_LEN;
		}
		
		if((low > high)||(low < MIN_INT_BIT)||(high > MAX_INT_BIT)){
			return ErrorCode.INVALID_BIT_RANGE;
		}
		
		int original = hostIntFromByte(array, start);
		
		int maskHigh = 0x80000000 >> high;
		
		original &= maskHigh;
		//System.out.println("original1:"+String.format("%x", original));
		int maskLow;
		if(0 == low) {
			maskLow = 0xffffffff;
		}
		else {
			maskLow = (1 << (INT_BIT_NUM - low)) -1 ;
		}
		
		original &= maskLow;
		//System.out.println("original2:"+String.format("%x", original));
		
		//System.out.println("maskHigh:"+String.format("%x", maskHigh)+"maskLow:"+String.format("%x", maskLow));
		return original;
	}
	
	public static int netBitFromByte(byte[] array, int start, int low, int high){
		if( (high/BYTE_BIT_NUM)+start >= array.length) {
			return ErrorCode.INVALID_LEN;
		}
		
		if( (low > high)||(high > MAX_INT_BIT)){
			return ErrorCode.INVALID_BIT_RANGE;
		}
		
		int original = netIntFromByte(array, start);
		//System.out.println("original:"+String.format("%x", original));
		
		int maskHigh = 0x80000000 >> high;
			
		original &= maskHigh;
		//System.out.println("original1:"+String.format("%x", original));
		int maskLow;
		if(0 == low) {
			maskLow = 0xffffffff;
		}
		else {
			maskLow = (1 << (INT_BIT_NUM - low)) -1 ;
		}
		
		original &= maskLow;
		//System.out.println("original2:"+String.format("%x", original));
		
		//System.out.println("maskHigh:"+String.format("%x", maskHigh)+"maskLow:"+String.format("%x", maskLow));
		return original >> (INT_BIT_NUM-high-1);
	}
}

	