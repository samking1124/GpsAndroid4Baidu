package org.gps.activity;

import java.util.ArrayList;
import java.util.Calendar;

import org.gps.db.GpsDBOpenHelper;
import org.gps.service.ConfigUtil;
import org.gps.service.History;
import org.gps.service.HistoryLineOverlay;
import org.gps.service.HistoryMarkerOverlay;
import org.gps.service.HttpClientUtil;
import org.gps.service.Tracker;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.mapapi.BMapManager;
import com.baidu.mapapi.GeoPoint;
import com.baidu.mapapi.MapActivity;
import com.baidu.mapapi.MapController;
import com.baidu.mapapi.MapView;
import com.solok.datetime.DateDialog;
import com.solok.datetime.OnDateSetListener;

/**
 * @author Administrator
 * 
 */
public class HistoryActivity extends MapActivity {
	private final static int DATE_DIALOG_ID = 1;
	private static final int SET_DIALOG_ID = 2;
	private MapView historyMapView;
	private HistoryMarkerOverlay historyMarkerOverlay;
	private HistoryLineOverlay lineOverlay;
	private MapController mapController;
	private BMapManager mBMapMan;
	private ArrayAdapter<Tracker> trackerAdpter;
	private ArrayAdapter<Speed> speedAdpter;
	private int trackerSelectPosition;
	private int speedSelectPosition;
	private String trackerNo;
	private String date;
	private int delayTime;
	private TextView infoTracker;
	private TextView infoDate;
	private TextView dateView;
	private ProgressBar progressBar;
	private Handler handler = new Handler();
	private boolean stopPlay = false;
	private boolean isPlaying = false;
	private boolean isPause = false;

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.history);
		//初始百度地图管理器
		mBMapMan = new BMapManager(getApplication());
		mBMapMan.init(ConfigUtil.API_KEY, null);
		super.initMapActivity(mBMapMan);
		
		initMap();
		progressBar = (ProgressBar) this.findViewById(R.id.progressBar);
		infoTracker = (TextView) this.findViewById(R.id.infoTracker);
		infoDate = (TextView) this.findViewById(R.id.infoDate);
		// 设置初始跟踪器
		ArrayList<Tracker> trackers = getTrackers();
		trackerAdpter = new ArrayAdapter<Tracker>(this,
				android.R.layout.simple_spinner_item, android.R.id.text1,
				trackers);
		trackerAdpter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		trackerNo = trackers.get(0).getTrackerNo();
		infoTracker.setText(trackers.get(0).getName());

		// 设置初始日期
		date = (String) DateFormat.format("yyyy-M-d", Calendar.getInstance());
		infoDate.setText(date);

		// 设置初始速度
		ArrayList<Speed> speeds = getInitSpeeds();
		delayTime = speeds.get(0).delayTime;
		speedAdpter = new ArrayAdapter<Speed>(this,
				android.R.layout.simple_spinner_item, android.R.id.text1,
				speeds);
		speedAdpter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		// 设置跟踪器设置按钮
		ImageButton setButton = (ImageButton) this.findViewById(R.id.setBt);
		setButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showDialog(SET_DIALOG_ID);
			}
		});

		// 设置播放按钮
		ImageButton playButton = (ImageButton) this.findViewById(R.id.playBt);
		playButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// 如果是停止状态，取消停止
				if (stopPlay) {
					stopPlay = false;
				}
				// 如果是暂停状态，取消暂停
				if (isPause) {
					isPause = false;
					return;
				}
				// 如果正在播放，直接返回
				if (isPlaying) {
					return;
				}
				// 清空地图上已经播放的标记
				clearMap();
				
				// 开启播放线程
				new PlayThread().start();
			}
		});

		// 设置暂停按钮
		ImageButton pauseButton = (ImageButton) this.findViewById(R.id.pauseBt);
		pauseButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				isPause = true;
			}
		});

		// 设置停止按钮
		ImageButton stopButton = (ImageButton) this.findViewById(R.id.stopBt);
		stopButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				stopPlay = true;
			}
		});

		// 设置返回按钮
		ImageButton backButton = (ImageButton) this.findViewById(R.id.backBt);
		backButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(HistoryActivity.this,
						MonitorActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				startActivity(intent);
				finish();
			}
		});

	}

	private void initMap() {
		historyMapView = (MapView) findViewById(R.id.historyMapView);
		historyMapView.setBuiltInZoomControls(true);
		historyMapView.setSatellite(false);
		mapController = historyMapView.getController();
		mapController.setZoom(ConfigUtil.INIT_ZOOM);
		mapController.animateTo(new GeoPoint(ConfigUtil.INIT_LAT,
				ConfigUtil.INIT_LNG));
	}

	private void clearMap() {
		// 清空地图上已经播放的标记
		historyMapView.getOverlays().clear();
	}

	private class Speed {
		private int delayTime;
		private String display;

		public Speed(int delayTime, String display) {
			this.delayTime = delayTime;
			this.display = display;
		}

		@Override
		public String toString() {
			return display;
		}

	}


	private class PlayThread extends Thread {

		@Override
		public void run() {
			isPlaying = true;
			ArrayList<History> histories = HttpClientUtil.getHistory(trackerNo,
					date);
			if (histories == null || histories.size() == 0) {
				handler.post(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(HistoryActivity.this, "未查询到轨迹记录", 3000).show();
					}
				});
				isPlaying = false;
				return;
			}
			final ArrayList<GeoPoint> geoPoints = new ArrayList<GeoPoint>();
			for (History history : histories) {
				geoPoints.add(history.getCurrentGeoPoint());
			}
			
			handler.post(new Runnable() {
				@Override
				public void run() {
					mapController.setZoom(ConfigUtil.HISTORY_PLAY_ZOOM);
					// 添加轨迹连线图层
					lineOverlay = new HistoryLineOverlay(geoPoints);
					historyMapView.getOverlays().add(lineOverlay);
					// 添加轨迹点图层
					historyMarkerOverlay = new HistoryMarkerOverlay(
							HistoryActivity.this);
					historyMapView.getOverlays().add(historyMarkerOverlay);
				}
			});
			
			
			// 位置标记移动播放
			for (int i = 0; i < geoPoints.size(); i++) {
				if (stopPlay) {
					isPlaying = false;
					return;
				}
				try {
					while (isPause) {
						if (stopPlay) {
							isPause = false;
							isPlaying = false;
							return;
						}
						Thread.sleep(1000);
					}
					Thread.sleep(delayTime);
					final int index = i;
					handler.post(new Runnable() {
						@Override
						public void run() {
							mapController.animateTo(geoPoints.get(index));
							historyMarkerOverlay.setOverlayItem(geoPoints.get(index));
							progressBar.setProgress((index + 1) * 100 / geoPoints.size());
						}
					});
					
				} catch (InterruptedException e) {
					isPlaying = false;
					return;
				}
			}
			isPlaying = false;
		
		}

	}
	
	@Override
	protected void onResume() {
		if (mBMapMan != null) {
			mBMapMan.start();
		}
		super.onResume();
	}

	@Override
	protected void onPause() {
		if (mBMapMan != null) {
			mBMapMan.stop();
		}
		super.onPause();
		// 改变暂停标记以防止线程卡在暂停状态
		isPause = false;
		// 改变播放线程标记，以结束播放线程
		stopPlay = true;
	}


	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = null;
		switch (id) {
		case DATE_DIALOG_ID:
			return new DateDialog(this, new OnDateSetListener() {

				@Override
				public void onDateSet(int year, String month, String date) {
					dateView.setText(year + "-" + month + "-" + date);

				}
			});

		case SET_DIALOG_ID:
			AlertDialog.Builder builder = new Builder(this);
			builder.setTitle("查询设置");

			LayoutInflater inflater = getLayoutInflater();
			View layout = inflater.inflate(R.layout.history_set,
					(ViewGroup) findViewById(R.id.history_set));
			builder.setView(layout);

			// 设置跟踪器下拉菜单
			final Spinner trackerSp = (Spinner) layout
					.findViewById(R.id.trackerSp);
			trackerSp.setAdapter(trackerAdpter);
			trackerSp.setSelection(trackerSelectPosition);

			// 设置日期控件
			dateView = (TextView) layout.findViewById(R.id.dateTv);
			dateView.setText(date);
			dateView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					showDialog(DATE_DIALOG_ID);
				}
			});

			// 设置速度下拉单
			final Spinner speedSp = (Spinner) layout.findViewById(R.id.speedSp);
			speedSp.setAdapter(speedAdpter);
			speedSp.setSelection(speedSelectPosition);

			// 设置确定和取消按钮
			builder.setPositiveButton("确定",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							trackerSelectPosition = trackerSp
									.getSelectedItemPosition();
							speedSelectPosition = speedSp
									.getSelectedItemPosition();
							trackerNo = ((Tracker) trackerSp.getSelectedItem())
									.getTrackerNo();
							infoTracker.setText(((Tracker) trackerSp
									.getSelectedItem()).getName());
							date = (String) dateView.getText();
							infoDate.setText(dateView.getText());
							delayTime = ((Speed) speedSp.getSelectedItem()).delayTime;

						}
					});
			builder.setNegativeButton("取消",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							removeDialog(SET_DIALOG_ID);
						}
					});

			dialog = builder.create();
			dialog.setCancelable(true);
			return dialog;

		default:
			return dialog;
		}

	}

	private ArrayList<Tracker> getTrackers() {
		ArrayList<Tracker> trackers = new ArrayList<Tracker>();
		for (Tracker tracker : GpsDBOpenHelper.getInstance(this)
				.getTrackerData().values()) {
			trackers.add(tracker);
		}
		return trackers;
	}

	private ArrayList<Speed> getInitSpeeds() {
		ArrayList<Speed> speeds = new ArrayList<Speed>();
		speeds.add(new Speed(1000, "中速"));
		speeds.add(new Speed(200, "很快"));
		speeds.add(new Speed(500, "快速"));
		speeds.add(new Speed(1500, "慢速"));
		speeds.add(new Speed(2000, "很慢"));
		return speeds;
	}

}
