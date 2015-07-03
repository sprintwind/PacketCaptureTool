package com.sprintwind.packetcapturetool;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class PcapPacketHeader implements ByteInitializer{

static final int PCAP_PKT_HDR_LEN = 16; 

private TimeVal timeVal;
private int capLen;
private int originLen;

public TimeVal getTimeVal() {
	return timeVal;
}

public void setTimeVal(TimeVal timeVal) {
	this.timeVal = timeVal;
}

public int getCapLen() {
	return capLen;
}

public void setCapLen(int capLen) {
	this.capLen = capLen;
}

public int getOriginLen() {
	return originLen;
}

public void setOriginLen(int originLen) {
	this.originLen = originLen;
}

public PcapPacketHeader() {
	super();
	// TODO Auto-generated constructor stub
	
	this.timeVal = new TimeVal();
	this.capLen = 0;
	this.originLen = 0;
	}

public String getTimeString() {

	SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	Calendar calendar = Calendar.getInstance();
	calendar.setTimeInMillis((long)this.timeVal.sec*1000);
	
	return formatter.format(calendar.getTimeInMillis()) +"."+this.timeVal.uSec;
	}

public Date getDate(){
	Date date = new Date((long)this.timeVal.sec*1000);
	return date;
}

/**
 * @param array array of bytes
 * @param start start index of the byte array
 * @return ErrorCode.OK on success, others when failed.
 */
public int initWithByteArray(byte[] array, int start) {

	int Error;
	
	if(array.length - start < PCAP_PKT_HDR_LEN) {
	return ErrorCode.INVALID_LEN;
		}
	
	Error = timeVal.initWithByteArray(array, start);
	if(ErrorCode.OK != Error) {
	return Error;
		}
	
	capLen = ByteTranslater.hostIntFromByte(array, start + TimeVal.TIME_VAL_LEN);
	originLen = ByteTranslater.hostIntFromByte(array, start + TimeVal.TIME_VAL_LEN + ByteTranslater.BYTE_LEN_OF_INT);
	
	return ErrorCode.OK;
	}

public void print() {
	System.out.println("pcap packet header info:");
	System.out.println("----------------------------");
	System.out.println("time:"+this.getTimeString());
	System.out.println("capture length:"+this.capLen);
	System.out.println("original length:"+this.originLen);
	System.out.println("----------------------------");
	}
}