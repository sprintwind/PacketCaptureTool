package com.sprintwind.packetcapturetool;

public class UdpHeader implements ByteInitializer{
	
	public static int UDP_HDR_LEN = 8;
	
	private short sourcePort;
	private short destPort;
	private short length;
	private short checksum;
	
	public int getSourcePort() {
		return sourcePort&0xffff;
	}



	public void setSourcePort(short sourcePort) {
		this.sourcePort = sourcePort;
	}



	public int getDestPort() {
		return destPort&0xffff;
	}



	public void setDestPort(short destPort) {
		this.destPort = destPort;
	}



	public int getLength() {
		return length&0xffff;
	}



	public void setLength(short length) {
		this.length = length;
	}



	public int getChecksum() {
		return checksum&0xffff;
	}



	public void setChecksum(short checksum) {
		this.checksum = checksum;
	}



	@Override
	public int initWithByteArray(byte[] array, int start) {
		// TODO Auto-generated method stub
		
		int newStart = 0;
		
		if(array.length - start < UDP_HDR_LEN) {
			return ErrorCode.INVALID_LEN;
		}

		newStart = start;
		
		this.sourcePort = ByteTranslater.netShortFromByte(array, newStart);
		
		newStart += ByteTranslater.BYTE_LEN_OF_SHORT;
		this.destPort = ByteTranslater.netShortFromByte(array, newStart);
		
		newStart += ByteTranslater.BYTE_LEN_OF_SHORT;
		this.length = ByteTranslater.netShortFromByte(array, newStart);
		
		newStart += ByteTranslater.BYTE_LEN_OF_SHORT;
		this.checksum = ByteTranslater.netShortFromByte(array, newStart);
		
		return ErrorCode.OK;
	}

}
