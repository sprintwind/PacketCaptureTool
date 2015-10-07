package com.sprintwind.packetcapturetool;

import android.support.v7.app.ActionBarActivity;
import android.app.ProgressDialog;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class SuggestionActivity extends ActionBarActivity {
	
	private static final String LOG_TAG = "sprintwind";
	
	private Button bttnSendSuggestion;
	private EditText edttxtSuggestion;
	
	private ProgressDialog pgrssdlgSending;
	
	private boolean sendMailReturns;
	private boolean sendMailSuccess;
	
	private Handler handler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_suggestion);
		this.setTitle(R.string.suggestion);
		
		edttxtSuggestion = (EditText) findViewById(R.id.edttxtSuggestion);
		bttnSendSuggestion = (Button) findViewById(R.id.bttnSendSuggestion);
		bttnSendSuggestion.setOnClickListener(new OnButtonSendSuggestionClickListener());
		
		pgrssdlgSending = new ProgressDialog(SuggestionActivity.this);
		pgrssdlgSending.setTitle("正在发送...");
		pgrssdlgSending.setIndeterminate(true);
		
		handler = new Handler();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.suggestion, menu);
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
	
	private class OnButtonSendSuggestionClickListener implements OnClickListener {

		@Override
		public void onClick(View arg0) {
			
			if(edttxtSuggestion.getText().toString().trim().equals("")) {
				Toast.makeText(getApplicationContext(), "意见不能为空哦", Toast.LENGTH_SHORT).show();
				return;
			}
			
			sendMailReturns = false;
			pgrssdlgSending.show();
			handler.post(new UpdateThread());
			
			new Thread(new SendMailThread()).start();
		}
		
	}
	

	private class SendMailThread implements Runnable {
	    

		@Override
	    public void run() {
	    	Log.i(LOG_TAG, "SendMailRunnable");
	    	try {
	    		sendMailReturns = false;
	    		sendMailSuccess = false;
	    		
	    		PackageManager manager = SuggestionActivity.this.getPackageManager();//.getPackageManager();
	            PackageInfo info = manager.getPackageInfo(SuggestionActivity.this.getPackageName(), 0);
	            String version = info.versionName;
	    		
	    		String emailContent = edttxtSuggestion.getText().toString();
	    		emailContent += "\n (version:"+version;
	    		emailContent += ", cellphone model:" + android.os.Build.MODEL;
	    		emailContent += ", Android version:" + android.os.Build.VERSION.RELEASE;
	    		emailContent += ", SDK version:"+ android.os.Build.VERSION.SDK_INT + ")";
	    		
				EMailUtil.SendEmail("smtp.qq.com", "sprintwind@qq.com", "sprintwind@qq.com", "lovemeixin0913", "624988625@qq.com", "25", "抓包精灵意见反馈", emailContent);
				sendMailReturns = true;
				sendMailSuccess = true;
				handler.post(new UpdateThread());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				sendMailReturns = true;
				sendMailSuccess = false;
			}
	        
	    }
	}
	
	private class UpdateThread extends Thread {
		public void run(){
			
				if(sendMailReturns) {
					if(sendMailSuccess) {
						Toast.makeText(getApplicationContext(), "您的意见已反馈成功,感谢您对抓包精灵的支持", Toast.LENGTH_SHORT).show();
					}
					else {
						Toast.makeText(getApplicationContext(), "发送失败,请稍后再试", Toast.LENGTH_SHORT).show();
					}
					pgrssdlgSending.dismiss();
				}
				
		}
	}
	
}
