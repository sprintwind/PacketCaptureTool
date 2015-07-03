package com.sprintwind.packetcapturetool;

public class MacAddress implements ByteInitializer{
	static final int ETH_MAC_LEN = 6;
	
	private byte[] mac;
	
	public byte[] getMac() {
		return mac;
	}
	
	public String getMacString(){
		return String.format("%02x:%02x:%02x:%02x:%02x:%02x", mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]);
	}

	public void setMac(byte[] mac) {
		this.mac = mac;
	}

	public MacAddress() {
		super();
		mac = new byte[ETH_MAC_LEN];
	}
	
	public int initWithByteArray(byte[] array, int start) {
		if(array.length -start < ETH_MAC_LEN) {
			return ErrorCode.INVALID_LEN;
		}
		
		for(int i=0; i<ETH_MAC_LEN; i++){
			mac[i] = array[i+start];
		}
		
		return ErrorCode.OK;
	}
}
