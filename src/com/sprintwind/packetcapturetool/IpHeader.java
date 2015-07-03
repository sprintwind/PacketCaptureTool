package com.sprintwind.packetcapturetool;

public class IpHeader implements ByteInitializer{
	
	static final int IP_VERSION_IPV4 = 4;
	static final int IP_VERSON_IPV6 = 6;
	
	static final int IP_HDR_MIN_LEN = 20;
	static final int IP_PROTO_ICMP = 1;
	static final int IP_PROTO_TCP = 6;
	static final int IP_PROTO_UDP = 17;
	
	private char version;
	private char headerLen;
	private char TOS;
	private short totalLen;
	private short identification;
	private char flags;
	private short offset;
	private char TTL;
	private char protocol;
	private short checksum;
	private IpAddress sourceIp;
	private IpAddress destIp;
	private byte[] options;
	
	public IpHeader() {
		super();
		this.sourceIp = new IpAddress();
		this.destIp = new IpAddress();
	}
	
	public int getVersion() {
		return version&0xff;
	}

	public void setVersion(char version) {
		this.version = version;
	}

	public int getHeaderLen() {
		return (headerLen&0xff)*4;
	}

	public void setHeaderLen(char headerLen) {
		this.headerLen = headerLen;
	}

	public int getTOS() {
		return (TOS&0xff);
	}

	public void setTOS(char tOS) {
		TOS = tOS;
	}

	public int getTotalLen() {
		return (totalLen&0xffff);
	}

	public void setTotalLen(short totalLen) {
		this.totalLen = totalLen;
	}

	public int getIdentification() {
		return (identification&0xffff);
	}

	public void setIdentification(short identification) {
		this.identification = identification;
	}

	public int getFlags() {
		return flags&0x7;
	}

	public void setFlags(char flags) {
		this.flags = flags;
	}

	public short getOffset() {
		return offset;
	}

	public void setOffset(short offset) {
		this.offset = offset;
	}

	public int getTTL() {
		return TTL&0xff;
	}

	public void setTTL(char TTL) {
		this.TTL = TTL;
	}

	public int getProtocol() {
		return protocol&0xff;
	}

	public void setProtocol(char protocol) {
		this.protocol = protocol;
	}

	public int getChecksum() {
		return checksum&0xffff;
	}

	public void setChecksum(short checksum) {
		this.checksum = checksum;
	}

	public IpAddress getSourceIp() {
		return sourceIp;
	}

	public void setSourceIp(IpAddress sourceIp) {
		this.sourceIp = sourceIp;
	}

	public IpAddress getDestIp() {
		return destIp;
	}

	public void setDestIp(IpAddress destIp) {
		this.destIp = destIp;
	}

	public byte[] getOptions() {
		return options;
	}

	public void setOptions(byte[] options) {
		this.options = options;
	}
	
	public String getProtocolString() {
		int protocol = this.protocol&0xff;
		String protocolString = new String();
		switch(protocol) {
		case IP_PROTO_ICMP:
			protocolString += "ICMP(";
			break;
		case IP_PROTO_TCP:
			protocolString += "TCP(";
			break;
		case IP_PROTO_UDP:
			protocolString +=  "UDP(";
			break;
		default:
			protocolString +=  "UNKNOWN(";
		}
		
		protocolString += protocol+")";
		return protocolString;
	}

	@Override
	public int initWithByteArray(byte[] array, int start) {
		
		int newStart = start;
		
		if(array.length - start < IP_HDR_MIN_LEN){
			return ErrorCode.INVALID_LEN;
		}
		
		this.version = (char) ByteTranslater.netBitFromByte(array, newStart, 0, 3);
		//System.out.println("ip version:"+String.format("%x", (int)this.version));
		
		if(IP_VERSION_IPV4 == (int)this.version){
			this.headerLen = (char) ByteTranslater.netBitFromByte(array, newStart, 4, 7);
			
			/* 数组的长度不能小于IP头部长度 */
			if(array.length - start < (this.headerLen << 2)){
				return ErrorCode.INVALID_LEN;
			}
			
			newStart += ByteTranslater.BYTE_LEN_OF_CHAR;
			this.TOS = ByteTranslater.charFromByte(array, newStart);
			
			newStart += ByteTranslater.BYTE_LEN_OF_CHAR;
			this.totalLen = ByteTranslater.netShortFromByte(array, newStart);
			
			newStart += ByteTranslater.BYTE_LEN_OF_SHORT;
			this.identification = ByteTranslater.netShortFromByte(array, newStart);
			
			newStart += ByteTranslater.BYTE_LEN_OF_SHORT;
			this.flags = (char) ByteTranslater.hostBitFromByte(array, newStart, 0, 2);
			this.offset = (short) ByteTranslater.hostBitFromByte(array, newStart, 3, 15);
			
			newStart += ByteTranslater.BYTE_LEN_OF_SHORT;
			this.TTL = ByteTranslater.charFromByte(array, newStart);
			
			newStart += ByteTranslater.BYTE_LEN_OF_CHAR;
			this.protocol = ByteTranslater.charFromByte(array, newStart);
			
			newStart += ByteTranslater.BYTE_LEN_OF_CHAR;
			this.checksum = ByteTranslater.netShortFromByte(array, newStart);
			
			newStart += ByteTranslater.BYTE_LEN_OF_SHORT;
			this.sourceIp.setIpv4Address(ByteTranslater.netIntFromByte(array, newStart));
			
			newStart += ByteTranslater.BYTE_LEN_OF_INT;
			this.destIp.setIpv4Address(ByteTranslater.netIntFromByte(array, newStart));
			
			newStart += ByteTranslater.BYTE_LEN_OF_INT;
			int optionLen = (this.headerLen << 2) - IP_HDR_MIN_LEN;
			if(optionLen <= 0){
				return ErrorCode.OK;
			}
			
			this.options = new byte[optionLen];
			for(int i=0; i<optionLen;i++){
				this.options[i] = array[newStart+i];
			}
		}
		
		
		return ErrorCode.OK;
	}
	
	
}
