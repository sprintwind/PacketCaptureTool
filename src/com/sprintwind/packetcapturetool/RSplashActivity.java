package com.sprintwind.packetcapturetool;

import com.baidu.mobads.SplashAd;
import com.baidu.mobads.SplashAdListener;
import com.baidu.mobads.SplashAd.SplashType;

import android.support.v7.app.ActionBarActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.RelativeLayout;



public class RSplashActivity extends ActionBarActivity {
	
private final String LOG_TAG = "sprintwind";
	
public boolean waitingOnRestart=false;
	
	private void jumpWhenCanClick() {
		Log.d("test", "this.hasWindowFocus():"+this.hasWindowFocus());
		if(this.hasWindowFocus()||waitingOnRestart){
			this.startActivity(new Intent(RSplashActivity.this, MainActivity.class));
			this.finish();
		}else{
			waitingOnRestart=true;
		}
		
	}
	
	/**
	 * 不可点击的开屏，使用该jump方法，而不是用jumpWhenCanClick
	 */
	private void jump() {
		this.startActivity(new Intent(RSplashActivity.this, MainActivity.class));
		this.finish();
	}
	
	@Override
	protected void onRestart() {
		super.onRestart();
		if(waitingOnRestart){
			jumpWhenCanClick();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_rsplash);
		
		RelativeLayout adsParent = (RelativeLayout)this.findViewById(R.id.adsRl);
        SplashAdListener listener = new SplashAdListener(){

			@Override
			public void onAdClick() {
				// TODO Auto-generated method stub
				Log.i(LOG_TAG, "onAdClick");
			}

			@Override
			public void onAdDismissed() {
				// TODO Auto-generated method stub
				Log.i(LOG_TAG, "onAdDismissed");
				jumpWhenCanClick();
			}

			@Override
			public void onAdFailed(String arg0) {
				// TODO Auto-generated method stub
				Log.i(LOG_TAG, "onAdFailed");
				jump();
			}

			@Override
			public void onAdPresent() {
				// TODO Auto-generated method stub
				Log.i(LOG_TAG, "onAdPresent");
			}
        	
        };
        
        new SplashAd(this, adsParent, listener, "", true, SplashType.REAL_TIME);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.rsplash, menu);
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
}
