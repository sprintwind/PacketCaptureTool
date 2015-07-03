package com.sprintwind.packetcapturetool;

import java.util.Locale;

public class IpAddress {
	static final int IP_TYPE_IPV4 = 4;
	static final int IP_TYPE_IPV6 = 6;
	
	static final int IPV4_BYTE_LEN = 4;
	static final int IPV6_BYTE_LEN = 16;
	
	static final int IPV6_INT_LEN = 4;
	
	private int ipv4Address;
	private int[] ipv6Address;
	
	public int getIpv4Address() {
		return ipv4Address;
	}
	
	public String getIpv4AddressString() {
		//System.out.println("ipv4Address:"+String.format("%x", ipv4Address));
		return String.format(Locale.CHINA, "%d.%d.%d.%d", ((ipv4Address&0xff000000)>>24)&0xff, ((ipv4Address&0xff0000)>>16)&0xff, ((ipv4Address&0xff00)>>8)&0xff, ipv4Address&0xff);
	}

	public void setIpv4Address(int ipv4Address) {
		this.ipv4Address = ipv4Address;
	}

	public int[] getIpv6Address() {
		return ipv6Address;
	}

	public void setIpv6Address(int[] ipv6Address) {
		this.ipv6Address = ipv6Address;
	}

	public IpAddress(){
		super();
		
			ipv6Address = new int[IPV6_INT_LEN];
	}
	
}
