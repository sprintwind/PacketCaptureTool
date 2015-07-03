package com.sprintwind.packetcapturetool;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

@TargetApi(Build.VERSION_CODES.HONEYCOMB) public class AnalyzeActivity extends Fragment {
	
	private final String PCAP_FILE_SUFFIX = ".pcap";
	private final int PCAP_MAGIC_SIZE = 4;
	private final String LOG_TAG = "sprintwind";
	private final String CAP_FILE_DIR = "packet_capture";
	
	private EditText edttxtFile;
	private Button	btnBrowse;
	private Button bttnAnalyze;
	private ProgressDialog pgsdlgAnalyzing;
	private View view;
	
	private String sdCardPath;
	
	
	@Override  
    public View onCreateView(LayoutInflater inflater, ViewGroup container,  
            Bundle savedInstanceState) {
		
		view = inflater.inflate(R.layout.activity_analyze, container, false); 
		initAnalyzeView(view);
		
		return view;
	}
	
	/** 调用文件选择软件来选择文件 **/  
	private void showFileChooser() {  
	    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);  
	    File captureFilePath = new File(sdCardPath+"/"+CAP_FILE_DIR);
	    Uri uri = Uri.fromFile(captureFilePath);
	    intent.setDataAndType(uri, "*/*");
	    Log.i(LOG_TAG, uri.toString());
	    //intent.setType("*/*");
	    //intent.setAction(android.content.Intent.ACTION_VIEW);
	    intent.addCategory(Intent.CATEGORY_OPENABLE);  
	    try {  
	        startActivityForResult(Intent.createChooser(intent, "请选择一个要解析的文件"),  
	                1);  
	    } catch (android.content.ActivityNotFoundException ex) {  
	        // Potentially direct the user to the Market with a Dialog  
	        Toast.makeText(AnalyzeActivity.this.getActivity().getApplicationContext(), "没有找到文件管理器,请先安装文件管理器", Toast.LENGTH_SHORT)  
	                .show();  
	    }  
	}
	
	public class OnAnalyzeButtonClickListener implements OnClickListener {

		@Override
		public void onClick(View arg0) {
			// TODO Auto-generated method stub
			if(edttxtFile.getText().toString().length() == 0){
				Toast.makeText(AnalyzeActivity.this.getActivity().getApplicationContext(), "请先选择一个抓包文件", Toast.LENGTH_SHORT).show();
				return;
			}
			
			if(!edttxtFile.getText().toString().endsWith(PCAP_FILE_SUFFIX)){
				Toast.makeText(AnalyzeActivity.this.getActivity().getApplicationContext(), "不是pcap格式的文件", Toast.LENGTH_SHORT).show();
				return;
			}
			
			pgsdlgAnalyzing.show();
			
			File flCapture = new File(edttxtFile.getText().toString());
			InputStream is = null;
			
			try{
				is = new FileInputStream(flCapture);
				
				byte[] bytBuffer = new byte[PCAP_MAGIC_SIZE];
				is.read(bytBuffer);
				
				/* 判断魔术字是否正确:d4c3b2a1 */
				if(bytBuffer[0] != -44 || bytBuffer[1] != -61 || bytBuffer[2] != -78 || bytBuffer[3] != -95 ){
					Log.i(LOG_TAG, "bytBuffer[0]:"+bytBuffer[0]+", bytBuffer[1]"+bytBuffer[1]+", bytBuffer[2]"+bytBuffer[2]+", bytBuffer[3]"+bytBuffer[3]);
					Toast.makeText(AnalyzeActivity.this.getActivity().getApplicationContext(), "抓包文件已损坏", Toast.LENGTH_SHORT).show();
					is.close();
					pgsdlgAnalyzing.dismiss();
					return;
				}
				is.close();
			}
			catch(Exception e){
				
				Toast.makeText(AnalyzeActivity.this.getActivity().getApplicationContext(), "打开文件失败,可能不是pcap格式的文件或文件已损坏", Toast.LENGTH_SHORT).show();
				pgsdlgAnalyzing.dismiss();
				return;
			}
			
			Intent intent = new Intent(AnalyzeActivity.this.getActivity().getApplicationContext(), PacketBriefActivity.class);
			intent.putExtra("cap_file", edttxtFile.getText().toString());
			startActivityForResult(intent, PCAP_MAGIC_SIZE);
		}
		
	}
    
    public class OnBrowseBottonClickListener implements OnClickListener {

		@Override
		public void onClick(View arg0) {
			// TODO Auto-generated method stub
			showFileChooser();
		}
    	
    }
    
    @Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if (resultCode == Activity.RESULT_OK) {
    		Uri uri = data.getData();
    		String url = uri.getPath();
    		Log.i(LOG_TAG, "url:"+url+", edttxtFile:"+edttxtFile);
    		
    		/* 遇到内存不足导致MainActivity重新加载时,需要重新载入AnalyzeActivity */
    		if(null == edttxtFile){
				initAnalyzeView(view);
    		}
    		edttxtFile.setText(url);
    		pgsdlgAnalyzing.dismiss();
    	}
    	
    	if(Activity.RESULT_CANCELED == resultCode){
    		pgsdlgAnalyzing.dismiss();
    	}
    	
    	Log.i(LOG_TAG, "Activity result:"+resultCode);
    }
	
	private void initAnalyzeView(View view)
    {
    	edttxtFile = (EditText) view.findViewById(R.id.edttxtFile);
    	btnBrowse = (Button) view.findViewById(R.id.bttnBrowse);
    	bttnAnalyze = (Button) view.findViewById(R.id.bttnAnalyze);
    	
    	btnBrowse.setOnClickListener(new OnBrowseBottonClickListener());
    	bttnAnalyze.setOnClickListener(new OnAnalyzeButtonClickListener());
    	
    	pgsdlgAnalyzing = new ProgressDialog(getActivity());
		pgsdlgAnalyzing.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		pgsdlgAnalyzing.setTitle(R.string.analyzing);
		pgsdlgAnalyzing.setMessage(getString(R.string.waitting_for_analyzing));
		pgsdlgAnalyzing.setIndeterminate(false);
		
		sdCardPath = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
    }
}
