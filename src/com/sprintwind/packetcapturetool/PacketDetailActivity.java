package com.sprintwind.packetcapturetool;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PacketDetailActivity extends Activity {
	
	private TextView txtvwPacketCharData;
	private TextView txtvwPacketStringData;
	
	private final String TEXTVIEW_TEXT_ALIGN = "  ";
	private final String LOG_TAG = "sprintwind";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_packet_detail);
		
		Intent intent = this.getIntent();
		byte[] packetContent = intent.getByteArrayExtra("packet_content");
		
		txtvwPacketCharData = (TextView) findViewById(R.id.txtvwPacketCharData);
		txtvwPacketStringData = (TextView) findViewById(R.id.txtvwPacketStringData);
		initRawData(txtvwPacketCharData, txtvwPacketStringData, packetContent);
		
		LinearLayout lnrlytPacketLayer = (LinearLayout) findViewById(R.id.lnrlytPacketLayer);
		LayoutInflater inflater = LayoutInflater.from(this.getApplicationContext());
		
		int start = 0;
		
		/* 添加以太头视图 */
		View view = inflater.inflate(R.layout.view_ether_header, null);
		EtherHeader etherHeader = new EtherHeader();
		etherHeader.initWithByteArray(packetContent, start);
		initEtherHeaderView(view, etherHeader, start);
		lnrlytPacketLayer.addView(view);
		
		start += EtherHeader.ETH_HDR_LEN;
		
		/* 如果是ARP协议,解析并添加ARP头 */
		if(etherHeader.getProtocol() == EtherHeader.ETH_PROTO_ARP) {
			/* 添加ARP头视图 */
			view = inflater.inflate(R.layout.view_arp_header, null);
			ArpHeader arpHeader = new ArpHeader();
			arpHeader.initWithByteArray(packetContent, start);
			initArpHeaderView(view, arpHeader, start);
			lnrlytPacketLayer.addView(view);
		}
		/* 如果是IP协议,解析并添加IP头 */
		else if(etherHeader.getProtocol() == EtherHeader.ETH_PROTO_IP) {
			/* 添加IP头视图 */
			view = inflater.inflate(R.layout.view_ip_header, null);
			IpHeader ipHeader = new IpHeader();
			ipHeader.initWithByteArray(packetContent, start);
			initIpHeaderView(view, ipHeader, start);
			lnrlytPacketLayer.addView(view);
			
			start += ipHeader.getHeaderLen();
			
			switch(ipHeader.getProtocol()){
			case IpHeader.IP_PROTO_ICMP:
				break;
			case IpHeader.IP_PROTO_TCP:
				/* 添加TCP头视图 */
				view = inflater.inflate(R.layout.view_tcp_header, null);
				TcpHeader tcpHeader = new TcpHeader();
				tcpHeader.initWithByteArray(packetContent, start);
				initTcpHeaderView(view, tcpHeader, start);
				lnrlytPacketLayer.addView(view);
				break;
			case IpHeader.IP_PROTO_UDP:
				/* 添加UDP头视图 */
				view = inflater.inflate(R.layout.view_udp_header, null);
				UdpHeader udpHeader = new UdpHeader();
				udpHeader.initWithByteArray(packetContent, start);
				initUdpHeaderView(view, udpHeader, start);
				lnrlytPacketLayer.addView(view);
				break;
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.packet_detail, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void initEtherHeaderView(View view, EtherHeader etherHeader, int start) {
		
		OnEtherHeaderItemClickListener onClickListener = new OnEtherHeaderItemClickListener();
		
		TextView txtvwEtherDestMac = (TextView) view.findViewById(R.id.txtvwEtherDestMac);
		txtvwEtherDestMac.setText("目的MAC:"+TEXTVIEW_TEXT_ALIGN+etherHeader.getDestMac().getMacString());
		txtvwEtherDestMac.setOnClickListener(onClickListener);
		
		TextView txtvwEtherSourceMac = (TextView) view.findViewById(R.id.txtvwEtherSourceMac);
		txtvwEtherSourceMac.setText("源MAC:"+TEXTVIEW_TEXT_ALIGN+etherHeader.getSourceMac().getMacString());
		txtvwEtherSourceMac.setOnClickListener(onClickListener);
		
		TextView txtvwEtherProtocol = (TextView) view.findViewById(R.id.txtvwEtherProto);
		txtvwEtherProtocol.setText("协议类型:"+TEXTVIEW_TEXT_ALIGN+etherHeader.getProtocolString());
		txtvwEtherProtocol.setOnClickListener(onClickListener);
		
		/* 设置边距 */
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		layoutParams.setMargins(5, 5, 5, 5);
		view.setLayoutParams(layoutParams);
	}
	
	private class OnEtherHeaderItemClickListener implements OnClickListener {

		@Override
		public void onClick(View arg0) {
			// TODO Auto-generated method stub
			int originalBeginIndex = 0, originalEndIndex = 0;
			int charDataStart = 0, charDataEnd = 0;
			int stringDataStart = 0, stringDataEnd = 0;
			
			String charData = txtvwPacketCharData.getText().toString();
			SpannableStringBuilder ssbCharData = new SpannableStringBuilder(charData);
			
			String stringData = txtvwPacketStringData.getText().toString();
			SpannableStringBuilder ssbStringData = new SpannableStringBuilder(stringData);
			
			switch(arg0.getId()){
			case R.id.txtvwEtherDestMac:
				originalBeginIndex = 0;
				originalEndIndex = MacAddress.ETH_MAC_LEN;
				break;
			case R.id.txtvwEtherSourceMac:
				originalBeginIndex = MacAddress.ETH_MAC_LEN;
				originalEndIndex = originalBeginIndex + MacAddress.ETH_MAC_LEN;
				break;
			case R.id.txtvwEtherProto:
				originalBeginIndex = MacAddress.ETH_MAC_LEN *2;
				originalEndIndex = originalBeginIndex + ByteTranslater.BYTE_LEN_OF_SHORT;
				break;
			default:
				break;
				
			}
			
			charDataStart = getCharDataBeginIndex(originalBeginIndex);
			charDataEnd = getCharDataEndIndex(originalEndIndex);
			
			stringDataStart = getStringDataBeginIndex(originalBeginIndex);
			stringDataEnd = getStringDataEndIndex(originalEndIndex);

			ssbCharData.setSpan(new BackgroundColorSpan(Color.parseColor("#ADD8E6")), charDataStart, charDataEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			txtvwPacketCharData.setText(ssbCharData);
			
			ssbStringData.setSpan(new BackgroundColorSpan(Color.parseColor("#ADD8E6")), stringDataStart, stringDataEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			txtvwPacketStringData.setText(ssbStringData);
		}
		
	}
	
	private int getCharDataBeginIndex(int originalIndex) {
		if(originalIndex < 8) {
			return originalIndex*3;
		}
		return originalIndex/8*24 + originalIndex%8*3 + 1;
	}
	
	private int getCharDataEndIndex(int originalIndex) {
		if(originalIndex < 8){
			return originalIndex/8*24 + originalIndex%8*3 - 1;
		}
		return originalIndex/8*24 + originalIndex%8*3;
	}
	
	private int getStringDataBeginIndex(int originalIndex) {
		return originalIndex/8*9 + originalIndex%8;
	}
	
	private int getStringDataEndIndex(int originalIndex) {
		return originalIndex/8*9 + originalIndex%8;
	}
	
	private void initArpHeaderView(View view, ArpHeader arpHeader, int start) {
		TextView txtvwArpHardwareType = (TextView) view.findViewById(R.id.txtvwArpHardwareType);
		txtvwArpHardwareType.setText("硬件类型:"+TEXTVIEW_TEXT_ALIGN+arpHeader.getHardwareTypeString());
		
		TextView txtvwArpProtocolType = (TextView) view.findViewById(R.id.txtvwArpProtocolType);
		txtvwArpProtocolType.setText("协议类型:"+TEXTVIEW_TEXT_ALIGN+arpHeader.getProtocolTypeString());
		
		TextView txtvwArpHardwareAddressLen = (TextView) view.findViewById(R.id.txtvwArpHardwareAddressLen);
		txtvwArpHardwareAddressLen.setText("硬件地址长度:"+TEXTVIEW_TEXT_ALIGN+arpHeader.getHardwareAddressLen());
		
		TextView txtvwArpProtocolAddressLen = (TextView) view.findViewById(R.id.txtvwArpProtocolAddressLen);
		txtvwArpProtocolAddressLen.setText("协议地址长度:"+TEXTVIEW_TEXT_ALIGN+arpHeader.getProtocolAddressLen());
		
		TextView txtvwArpOperation = (TextView) view.findViewById(R.id.txtvwArpOperation);
		txtvwArpOperation.setText("操作类型:"+TEXTVIEW_TEXT_ALIGN+arpHeader.getOperationString());
		
		TextView txtvwArpSourceHardwareAddress = (TextView) view.findViewById(R.id.txtvwArpSourceHardwareAddress);
		txtvwArpSourceHardwareAddress.setText("发送者硬件地址:"+TEXTVIEW_TEXT_ALIGN+arpHeader.getSourceHardwareAddressString());
		
		TextView txtvwArpSourceIpAddress = (TextView) view.findViewById(R.id.txtvwArpSourceIpAddress);
		txtvwArpSourceIpAddress.setText("发送者IP地址:"+TEXTVIEW_TEXT_ALIGN+arpHeader.getSourceProtocolAddress().getIpv4AddressString());
		
		TextView txtvwArpDestHardwareAddress = (TextView) view.findViewById(R.id.txtvwArpDestHardwareAddress);
		txtvwArpDestHardwareAddress.setText("接收者硬件地址:"+TEXTVIEW_TEXT_ALIGN+arpHeader.getDestHardwareAddressString());
		
		TextView txtvwArpDestIpAddress = (TextView) view.findViewById(R.id.txtvwArpDestIpAddress);
		txtvwArpDestIpAddress.setText("接收者IP地址:"+TEXTVIEW_TEXT_ALIGN+arpHeader.getDestProtocolAddress().getIpv4AddressString());
		
		/* 设置边距 */
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		layoutParams.setMargins(5, 5, 5, 5);
		view.setLayoutParams(layoutParams);
	}
	
	private void initIpHeaderView(View view, IpHeader ipHeader, int start) {
		
		TextView txtvwIpVersion = (TextView) view.findViewById(R.id.txtvwIpVersion);
		txtvwIpVersion.setText("版本号:"+TEXTVIEW_TEXT_ALIGN+ipHeader.getVersion());
		
		TextView txtvwIpHeaderLength = (TextView) view.findViewById(R.id.txtvwIpHeaderLength);
		txtvwIpHeaderLength.setText("头部长度:"+TEXTVIEW_TEXT_ALIGN+ipHeader.getHeaderLen());
		
		TextView txtvwIpTOS = (TextView) view.findViewById(R.id.txtvwIpTOS);
		txtvwIpTOS.setText("TOS:"+TEXTVIEW_TEXT_ALIGN+ipHeader.getTOS());
		
		TextView txtvwIpTotalLength = (TextView) view.findViewById(R.id.txtvwIpTotalLength);
		txtvwIpTotalLength.setText("总长度:"+TEXTVIEW_TEXT_ALIGN+ipHeader.getTotalLen());
		
		TextView txtvwIpIdentification = (TextView) view.findViewById(R.id.txtvwIpIdentification);
		txtvwIpIdentification.setText("ID:"+TEXTVIEW_TEXT_ALIGN+ipHeader.getIdentification());
		
		TextView txtvwIpFlags = (TextView) view.findViewById(R.id.txtvwIpFlags);
		txtvwIpFlags.setText("标志:"+TEXTVIEW_TEXT_ALIGN+ipHeader.getFlags());
		
		TextView txtvwIpOffset = (TextView) view.findViewById(R.id.txtvwIpOffset);
		txtvwIpOffset.setText("分片偏移:"+TEXTVIEW_TEXT_ALIGN+ipHeader.getOffset());
		
		TextView txtvwIpTTL = (TextView) view.findViewById(R.id.txtvwIpTTL);
		txtvwIpTTL.setText("TTL:"+TEXTVIEW_TEXT_ALIGN+ipHeader.getTTL());
		
		TextView txtvwIpProtocol = (TextView) view.findViewById(R.id.txtvwIpProtocol);
		txtvwIpProtocol.setText("协议类型:"+TEXTVIEW_TEXT_ALIGN+ipHeader.getProtocolString());
		
		TextView txtvwIpChecksum = (TextView) view.findViewById(R.id.txtvwIpChecksum);
		txtvwIpChecksum.setText("校验和:"+TEXTVIEW_TEXT_ALIGN+ipHeader.getChecksum());
		
		TextView txtvwIpSourceIP = (TextView) view.findViewById(R.id.txtvwIpSourceIP);
		txtvwIpSourceIP.setText("源IP:"+TEXTVIEW_TEXT_ALIGN+ipHeader.getSourceIp().getIpv4AddressString());
		
		TextView txtvwIpDestIP = (TextView) view.findViewById(R.id.txtvwIpDestIP);
		txtvwIpDestIP.setText("目的IP:"+TEXTVIEW_TEXT_ALIGN+ipHeader.getDestIp().getIpv4AddressString());
		
		/* TODO: IP选项 */
		
		/* 设置边距 */
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		layoutParams.setMargins(5, 5, 5, 5);
		view.setLayoutParams(layoutParams);
		
	}
	
	private void initUdpHeaderView(View view, UdpHeader udpHeader, int start) {
		TextView txtvwUdpSourcePort = (TextView) view.findViewById(R.id.txtvwUdpSourcePort);
		txtvwUdpSourcePort.setText("源端口号:"+TEXTVIEW_TEXT_ALIGN+udpHeader.getSourcePort());
		
		TextView txtvwUdpDestPort = (TextView) view.findViewById(R.id.txtvwUdpDestPort);
		txtvwUdpDestPort.setText("目的端口号:"+TEXTVIEW_TEXT_ALIGN+udpHeader.getDestPort());
		
		TextView txtvwUdpLength = (TextView) view.findViewById(R.id.txtvwUdpLength);
		txtvwUdpLength.setText("长度:"+TEXTVIEW_TEXT_ALIGN+udpHeader.getLength());
		
		TextView txtvwUdpCheckSum = (TextView) view.findViewById(R.id.txtvwUdpCheckSum);
		txtvwUdpCheckSum.setText("校验和:"+TEXTVIEW_TEXT_ALIGN+udpHeader.getChecksum());
		
		/* 设置边距 */
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		layoutParams.setMargins(5, 5, 5, 5);
		view.setLayoutParams(layoutParams);
	}
	
	private void initTcpHeaderView(View view, TcpHeader tcpHeader, int start) {
		TextView txtvwTcpSourcePort = (TextView) view.findViewById(R.id.txtvwTcpSourcePort);
		txtvwTcpSourcePort.setText("源端口号:"+TEXTVIEW_TEXT_ALIGN+tcpHeader.getSourcePort());
		
		TextView txtvwTcpDestPort = (TextView) view.findViewById(R.id.txtvwTcpDestPort);
		txtvwTcpDestPort.setText("目的端口号:"+TEXTVIEW_TEXT_ALIGN+tcpHeader.getDestPort());
		
		TextView txtvwTcpSeqNumber = (TextView) view.findViewById(R.id.txtvwTcpSeqNumber);
		txtvwTcpSeqNumber.setText("序号:"+TEXTVIEW_TEXT_ALIGN+tcpHeader.getSequenceNumber());
		
		TextView txtvwTcpAckNumber = (TextView) view.findViewById(R.id.txtvwTcpAckNumber);
		txtvwTcpAckNumber.setText("应答序号:"+TEXTVIEW_TEXT_ALIGN+tcpHeader.getAckNumber());
		
		TextView txtvwTcpDataOffset = (TextView) view.findViewById(R.id.txtvwTcpDataOffset);
		txtvwTcpDataOffset.setText("头长度:"+TEXTVIEW_TEXT_ALIGN+tcpHeader.getHeaderLength());
		
		TextView txtvwTcpReserve = (TextView) view.findViewById(R.id.txtvwTcpReserve);
		txtvwTcpReserve.setText("保留字段:"+TEXTVIEW_TEXT_ALIGN+tcpHeader.getReserve());
		
		TextView txtvwTcpFlags = (TextView) view.findViewById(R.id.txtvwTcpFlags);
		txtvwTcpFlags.setText("标志位:"+TEXTVIEW_TEXT_ALIGN+tcpHeader.getFlagsString());
		
		TextView txtvwTcpWindowSize = (TextView) view.findViewById(R.id.txtvwTcpWindowSize);
		txtvwTcpWindowSize.setText("窗口大小:"+TEXTVIEW_TEXT_ALIGN+tcpHeader.getWindowSize());
		
		TextView txtvwTcpCheckSum = (TextView) view.findViewById(R.id.txtvwTcpCheckSum);
		txtvwTcpCheckSum.setText("校验和:"+TEXTVIEW_TEXT_ALIGN+tcpHeader.getChecksum());
		
		TextView txtvwTcpUrgent = (TextView) view.findViewById(R.id.txtvwTcpUrgent);
		txtvwTcpUrgent.setText("紧急指针:"+TEXTVIEW_TEXT_ALIGN+tcpHeader.getUrgent());
		
		TextView txtvwTcpOptions = (TextView) view.findViewById(R.id.txtvwTcpOptions);
		String tcpOptionsString = new String();
		tcpOptionsString += "选项:"+TEXTVIEW_TEXT_ALIGN;
		
		byte[] options = tcpHeader.getOptions();
		int count = 0;
		if(null != options) {
			for(byte data:options) {
				tcpOptionsString += String.format("%02x ", data);
				++count;
				if(count%8 == 0){
					tcpOptionsString += '\n';
				}
			}
		}
		
		txtvwTcpOptions.setText(tcpOptionsString);
		
		/* 设置边距 */
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		layoutParams.setMargins(5, 5, 5, 5);
		view.setLayoutParams(layoutParams);
	}

	private void initRawData(TextView txtvwCharData, TextView txtvwStringData, byte[] packetContent) {
		String charData = new String();
		String stringData = new String();
		
		int count = 0;
		for(byte data:packetContent) {
			charData = charData + String.format("%02x", data) + " ";
			stringData = stringData + (char)data;
			++count;
			
			if(count%8 == 0) {
				charData += '\n';
				stringData += '\n';
			}
		}
		
		txtvwCharData.setText(charData);
		txtvwStringData.setText(stringData);
	}
	
}
