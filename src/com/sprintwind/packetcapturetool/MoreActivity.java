package com.sprintwind.packetcapturetool;

import java.util.ArrayList;
import java.util.HashMap;

import com.baidu.autoupdatesdk.BDAutoUpdateSDK;
import com.baidu.autoupdatesdk.UICheckUpdateCallback;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

@TargetApi(Build.VERSION_CODES.HONEYCOMB) public class MoreActivity extends Fragment {
	
	private final String ITEM_IMAGE_RIGHT = "ItemImageRight";
	private final String ITEM_TEXT  = "ItemText";
	
	
	private enum MoreListItemCol{
		COL_SHARE,
		COL_SUGGETSION,
		COL_ENCOURAGE,
		COL_CURRENT_VERSION
	};
	
	private ListView lstvwMore;
	private ArrayList<HashMap<String, Object>> lstvwItems;
	
	private ProgressDialog dialog;
	
	private View view;
	
	@Override  
    public View onCreateView(LayoutInflater inflater, ViewGroup container,  
            Bundle savedInstanceState) {
		
		view = inflater.inflate(R.layout.activity_more, container, false); 
		initMoreView(view);
		
		return view;
	}
	
	private void initMoreView(View view) {
		dialog = new ProgressDialog(MoreActivity.this.getActivity());
		dialog.setTitle("正在检查更新");
		dialog.setIndeterminate(true);
		
		lstvwMore = (ListView) view.findViewById(R.id.lstvwMore);
		lstvwItems = new ArrayList<HashMap<String, Object>>();
		
		/* 分享给好友 */
		HashMap<String, Object> hmShare = new HashMap<String, Object>();
		hmShare.put(ITEM_TEXT, getString(R.string.share_with_friends));
		hmShare.put(ITEM_IMAGE_RIGHT, R.drawable.goto_icon_selecter);
		lstvwItems.add(hmShare);
		
		/* 意见反馈 */
		HashMap<String, Object> hmSuggestion = new HashMap<String, Object>();
		hmSuggestion.put(ITEM_TEXT, getString(R.string.suggestion));
		lstvwItems.add(hmSuggestion);
		
		/* 给我鼓励 */
		HashMap<String, Object> hmEncourage = new HashMap<String, Object>();
		hmEncourage.put(ITEM_TEXT, getString(R.string.encourage));
		lstvwItems.add(hmEncourage);
		
		/* 当前版本 */
		HashMap<String, Object> hmCurrentVersion = new HashMap<String, Object>();
		hmCurrentVersion.put(ITEM_TEXT, getString(R.string.current_version)+": "+getVersion());
		lstvwItems.add(hmCurrentVersion);
		
		updateListView();
		lstvwMore.setOnItemClickListener(new OnListViewItemClickListener());
	}
	
	/* 
     * 更新设置列表
     */
    public void updateListView()
    {
    	String[] strFrom = new String[] {ITEM_TEXT};
		int[] iTo = new int[] {R.id.lstvwMoreItemText};
		SimpleAdapter sadpSettings = new SimpleAdapter(MoreActivity.this.getActivity(), lstvwItems, R.layout.more_listview_item, strFrom, iTo);
		lstvwMore.setAdapter(sadpSettings);
    }
    
    public class OnListViewItemClickListener implements OnItemClickListener{

		@Override
		public void onItemClick(AdapterView<?> adapter, View view, int col,
				long arg3) {
			// TODO Auto-generated method stub
			
			/* 分享 */
			if(col == MoreListItemCol.COL_SHARE.ordinal()) {
				Intent intent=new Intent(Intent.ACTION_SEND);   
	            intent.setType("text/*");   
	            intent.putExtra(Intent.EXTRA_SUBJECT, "分享");   
	            intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_string));    
	            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);   
	            startActivity(Intent.createChooser(intent, "分享"+MoreActivity.this.getActivity().getTitle()));
	            return;
			}
			/* 意见 */
			if(col == MoreListItemCol.COL_SUGGETSION.ordinal()) {
				Intent intent = new Intent(MoreActivity.this.getActivity().getApplicationContext(), SuggestionActivity.class);
				startActivity(intent);
				return;
			}
			/* 打分 */
			if(col == MoreListItemCol.COL_ENCOURAGE.ordinal()) {
				Uri uri = Uri.parse("market://details?id="+MoreActivity.this.getActivity().getPackageName());  
				Intent intent = new Intent(Intent.ACTION_VIEW,uri);  
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);  
				startActivity(intent);
				return;
			}
			
			/* 版本更新 */
			if(col == MoreListItemCol.COL_CURRENT_VERSION.ordinal()) {
				dialog.show();
				BDAutoUpdateSDK.uiUpdateAction(MoreActivity.this.getActivity(), new MyUICheckUpdateCallback());
				return;
			}
			
		}
    	
    }
    
    private class MyUICheckUpdateCallback implements UICheckUpdateCallback {

		@Override
		public void onCheckComplete() {
			dialog.dismiss();
			Toast.makeText(getActivity(), "已是最新版本", Toast.LENGTH_SHORT).show();
		}

	}
    
    @Override
	public void onDestroy() {
		dialog.dismiss();
		super.onDestroy();
	}
    
    /**
     * 获取版本号
     * @return 当前应用的版本号
     */
    public String getVersion() {
        try {
            PackageManager manager = MoreActivity.this.getActivity().getPackageManager();
            PackageInfo info = manager.getPackageInfo(MoreActivity.this.getActivity().getPackageName(), 0);
            String version = info.versionName;
            return version;
        } catch (Exception e) {
            e.printStackTrace();
            return this.getString(R.string.get_version_failed);
        }
    }
	
}
