package com.sprintwind.packetcapturetool;

import android.util.Log;

public class TcpHeader implements ByteInitializer{
	
	public static int TCP_HDR_MIN_LEN = 20;
	
	public static int FLAG_MASK_FIN = 0x1;
	public static int FLAG_MASK_SYN = 0x2;
	public static int FLAG_MASK_RST = 0x4;
	public static int FLAG_MASK_PSH = 0x8;
	public static int FLAG_MASK_ACK = 0x10;
	public static int FLAG_MASK_URG = 0x20;
	
	private short sourcePort;
	private short destPort;
	private long sequenceNumber;
	private long ackNumber;
	private char headerLength;
	private short reserve;
	private short flags;
	private short windowSize;
	private short checksum;
	private short urgent;
	private byte[] options;
	
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



	public long getSequenceNumber() {
		return sequenceNumber&0xffffffffL;
	}



	public void setSequenceNumber(int sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}



	public long getAckNumber() {
		return ackNumber&0xffffffffL;
	}



	public void setAckNumber(int ackNumber) {
		this.ackNumber = ackNumber;
	}



	public int getHeaderLength() {
		return (headerLength&0xf)*4;
	}



	public void setHeaderLength(char headerLength) {
		this.headerLength = headerLength;
	}



	public int getReserve() {
		return reserve&0x3f;
	}


	public void setReserve(short reserve) {
		this.reserve = reserve;
	}


	public int getFlags() {
		return flags&0x3f;
	}

	public String getFlagsString() {
		int flags=this.flags&0x3f;
		String flagsString = new String();
		
		if((flags&FLAG_MASK_FIN) != 0) {
			flagsString += "FIN";
		}
		
		if((flags&FLAG_MASK_SYN) != 0) {
			if(flagsString.length() > 0) {
				flagsString += ",";
			}
			flagsString += "SYN";
		}
		
		if((flags&FLAG_MASK_RST) != 0) {
			if(flagsString.length() > 0) {
				flagsString += ",";
			}
			flagsString += "RST";
		}
		
		if((flags&FLAG_MASK_PSH) != 0) {
			if(flagsString.length() > 0) {
				flagsString += ",";
			}
			flagsString += "PSH";
		}
		
		if((flags&FLAG_MASK_ACK) != 0) {
			if(flagsString.length() > 0) {
				flagsString += ",";
			}
			flagsString += "ACK";
		}
		
		if((flags&FLAG_MASK_URG) != 0) {
			if(flagsString.length() > 0) {
				flagsString += ",";
			}
			flagsString += "URG";
		}
		
		return flagsString;
	}

	public void setFlags(short flags) {
		this.flags = flags;
	}



	public int getWindowSize() {
		return windowSize&0xffff;
	}



	public void setWindowSize(short windowSize) {
		this.windowSize = windowSize;
	}



	public int getChecksum() {
		return checksum&0xffff;
	}



	public void setChecksum(short checksum) {
		this.checksum = checksum;
	}



	public int getUrgent() {
		return urgent&0xffff;
	}



	public void setUrgent(short urgent) {
		this.urgent = urgent;
	}



	public byte[] getOptions() {
		return options;
	}



	public void setOptions(byte[] options) {
		this.options = options;
	}



	@Override
	public int initWithByteArray(byte[] array, int start) {
		
		int newStart = 0;
		
		if(array.length - start < TCP_HDR_MIN_LEN) {
			return ErrorCode.INVALID_LEN;
		}

		newStart = start;
		this.sourcePort = ByteTranslater.netShortFromByte(array, newStart);
		
		newStart += ByteTranslater.BYTE_LEN_OF_SHORT;
		this.destPort = ByteTranslater.netShortFromByte(array, newStart);
		
		newStart += ByteTranslater.BYTE_LEN_OF_SHORT;
		this.sequenceNumber = ByteTranslater.netIntFromByte(array, newStart);
		
		newStart += ByteTranslater.BYTE_LEN_OF_INT;
		this.ackNumber = ByteTranslater.netIntFromByte(array, newStart);
		
		newStart += ByteTranslater.BYTE_LEN_OF_INT;
		this.headerLength = (char) ByteTranslater.netBitFromByte(array, newStart, 0, 3);
		this.reserve = (short) ByteTranslater.netBitFromByte(array, newStart, 4, 9);
		this.flags = (short) ByteTranslater.netBitFromByte(array, newStart, 10, 15);
		
		newStart += ByteTranslater.BYTE_LEN_OF_SHORT;
		this.windowSize = ByteTranslater.netShortFromByte(array, newStart);
		
		newStart += ByteTranslater.BYTE_LEN_OF_SHORT;
		this.checksum = ByteTranslater.netShortFromByte(array, newStart);
		
		newStart += ByteTranslater.BYTE_LEN_OF_SHORT;
		this.urgent = ByteTranslater.netShortFromByte(array, newStart);
		
		int optionLen = this.getHeaderLength() - TCP_HDR_MIN_LEN;
		if(optionLen <= 0){
			return ErrorCode.OK;
		}
		
		Log.i("sprintwind", "dataOffset:"+this.getHeaderLength() +", optionLen:"+optionLen);
		
		if(optionLen > (array.length - newStart)) {
			Log.e("sprintwind", "Invalid dataOffset:"+this.getHeaderLength());
			return ErrorCode.INVALID_LEN;
		}
		
		/* 处理选项 */
		this.options = new byte[optionLen];
		for(int i=0; i<optionLen;i++){
			this.options[i] = array[newStart+i];
		}
		
		return ErrorCode.OK;
	}
	
}
