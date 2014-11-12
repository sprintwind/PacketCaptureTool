package com.example.packetcapturetool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.example.packetcapturetool.ShellUtils.CommandResult;

import android.R.color;
import android.support.v7.app.ActionBarActivity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends ActionBarActivity {
	
	private static int BUFFER_SIZE = 1024;
	private final int MAX_CAPSIZE_LEN = 5;
	private final int MAX_SAVE_FILE_NAME_LEN = 32;
	private final int MIN_CAP_SIZE = 24;
	private final int MAX_CAP_SIZE = 65535;
	private final int MAX_INTEGER_LEN = 10;
	
	private final String CAP_TOOL = "cap_tool";
	private final String PCAP_FILE_SUFFIX = ".pcap";
	private final String STATS_FILE = ".cap_stats";
	private final String CAP_FILE_DIR = "packet_capture";

	private ArrayAdapter<String> arradpInterface;
	private ArrayAdapter<CharSequence> arradpProtocol;

	private Button btnStartCapture;
	private Spinner spinInterface;
	private Spinner spinProtocol;
	private TextView tvChoseInterface;
	private EditText etCapSize;
	private EditText etFileName;
	private TextView tvStatus;

	private Handler handler;
	private Process process;
	private String  captoolDir;
	private String sdcardDir;
	private String saveFilePath;
	
	private CommandResult cmdResult;
	private String capture_stats;
	private boolean capture = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        tvChoseInterface = (TextView) findViewById(R.id.tvChoseInterface);
        
        btnStartCapture = (Button) findViewById(R.id.btnStartCapture);
		btnStartCapture.setOnClickListener((OnClickListener) new OnBtnStartCaptureClickListener());
		
		spinInterface = (Spinner) findViewById(R.id.spinnerInterface);
		spinProtocol = (Spinner) findViewById(R.id.spinProtocol);
		
		etCapSize = (EditText) findViewById(R.id.etCapSize);
		etCapSize.setMaxWidth(MAX_CAPSIZE_LEN);
		
		etFileName = (EditText) findViewById(R.id.etFileName);
		etFileName.setMaxWidth(MAX_SAVE_FILE_NAME_LEN);
		
		SimpleDateFormat format = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
		String timeString = format.format(new Date());
		etFileName.setText(timeString);
		
		tvStatus = (TextView) findViewById(R.id.tvStatus);
		tvStatus.setText("等待执行抓包命令");
		tvStatus.setTextColor(Color.RED);
		tvStatus.setBackgroundColor(Color.GRAY);
		
		handler = new Handler();

		/* 加载网口列表 */
		loadInterface();
		
		/* 加载协议列表 */
		arradpProtocol = ArrayAdapter.createFromResource(this, R.array.protocol, android.R.layout.simple_list_item_single_choice);//new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_single_choice, R.array.protocol);
		spinProtocol.setAdapter(arradpProtocol);
		
		/* 初始化相关路径 */
		captoolDir = this.getApplicationContext().getFilesDir().getParentFile().getPath();
		sdcardDir= android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
		
		/* 创建应用目录 */
		createAppDirectory();
		
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
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
    
    public void loadInterface()
    {
    	String interfaceStr = JNIgetInterfaces();
		if(null != interfaceStr)
		{
			System.out.println("interfaceStr:"+interfaceStr);
			String[] interfaceArr = interfaceStr.split("\\|");
			
			arradpInterface = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_single_choice, interfaceArr);
			spinInterface.setAdapter(arradpInterface);
		}
		else{
			System.out.println("interfaceArray is null!");
		}
    }
    
    public void createAppDirectory()
    {
    	
    	 boolean sdCardExist = Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
    	 if(!sdCardExist)
    	 {
    		 Toast.makeText(this, "请插入外部SD存储卡", Toast.LENGTH_SHORT).show();
    		 return;
    	 }
    	 
    	 File appDir = new File(sdcardDir+"/"+CAP_FILE_DIR);
    	 if(!appDir.exists())
    	 {
    		 appDir.mkdir();
    	 }
    	 return;
    }
    
    public boolean isValidCaptureSize()
    {
    	String text = etCapSize.getText().toString().trim();
    	if(text.equals(""))
    	{
    		return false;
    	}
    	
    	int size = Integer.parseInt(text);
    	if( (size < MIN_CAP_SIZE)||(size > MAX_CAP_SIZE))
    	{
    		return false;
    	}
    	
    	return true;
    }
    
    public class OnBtnStartCaptureClickListener implements OnClickListener{

		@Override
		public void onClick(View arg0) {
			// TODO Auto-generated method stub
			/* 根据按钮的Text判断是要开始抓包还是要停止抓包 */
			if(btnStartCapture.getText().equals(getString(R.string.start_capture))){
				
				/* 拷贝抓包工具到应用安装目录 */
				moveRawFileToAppPath(R.raw.cap_tool, captoolDir);
				
				/* 检查输入的抓取长度是否合法 */
				if(!isValidCaptureSize())
				{
					Toast.makeText(getApplicationContext(), getString(R.string.invalid_capture_size), Toast.LENGTH_SHORT).show();
					return;
				}
				
				saveFilePath = sdcardDir + "/" +CAP_FILE_DIR+"/" + etFileName.getText().toString() + PCAP_FILE_SUFFIX;
				
				CaptureThread captureThread = new CaptureThread();
				
				Thread thread = new Thread(captureThread);
				thread.start();
				
				capture = true;
			    new Thread(new StatisticThread()).start();
				
				Toast.makeText(getApplicationContext(), getString(R.string.capture_started), Toast.LENGTH_SHORT).show();
				btnStartCapture.setText(R.string.stop_capture);
				tvStatus.setText("开始抓包");
				
			}
			else{
				String[] commands = {
						"busybox killall -SIGINT "+CAP_TOOL,
						"sleep 1"
						//"busybox killall -SIGKILL "+CAP_TOOL
				};
				CommandResult result = ShellUtils.execCommand(commands, true, true);
				if(result.result < 0)
				{
					Toast.makeText(getApplicationContext(), "stop failed,"+result.errorMsg, Toast.LENGTH_SHORT).show();
					return;
				}
				System.out.println(result.successMsg);
				capture = false;
				
				Toast.makeText(getApplicationContext(), getString(R.string.capture_stopped)+saveFilePath, Toast.LENGTH_LONG).show();
				btnStartCapture.setText(R.string.start_capture);
				tvStatus.setText(getString(R.string.capture_stopped)+saveFilePath);
			}
		}
		
	}
    
    public class CaptureThread implements Runnable{

		@Override
		public void run() {
			// TODO Auto-generated method stub
			
			String[] commands = {
					"chmod 755 "+captoolDir+"/"+CAP_TOOL,
					captoolDir+"/"+CAP_TOOL + " " 
					+ spinInterface.getSelectedItem().toString() 
					+ " " + spinProtocol.getSelectedItem().toString()
					+ " " + etCapSize.getText()
					+ " " + sdcardDir + "/"+CAP_FILE_DIR
					+ " " + etFileName.getText().toString() 
			};
			
			cmdResult = ShellUtils.execCommand(commands, true, true);
			if(cmdResult.result < 0)
			{
				capture = false;
				handler.post(new UpdateThread());
			}
		}
		
	}
    
    public class StatisticThread implements Runnable{

		@Override
		public void run() {
			// TODO Auto-generated method stub
			
			while(capture)
			{
				try {
					File file = new File(sdcardDir+"/" +CAP_FILE_DIR+"/"+STATS_FILE);
					if(!file.exists())
					{
						continue;
					}
					BufferedReader br = new BufferedReader(new FileReader(file));
					capture_stats = br.readLine();
					System.out.println("stats:"+capture_stats);
					//capture_count = Integer.parseInt(stats);
					br.close();
					handler.post(new UpdateThread());
					Thread.sleep(1000);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
    	
    }
	
	public class UpdateThread implements Runnable{

		@Override
		public void run() {
			// TODO Auto-generated method stub
			
				//spinInterface.setAdapter(adapter);
			if(null != cmdResult)
			{
				if(cmdResult.result < 0)
				{
					Toast.makeText(getApplicationContext(), "抓包失败，"+cmdResult.errorMsg, Toast.LENGTH_SHORT).show();
					btnStartCapture.setText(R.string.start_capture);
					return;
				}
			}
			
			if(capture&&(null != capture_stats))
			{
				tvStatus.setText("已抓取："+capture_stats);
			}
			
		}
		
	}
	
	/*
	 * get root permission
	 */
	public boolean getRootPermission()
	{
		CommandResult result = ShellUtils.execCommand("chmod "+"777 "+getPackageCodePath(), true);
		if(result.result >= 0){
			return true;
		}
		else{
			return false;
		}
	}
	
	
	public static boolean is_root(){

	    boolean res = false;

	    try{ 
	        if ((!new File("/system/bin/su").exists()) && 
	            (!new File("/system/xbin/su").exists())){
	        res = false;
	    } 
	    else {
	        res = true;
	    };
	    } 
	    catch (Exception e) {  

	    } 
	    return res;
	}
	
	public boolean isCaptoolExist()
	{
		File captool = new File(captoolDir+"/"+CAP_TOOL);
		System.out.println(captool.getAbsolutePath());
		if(captool.exists()){
			return true;
		}
		return false;
	}
	
	public void moveRawFileToAppPath(int rawFileId, String dstPath)
	{
		File dstDir = new File(dstPath);
		if(!dstDir.exists()){
			Toast.makeText(getApplicationContext(), "文件"+dstDir+"不存在", Toast.LENGTH_SHORT).show();
			return;
		}
		
		InputStream is = getResources().openRawResource(rawFileId);
		try {
			FileOutputStream fos = new FileOutputStream(dstDir+"/"+CAP_TOOL);
			byte[] buffer = new byte[1024];
			int count = 0;
			try {
				while((count = is.read(buffer)) > 0){
					fos.write(buffer, 0, count);
				}
				fos.close();
				is.close();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	public native String JNIgetInterfaces();
	public native int JNIexcuteCommand(String cmd, String args);
	public native String JNIgetErrorString(int errno);
	public native int JNIstartCapture(String dev, int proto, int cap_len);
	public native void JNIstopCapture();
	public native int JNIgetProtoValue(String proto);
	public native int JNIgetRootPermission();
	
	static{
		System.loadLibrary("PacketCaptureTool");
	}
}
