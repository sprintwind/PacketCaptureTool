package com.sprintwind.packetcapturetool;

public class PcapPacket{

static final int PCAP_PKT_MIN_LEN = PcapPacketHeader.PCAP_PKT_HDR_LEN;

private PcapPacketHeader packetHeader;
private byte[] packetContent;

public PcapPacketHeader getPacketHeader() {
return packetHeader;
}
public void setPacketHeader(PcapPacketHeader packetHeader) {
this.packetHeader = packetHeader;
}
public byte[] getPacketContent() {
return packetContent;
}
public void setPacketContent(byte[] packetContent) {
	int contentLen = packetContent.length;
	if(contentLen <= 0){
		return;
	}
	
	this.packetContent = new byte[contentLen];
	
	for(int i=0; i<packetContent.length; i++){
		this.packetContent[i] = packetContent[i];
	}
}
}