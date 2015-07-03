package com.sprintwind.packetcapturetool;

import android.util.Log;

public class EtherHeader implements ByteInitializer{

	static final int ETH_HDR_LEN = 14;
	
	static final int ETH_PROTO_IP = 0x0800;
	static final int ETH_PROTO_ARP = 0x0806;
	static final int ETH_PROTO_DARP = 0x8035;
	static final int ETH_PROTO_IPV6 = 0x86DD;
	static final int ETH_PROTO_OAM = 0x8809;
	static final int ETH_PROTO_PPP = 0x880B;
	static final int ETH_RROTO_PPPDS = 0x8863;
	static final int ETH_RPOTO_PPPSS = 0x8864;
	
	private MacAddress sourceMac;
	private MacAddress destMac;
	private short protocol;

	public MacAddress getSourceMac() {
		return sourceMac;
	}

	public void setSourceMac(MacAddress sourceMac) {
		this.sourceMac = sourceMac;
	}

	public MacAddress getDestMac() {
		return destMac;
	}

	public void setDestMac(MacAddress destMac) {
		this.destMac = destMac;
	}

	public short getProtocol() {
		return protocol;
	}

	public void setProtocol(short protocol) {
		this.protocol = protocol;
	}
	
	public String getProtocolString() {
		int proto = (int)protocol&0xffff;
		String protocolString = new String();
		switch(proto){
		case ETH_PROTO_IP:
			protocolString += "IP(";
			break;
		case ETH_PROTO_ARP:
			protocolString += "ARP(";
			break;
		case ETH_PROTO_DARP:
			protocolString += "DARP(";
			break;
		case ETH_PROTO_IPV6:
			protocolString += "IPv6(";
			break;
		case ETH_PROTO_PPP:
			protocolString += "PPP(";
			break;
		case ETH_RROTO_PPPDS:
			protocolString += "PPP Discovery Stage(";
			break;
		case ETH_RPOTO_PPPSS:
			protocolString += "PPP Session Stage(";
			break;
		default:
			protocolString += "UNKOWN(";
		}
		
		protocolString += String.format("0x%04x", protocol) + ")";
		return protocolString;
	}

	public EtherHeader() {
		super();
		
		sourceMac = new MacAddress();
		destMac = new MacAddress();
	}

	/**
	* @param array array of bytes
	* @param start start index of the byte array
	* @return ErrorCode.OK on success, others when failed.
	*/
	public int initWithByteArray(byte[] array, int start) {
		int error = 0;
		int newStart = 0;
		
		if(array.length - start < ETH_HDR_LEN) {
			return ErrorCode.INVALID_LEN;
		}

		newStart = start;
		
		error = this.destMac.initWithByteArray(array, newStart);
		if(ErrorCode.OK != error){
			return error;
		}
		
		newStart += MacAddress.ETH_MAC_LEN;
		
		error = this.sourceMac.initWithByteArray(array, newStart);
		if(ErrorCode.OK != error){
			return error;
		}
		
		newStart += MacAddress.ETH_MAC_LEN;
		
		this.protocol = ByteTranslater.netShortFromByte(array, newStart);
	
		return ErrorCode.OK;
	}
	
	public void print() {
		System.out.println("Ether Header Info:");
		System.out.println("---------------------------------");
		System.out.println("Protocol:"+String.format("%x", this.protocol));
		System.out.println("Source MAC:"+this.sourceMac.getMacString());
		System.out.println("Dest MAC:"+this.destMac.getMacString());
		System.out.println("---------------------------------");
	}
}