package com.sprintwind.packetcapturetool;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class PacketBriefActivity extends Activity {
	
	static final int READ_BUFFER_SIZE = 65535;
	private static final String LOG_TAG = "sprintwind";
	
	private static final String PACKET_DATE = "PacketDate";
	private static final String PACKET_SRC_MAC = "PacketSrcMac";
	private static final String PACKET_DST_MAC = "PacketDstMac";
	private static final String PACKET_SRC_IP = "PacketSrcIp";
	private static final String PACKET_DST_IP = "PacketDstIp";
	private static final String PACKET_PROTOCOL = "PacketProtocol";
	private static final String PACKET_LENGTH = "PacketLength";
	
	private ListView lstvwPacketBrief;
	
	
	private String fileName;
	
	private ArrayList<PcapPacket> packetList;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_packet_brief);
		
		lstvwPacketBrief = (ListView) findViewById(R.id.lstvwBrief);
		
		
		packetList = new ArrayList<PcapPacket>();
		
		Intent intent = getIntent();
		String strFilePath = intent.getStringExtra("cap_file");
		
		if(ErrorCode.OK != analyzePcapFile(strFilePath)){
			Toast.makeText(getApplicationContext(), "解析抓包文件"+strFilePath+"失败", Toast.LENGTH_SHORT).show();
		}
		
		ArrayList<HashMap<String, Object>> listItem = new ArrayList<HashMap<String, Object>>();
		for(int i=0; i<packetList.size(); i++){
			HashMap<String, Object> hashMap = new HashMap<String, Object> ();
			
			PacketBrief packetBrief = new PacketBrief();
			packetBrief.initWithPcapPacket(packetList.get(i));
			hashMap.put(PACKET_DATE, packetBrief.getTimeString());
			hashMap.put(PACKET_SRC_MAC, packetBrief.getSourceMac().getMacString());
			hashMap.put(PACKET_DST_MAC, packetBrief.getDestMac().getMacString());
			hashMap.put(PACKET_SRC_IP, packetBrief.getSourceIp().getIpv4AddressString());
			hashMap.put(PACKET_DST_IP, packetBrief.getDestIp().getIpv4AddressString());
			hashMap.put(PACKET_PROTOCOL, packetBrief.getProtocolString());
			hashMap.put(PACKET_LENGTH, packetBrief.getLength());
			
			listItem.add(hashMap);
			
		}
		
		SimpleAdapter simpleAdapter = new SimpleAdapter(this, listItem, R.layout.packet_brief_listview_item,
				new String[]{PACKET_DATE, PACKET_SRC_IP, PACKET_DST_IP, PACKET_PROTOCOL, PACKET_LENGTH},
				new int[]{R.id.PacketDate, R.id.PacketSrcIp, R.id.PacketDstIp, R.id.PacketProtocol, R.id.PacketLength});
		
		lstvwPacketBrief.setAdapter(simpleAdapter);
		lstvwPacketBrief.setOnItemClickListener(new onListViewItemClickListener());
		
		this.setTitle(fileName);
	}

	public class onListViewItemClickListener implements OnItemClickListener {


		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
			// TODO Auto-generated method stub
			/*
			Toast toast = Toast.makeText(getApplicationContext(), "查看报文详细信息功能正在开发中, 敬请期待!", Toast.LENGTH_SHORT);
			toast.setGravity(Gravity.CENTER, 0, 0);
			toast.show();
			*/
			PcapPacket pcapPacket = packetList.get(arg2);
			
			
			Intent intent = new Intent(PacketBriefActivity.this.getApplicationContext(), PacketDetailActivity.class);
			intent.putExtra("packet_content", pcapPacket.getPacketContent());
			startActivity(intent);
		}
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.analyze, menu);
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
	
	public int analyzePcapFile(String filePath){
		
		File file = new File(filePath);
		if(!file.exists()){
			Toast.makeText(getApplicationContext(), "文件"+filePath+"不存在", Toast.LENGTH_SHORT).show();
			return ErrorCode.FILE_NOT_EXIST;
		}
		
		fileName = file.getName();

		try {
			FileInputStream fin = new FileInputStream(file);
	
			byte[] buffer = new byte[PcapFileHeader.PCAP_FILE_HDR_LEN];
	
			try {
	
			/* 读取pcap文件头 */
			fin.read(buffer, 0, PcapFileHeader.PCAP_FILE_HDR_LEN);
	
			PcapFileHeader pcapFileHeader = new PcapFileHeader();
			int retCode = pcapFileHeader.initWithByteArray(buffer, 0);
			if(ErrorCode.OK != retCode){
				Log.e(LOG_TAG, "init pcap file header failed, return code is "+retCode);
				Toast.makeText(getApplicationContext(), "解析pcap文件头失败", Toast.LENGTH_SHORT).show();
				fin.close();
				return ErrorCode.INVALID_PCAP_HEADER;
			}
	
			//pcapFileHeader.print();
	
			/* 读取pcap报文头 */
			buffer = new byte[PcapPacketHeader.PCAP_PKT_HDR_LEN];
	
			while(fin.read(buffer, 0, PcapPacketHeader.PCAP_PKT_HDR_LEN) > 0){
				PcapPacketHeader pcapPacketHeader = new PcapPacketHeader();
		
				retCode = pcapPacketHeader.initWithByteArray(buffer, 0);
				if(ErrorCode.OK != retCode){
					Log.e(LOG_TAG, "init pcap packet header failed, return code is "+retCode);
					Toast.makeText(getApplicationContext(), "解析pcap报文头失败", Toast.LENGTH_SHORT).show();
					fin.close();
					return ErrorCode.INVALID_PCAP_PACKET;
				}
		
				//pcapPacketHeader.print();
		
				/* 根据pcap报文头的长度读取pcap报文内容 */
				buffer = new byte[pcapPacketHeader.getCapLen()];
		
				fin.read(buffer, 0, pcapPacketHeader.getCapLen());
		
				PcapPacket pcapPacket = new PcapPacket();
				pcapPacket.setPacketHeader(pcapPacketHeader);
				pcapPacket.setPacketContent(buffer);
				
				packetList.add(pcapPacket);
			}
	
			fin.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return ErrorCode.IO_EXCEPTION;
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return ErrorCode.FILE_NOT_FOUND_EXCEPTION;
			}
		
		return ErrorCode.OK;
	}
	
	/*
	@Override
    public void onBackPressed() {
        Log.i(LOG_TAG, "onBackPressed");
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }
    */

}
