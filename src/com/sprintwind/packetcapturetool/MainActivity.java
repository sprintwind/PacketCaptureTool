package com.sprintwind.packetcapturetool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.sprintwind.packetcapturetool.R;
import com.sprintwind.packetcapturetool.ShellUtils.CommandResult;

import android.support.v7.app.ActionBarActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
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
	private final String LOG_TAG = "PacketCaptureTool";
	
	private enum ErrorCode{
		OK,
		ERR_FILE_NOT_EXIST,
		ERR_DEL_FILE_FAILED,
		ERR_IO_EXCEPTION,
	};

	private ArrayAdapter<String> arradpInterface;
	private ArrayAdapter<CharSequence> arradpProtocol;
	private ConnectionChangeReceiver broadcastReceiver;

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
		
		/* 检查手机是否已经获得root权限 */
		if(!is_root())
		{
			Toast.makeText(this, "您的手机还没有获得root权限，请先获取root权限", Toast.LENGTH_SHORT).show();
			return;
		}

		/* 加载网口列表 */
		//loadInterface();
		
		/* 加载协议列表 */
		arradpProtocol = ArrayAdapter.createFromResource(this, R.array.protocol, android.R.layout.simple_list_item_single_choice);//new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_single_choice, R.array.protocol);
		spinProtocol.setAdapter(arradpProtocol);
		
		/* 初始化相关路径 */
		captoolDir = this.getApplicationContext().getFilesDir().getParentFile().getPath();
		sdcardDir= android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
		
		/* 创建应用目录 */
		createAppDirectory();
		
		/* 注册网络变化通知 */
		registerNetStateReceiver();
		
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
    
    /*
     * 加载网口列表
     */
    public void loadInterface()
    {
    	String interfaceStr = JNIgetInterfaces();
		if(null != interfaceStr)
		{
			System.out.println("interfaceStr:"+interfaceStr);
			String[] interfaceArr = interfaceStr.split("\\|");
			
			arradpInterface = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_single_choice, interfaceArr);
			spinInterface.setAdapter(arradpInterface);
			Toast.makeText(getApplicationContext(), "网口信息加载完成", Toast.LENGTH_SHORT).show();
		}
		else{
			System.out.println("interfaceArray is null!");
			Toast.makeText(getApplicationContext(), "没有可用网口信息！", Toast.LENGTH_SHORT).show();
		}
    }
    
    /*
     * 创建应用目录
     */
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
    
    /*
     * 检查输入的抓包大小合法性
     */
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
				if(ErrorCode.OK !=moveRawFileToAppPath(R.raw.cap_tool, captoolDir))
				{
					return;
				}
				
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
					Toast.makeText(getApplicationContext(), "停止抓包失败,"+result.errorMsg, Toast.LENGTH_SHORT).show();
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
    
    /*
     * 抓包线程
     */
    public class CaptureThread implements Runnable{

		@Override
		public void run() {
			// TODO Auto-generated method stub
			
			//usage:CAP_TOOL <dev> <protocol> <cap_len> <save_path> <file_name>
			String[] commands = {
					"chmod 755 "+captoolDir+"/"+CAP_TOOL,
					captoolDir+"/"+CAP_TOOL + " " 
					+ spinInterface.getSelectedItem().toString() //interface
					+ " " + spinProtocol.getSelectedItem().toString() //protocol
					+ " " + etCapSize.getText() //capture size
					+ " " + sdcardDir + "/"+CAP_FILE_DIR //save dir
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
    
    /*
     * 统计线程
     */
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
			
			capture_stats = null;
		}
    	
    }
	
    /*
     * 更新UI线程
     */
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
	
	/* 
	 * 判断设备是否已经具有root权限
	 */
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
	
	/* 
	 * 检测抓包工具是否存在
	 */
	public boolean isCaptoolExist()
	{
		File captool = new File(captoolDir+"/"+CAP_TOOL);
		System.out.println(captool.getAbsolutePath());
		if(captool.exists()){
			return true;
		}
		return false;
	}
	
    /**
     * 删除单个文件
     * @param   sPath    被删除文件的文件名
     * @return 单个文件删除成功返回true，否则返回false
     */
    public boolean deleteFile(String sPath) {
        File file = new File(sPath);
        // 路径为文件且不为空则进行删除
        if (file.isFile() && file.exists()) {
            return file.delete();
        }
        
        return true;
    }
	
	/*
	 * 拷贝安装包中文件到指定的路径，文件已存在则尝试覆盖
	 */
	public ErrorCode moveRawFileToAppPath(int rawFileId, String dstPath)
	{
		/* 检测路径是否有效 */
		File dstDir = new File(dstPath);
		if(!dstDir.exists()){
			Toast.makeText(getApplicationContext(), "目录"+dstDir+"不存在", Toast.LENGTH_SHORT).show();
			Log.e(LOG_TAG, "目录"+dstDir+"不存在");
			return ErrorCode.ERR_FILE_NOT_EXIST;
		}
		
		/* 删除原有文件 */
		File file = new File(dstPath+"/"+CAP_TOOL);
		if(file.exists())
		{
			if(!file.delete())
			{
				Toast.makeText(getApplicationContext(), "删除"+dstDir+"原有文件失败，请重启手机后重试", Toast.LENGTH_SHORT).show();
				Log.e(LOG_TAG, "删除"+dstDir+"文件失败");
				return ErrorCode.ERR_DEL_FILE_FAILED;
			}
		}
		
		InputStream is = getResources().openRawResource(rawFileId);
		try {
			FileOutputStream fos = new FileOutputStream(dstPath+"/"+CAP_TOOL);
			
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
				Toast.makeText(getApplicationContext(), "内部异常!"+e.getMessage(), Toast.LENGTH_SHORT).show();
				e.printStackTrace();
				return ErrorCode.ERR_IO_EXCEPTION;
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			Toast.makeText(getApplicationContext(), "文件不存在!"+e.getMessage(), Toast.LENGTH_SHORT).show();
			e.printStackTrace();
			return ErrorCode.ERR_FILE_NOT_EXIST;
		}
		
		Log.v(LOG_TAG, "copy raw file "+rawFileId+" to "+dstPath+" success");
		return ErrorCode.OK;
	}
	
	/*
	 * 手动注册网络状态变化。
	 */
	private void registerNetStateReceiver() {
		broadcastReceiver = new ConnectionChangeReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(android.net.ConnectivityManager.CONNECTIVITY_ACTION);
		registerReceiver(broadcastReceiver, filter);
	}
	
	/*
	 * 处理网络状态变化的内部类
	 */
	public class ConnectionChangeReceiver extends BroadcastReceiver {


		@Override
		public void onReceive(Context context, Intent intent) {
			boolean isConnected = false;

			ConnectivityManager connectivityManager = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);

			NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

			if (networkInfo != null) {
				if(networkInfo.isConnected())
				{
					isConnected = true;
				}
				
				Log.i(LOG_TAG, "--Network Type  = " + networkInfo.getTypeName());
				Log.i(LOG_TAG, "--Network SubType  = " + networkInfo.getSubtypeName());
				Log.i(LOG_TAG, "--Network State = " + networkInfo.getState());
				
			} 
			
			String networkStatus = isConnected?"连接":"断开";
			Toast.makeText(getApplicationContext(), "网络连接已"+networkStatus+",重新加载网口信息...", Toast.LENGTH_SHORT).show();
			
			/* 网络连接发生变化后重新加载网卡信息 */
			loadInterface();
			
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
