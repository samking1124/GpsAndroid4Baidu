package org.gps.activity;

import java.util.HashMap;

import org.gps.db.GpsDBOpenHelper;
import org.gps.service.TrackerAdapter;
import org.gps.service.TrackerAdapter.ViewHolder;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class TrackerActivity extends Activity {
	private GpsDBOpenHelper dbHelper;
	private Handler handler = new Handler();
	private TrackerAdapter adapter;
	private ListView listView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		dbHelper = GpsDBOpenHelper.getInstance(this);
		this.setContentView(R.layout.tracker);
		adapter = new TrackerAdapter(this, dbHelper);
		listView = (ListView) this.findViewById(R.id.listView);
		listView.setItemsCanFocus(false);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				try {
					ViewHolder holder = (ViewHolder) (view.getTag());
					holder.stateView.toggle();
					HashMap<String, Object> item = (HashMap<String, Object>) parent
							.getItemAtPosition(position);
					boolean isChecked = holder.stateView.isChecked();
					dbHelper.getTrackerData().get(item.get("trackerNo")).setState(
							isChecked);
					adapter.getData().get(position).put("state",isChecked);
				} catch (Exception e) {
				}
			}
		});
		// 设置同步按钮
		Button synchroButton = (Button) this.findViewById(R.id.synchroBt);
		synchroButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// 调用HttpClient需要新开一个线程，否则报错。
				new Thread() {
					public void run() {
						if (GpsDBOpenHelper.getInstance(TrackerActivity.this)
								.synTracker()) {
							handler.post(new Runnable() {
								@Override
								public void run() {
									Toast.makeText(TrackerActivity.this,
											"同步成功", 30000).show();
									adapter.setData(dbHelper
											.getAdapterData());
									adapter.notifyDataSetChanged();
								}
							});

						} else {
							handler.post(new Runnable() {
								@Override
								public void run() {
									Toast.makeText(TrackerActivity.this,
											"同步失败", 30000).show();
								}
							});
						}
					}
				}.start();
			}
		});
		
		
		// 设置全选按钮
		Button allselectButton = (Button) this.findViewById(R.id.allselectBt);
		allselectButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				adapter.allselect();
				adapter.notifyDataSetChanged();
			}
		});
		
		// 设置确定按钮
		Button defineButton = (Button) this.findViewById(R.id.defineBt);
		defineButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dbHelper.updateState();
				back();
			}
		});
		
		

		// 设置取消按钮
		Button cancelButton = (Button) this.findViewById(R.id.cancelBt);
		cancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dbHelper.clearData();
				back();
			}
		});

	}
	
	
	private void back(){
		Intent intent = new Intent(TrackerActivity.this,
				MonitorActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		startActivity(intent);
		finish();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// 更新跟踪器监控状态
//		dbHelper.updateState();
	}

	public Handler getHandler() {
		return handler;
	}

	public void setHandler(Handler handler) {
		this.handler = handler;
	}
}
