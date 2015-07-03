package com.sprintwind.packetcapturetool;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.util.Log;

public class PacketBrief implements ByteInitializer{
	
	static final int PKT_PROTO_ARP = EtherHeader.ETH_PROTO_ARP;
	static final int PKT_PROTO_ICMP = IpHeader.IP_PROTO_ICMP;
	static final int PKT_PROTO_TCP = IpHeader.IP_PROTO_TCP;
	static final int PKT_PROTO_UDP = IpHeader.IP_PROTO_UDP;
	static final int PKT_PROTO_UNKOWN = 0x0;
	
	private TimeVal time;
	private MacAddress sourceMac;
	private MacAddress destMac;
	private IpAddress sourceIp;
	private IpAddress destIp;
	private int protocol;
	private int length;
	
	
	public String getTimeString(){
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return formatter.format((long)this.time.sec*1000)+":"+this.time.uSec;
	}

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
	public int getProtocol() {
		return protocol;
	}
	
	public String getProtocolString() {
		int protocol = this.protocol&0xffff;
		switch(protocol) {
		case PKT_PROTO_ARP:
			return "ARP";
		case PKT_PROTO_ICMP:
			return "ICMP";
		case PKT_PROTO_TCP:
			return "TCP";
		case PKT_PROTO_UDP:
			return "UDP";
		default:
			return "UNKOWN("+String.format("0x%04x", protocol)+")";
			
		}
	}
	public void setProtocol(short protocol) {
		this.protocol = protocol;
	}
	public int getLength() {
		return length;
	}
	public void setLength(int length) {
		this.length = length;
	}
	
	public PacketBrief() {
		super();
		
		this.sourceMac = new MacAddress();
		this.destMac = new MacAddress();
		this.sourceIp = new IpAddress();
		this.destIp = new IpAddress();
	}
	
	
	public int initWithPcapPacket(PcapPacket packet){
		this.time = packet.getPacketHeader().getTimeVal();
		this.length = packet.getPacketHeader().getOriginLen();
		
		return this.initWithByteArray(packet.getPacketContent(), 0);
	}
	
	@Override
	public int initWithByteArray(byte[] array, int start) {
		
		int newStart = start;
		
		EtherHeader etherHeader = new EtherHeader();
		int error = etherHeader.initWithByteArray(array, newStart);
		if(ErrorCode.OK != error){
			return error;
		}
		
		//etherHeader.print();
		this.sourceMac = etherHeader.getSourceMac();
		this.destMac = etherHeader.getDestMac();
		
		newStart += EtherHeader.ETH_HDR_LEN;
		int protocol = etherHeader.getProtocol()&0xffff;
		switch(protocol){
		
		case EtherHeader.ETH_PROTO_ARP:
			ArpHeader arpHeader = new ArpHeader();
			error = arpHeader.initWithByteArray(array, newStart);
			if(ErrorCode.OK != error){
				return error;
			}
			this.protocol = EtherHeader.ETH_PROTO_ARP;
			this.sourceIp = arpHeader.getSourceProtocolAddress();
			this.destIp = arpHeader.getDestProtocolAddress();
			break;
		case EtherHeader.ETH_PROTO_IP:
			IpHeader ipHeader = new IpHeader();
			error = ipHeader.initWithByteArray(array, newStart);
			if(ErrorCode.OK != error){
				return error;
			}
			this.protocol = (short) ipHeader.getProtocol();
			this.sourceIp = ipHeader.getSourceIp();
			this.destIp = ipHeader.getDestIp();
			break;
		default:
			this.protocol = etherHeader.getProtocol();
			
		}
		
		return ErrorCode.OK;
	}
	
	
}
