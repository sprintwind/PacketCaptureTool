package com.sprintwind.packetcapturetool;

public class ArpHeader implements ByteInitializer{
	
	static final int ARP_HARDWARE_ADDRESS_LEN = 6;
	static final int ARP_HDR_LEN = 28;
	
	static final int HARDWARE_TYPE_ETHERNET = 0x1;
	
	static final int PROTOCOL_TYPE_IP = 0x0800;
	static final int PROTOCOL_TYPE_ARP = 0x0806;
	static final int PROTOCOL_TYPE_RARP = 0x8035;
	
	static final int OPERATION_TYPE_ARP_REQUEST = 0x1;
	static final int OPERATION_TYPE_ARP_REPPLY = 0x2;
	static final int OPERATION_TYPE_RARP_REQUEST = 0x3;
	static final int OPERATION_TYPE_RARP_REPPLY = 0x4;
	
	
	private short hardwareType;
	private short protocolType;
	private char hardwareAddressLen;
	private char protocolAddressLen;
	private short operation;
	private byte[] sourceHardwareAddress;
	private IpAddress sourceProtocolAddress;
	private byte[] destHardwareAddress;
	private IpAddress destProtocolAddress;
	
	public int getHardwareType() {
		return hardwareType&0xffff;
	}
	
	public String getHardwareTypeString() {
		int hardwareType = this.hardwareType&0xffff;
		String hardwareTypeString = new String();
		
		switch(hardwareType) {
		case HARDWARE_TYPE_ETHERNET:
			hardwareTypeString += "EHTERNET(";
			break;
		default:
			hardwareTypeString += "UNKOWN(";	
		}
		
		hardwareTypeString += String.format("0x%x", hardwareType) + ")";
		return hardwareTypeString;
		
	}

	public void setHardwareType(short hardwareType) {
		this.hardwareType = hardwareType;
	}

	public int getProtocolType() {
		return protocolType&0xffff;
	}
	
	public String getProtocolTypeString() {
		int protocolType = this.protocolType&0xffff;
		String protocolTypeString = new String();
		
		switch(protocolType) {
		case PROTOCOL_TYPE_IP:
			protocolTypeString += "IP(";
			break;
		case PROTOCOL_TYPE_ARP:
			protocolTypeString += "ARP(";
			break;
		case PROTOCOL_TYPE_RARP:
			protocolTypeString += "RARP(";
			break;
		default:
			protocolTypeString += "UNKOWN(";	
		}
		
		protocolTypeString += String.format("0x%04x", protocolType) + ")";
		return protocolTypeString;
	}

	public void setProtocolType(short protocolType) {
		this.protocolType = protocolType;
	}

	public int getHardwareAddressLen() {
		return hardwareAddressLen&0xff;
	}

	public void setHardwareAddressLen(char hardwareAddressLen) {
		this.hardwareAddressLen = hardwareAddressLen;
	}

	public int getProtocolAddressLen() {
		return protocolAddressLen&0xff;
	}

	public void setProtocolAddressLen(char protocolAddressLen) {
		this.protocolAddressLen = protocolAddressLen;
	}

	public int getOperation() {
		return operation&0xffff;
	}
	
	public String getOperationString() {
		int operationType = this.operation&0xffff;
		String operatioinTypeString = new String();
		
		switch(operationType) {
		case OPERATION_TYPE_ARP_REQUEST:
			operatioinTypeString += "ARP REQUEST(";
			break;
		case OPERATION_TYPE_ARP_REPPLY:
			operatioinTypeString += "ARP REPPLY(";
			break;
		case OPERATION_TYPE_RARP_REQUEST:
			operatioinTypeString += "RARP REQUEST(";
			break;
		case OPERATION_TYPE_RARP_REPPLY:
			operatioinTypeString += "RARP REPPLY(";
			break;
		default:
			operatioinTypeString += "UNKOWN(";	
		}
		
		operatioinTypeString += String.format("0x%x", operationType) + ")";
		return operatioinTypeString;
	}

	public void setOperation(short operation) {
		this.operation = operation;
	}

	public byte[] getSourceHardwareAddress() {
		return sourceHardwareAddress;
	}
	
	public String getSourceHardwareAddressString() {
		MacAddress macAddress = new MacAddress();
		macAddress.setMac(sourceHardwareAddress);
		return macAddress.getMacString();
	}

	public void setSourceHardwareAddress(byte[] sourceHardwareAddress) {
		this.sourceHardwareAddress = sourceHardwareAddress;
	}

	public IpAddress getSourceProtocolAddress() {
		return sourceProtocolAddress;
	}

	public void setSourceProtocolAddress(int sourceProtocolAddress) {
		this.sourceProtocolAddress.setIpv4Address(sourceProtocolAddress);
	}

	public byte[] getDestHardwareAddress() {
		return destHardwareAddress;
	}
	
	public String getDestHardwareAddressString() {
		MacAddress macAddress = new MacAddress();
		macAddress.setMac(destHardwareAddress);
		return macAddress.getMacString();
	}

	public void setDestHardwareAddress(byte[] destHardwareAddress) {
		this.destHardwareAddress = destHardwareAddress;
	}

	public IpAddress getDestProtocolAddress() {
		return destProtocolAddress;
	}

	public void setDestProtocolAddress(int destProtocolAddress) {
		this.destProtocolAddress.setIpv4Address(destProtocolAddress);
	}

	public ArpHeader(){
		super();
		
		this.destHardwareAddress = new byte[ARP_HARDWARE_ADDRESS_LEN];
		this.sourceHardwareAddress = new byte[ARP_HARDWARE_ADDRESS_LEN];
		this.destProtocolAddress = new IpAddress();
		this.sourceProtocolAddress = new IpAddress();
	}
	
	@Override
	public int initWithByteArray(byte[] array, int start) {
		int newStart;
		
		if(array.length - start < ARP_HDR_LEN){
			return ErrorCode.INVALID_LEN;
		}
		
		newStart = start;
		this.hardwareType = ByteTranslater.netShortFromByte(array, newStart);
		
		newStart += ByteTranslater.BYTE_LEN_OF_SHORT;
		this.protocolType = ByteTranslater.netShortFromByte(array, newStart);
		
		newStart += ByteTranslater.BYTE_LEN_OF_SHORT;
		this.hardwareAddressLen = ByteTranslater.charFromByte(array, newStart);
		
		newStart += ByteTranslater.BYTE_LEN_OF_CHAR;
		this.protocolAddressLen = ByteTranslater.charFromByte(array, newStart);
		
		newStart += ByteTranslater.BYTE_LEN_OF_CHAR;
		this.operation = ByteTranslater.netShortFromByte(array, newStart);
		
		newStart += ByteTranslater.BYTE_LEN_OF_SHORT;
		for(int j=0; j<ARP_HARDWARE_ADDRESS_LEN; j++){
			this.sourceHardwareAddress[j] = array[newStart+j];
		}
		
		newStart += ARP_HARDWARE_ADDRESS_LEN;
		this.sourceProtocolAddress.setIpv4Address(ByteTranslater.netIntFromByte(array, newStart));
		
		newStart += ByteTranslater.BYTE_LEN_OF_INT;
		for(int i=0; i<ARP_HARDWARE_ADDRESS_LEN; i++){
			this.destHardwareAddress[i] = array[newStart+i];
		}
		
		newStart += ARP_HARDWARE_ADDRESS_LEN;
		this.destProtocolAddress.setIpv4Address(ByteTranslater.netIntFromByte(array, newStart));
		
		return ErrorCode.OK;
		
	}
	
	
}
