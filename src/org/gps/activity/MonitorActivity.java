package org.gps.activity;

import org.gps.service.ConfigUtil;
import org.gps.service.Marker;
import org.gps.service.MarkerOverlay;
import org.gps.service.MonitorClient;

import com.baidu.mapapi.BMapManager;
import com.baidu.mapapi.GeoPoint;
import com.baidu.mapapi.ItemizedOverlay;
import com.baidu.mapapi.MKAddrInfo;
import com.baidu.mapapi.MKBusLineResult;
import com.baidu.mapapi.MKDrivingRouteResult;
import com.baidu.mapapi.MKPoiResult;
import com.baidu.mapapi.MKSearch;
import com.baidu.mapapi.MKSearchListener;
import com.baidu.mapapi.MKSuggestionResult;
import com.baidu.mapapi.MKTransitRouteResult;
import com.baidu.mapapi.MKWalkingRouteResult;
import com.baidu.mapapi.MapActivity;
import com.baidu.mapapi.MapController;
import com.baidu.mapapi.MapView;
import com.baidu.mapapi.OverlayItem;
import com.baidu.mapapi.ItemizedOverlay.OnFocusChangeListener;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class MonitorActivity extends MapActivity {
	private final static String TAG = "MonitorActivity";
	private MapView monitorMapView;
	private MapController mapController;
	private MarkerOverlay markerOverlay;
	private View popView;
	private TextView content;
	private Drawable drawable;
	private BMapManager mBMapMan;
	private MKSearch mkSearch;
	private Marker currentMarker;
	private Handler handler = new Handler();

	public Handler getHandler() {
		return handler;
	}

	public void setHandler(Handler handler) {
		this.handler = handler;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.monitor);
		
		mBMapMan = new BMapManager(getApplication());
		mBMapMan.init(ConfigUtil.API_KEY, null);
		//初始化地图解析器
		mkSearch = new MKSearch();
		mkSearch.init(mBMapMan, new MKSearchListener() {
			
			@Override
			public void onGetWalkingRouteResult(MKWalkingRouteResult arg0, int arg1) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onGetTransitRouteResult(MKTransitRouteResult arg0, int arg1) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onGetSuggestionResult(MKSuggestionResult arg0, int arg1) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onGetPoiResult(MKPoiResult arg0, int arg1, int arg2) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onGetDrivingRouteResult(MKDrivingRouteResult arg0, int arg1) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onGetBusDetailResult(MKBusLineResult arg0, int arg1) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onGetAddrResult(MKAddrInfo addInfo, int arg1) {
				currentMarker.setAddress(addInfo.strAddr);
				currentMarker.setHasAdress(true);
				content.setText(currentMarker.getSnippet());
				monitorMapView.updateViewLayout(popView, popView.getLayoutParams());
				
			}
		});
		super.initMapActivity(mBMapMan);
		
		// 初始化地图
		initMap();
		// 启动实时监控线程
		try {
			MonitorClient.getInstance(this).start();
		} catch (Exception e) {
			Log.e(TAG, "实时监控线程未关闭，不能再次启动！");
		}

		// 设置按钮监听器
		Button trackerButton = (Button) this.findViewById(R.id.trackerBt);
		Button historyButton = (Button) this.findViewById(R.id.historyBt);
		Button exitButton = (Button) this.findViewById(R.id.exitBt);

		trackerButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MonitorActivity.this,
						TrackerActivity.class);
				startActivity(intent);
			}
		});

		historyButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MonitorActivity.this,
						HistoryActivity.class);
				startActivity(intent);
			}
		});

		exitButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}

	private void initPopView() {
		popView = View.inflate(this, R.layout.popview, null);
		popView.setVisibility(View.GONE);
		monitorMapView.addView(popView, new MapView.LayoutParams(
				MapView.LayoutParams.FILL_PARENT,
				MapView.LayoutParams.WRAP_CONTENT, null,
				MapView.LayoutParams.BOTTOM_CENTER));
	}

	private void initMap() {

		monitorMapView = (MapView) findViewById(R.id.monitorMapView);
		monitorMapView.setBuiltInZoomControls(true); // 设置启用内置的缩放控件

		mapController = monitorMapView.getController(); // 得到mMapView的控制权,可以用它控制和驱动平移和缩放

		mapController.setZoom(ConfigUtil.INIT_ZOOM);
		mapController.animateTo(new GeoPoint(ConfigUtil.INIT_LAT,
				ConfigUtil.INIT_LNG));

		drawable = this.getResources().getDrawable(R.drawable.online);
		markerOverlay = new MarkerOverlay(this, drawable);
		monitorMapView.getOverlays().add(markerOverlay);
		markerOverlay.setOnFocusChangeListener(onFocusChangeListener);
		//初始化标记气泡
		initPopView();
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	private final OnFocusChangeListener onFocusChangeListener = new OnFocusChangeListener() {
		@SuppressWarnings("unchecked")
		@Override
		public void onFocusChanged(ItemizedOverlay itemizedOverlay,
				OverlayItem overlayItem) {
			MapView.LayoutParams popParams = (MapView.LayoutParams) popView
					.getLayoutParams();
			if (overlayItem == null) {
				popView.setVisibility(View.GONE);
				return;
			}
			GeoPoint geoPoint = overlayItem.getPoint();
			popParams.point = geoPoint;
			popParams.x = -5;
			popParams.y = -drawable.getBounds().height();
			TextView title = (TextView) popView.findViewById(R.id.pop_title);
			title.setText(overlayItem.getTitle());
			content = (TextView) popView
					.findViewById(R.id.pop_content);
			content.setText(overlayItem.getSnippet());
			popView.setVisibility(View.VISIBLE);
			monitorMapView.updateViewLayout(popView, popParams);
			if (((Marker) overlayItem).hasAdress()) {
				return;
			}
			currentMarker = (Marker) overlayItem;
			mkSearch.reverseGeocode(currentMarker.getPoint());
		}
	};

	public MapView getMapView() {
		return monitorMapView;
	}

	public void setMapView(MapView mapView) {
		this.monitorMapView = mapView;
	}

	public MapController getMapController() {
		return mapController;
	}

	public void setPopView(View popView) {
		this.popView = popView;
	}

	public View getPopView() {
		return popView;
	}

	public void setMapController(MapController mapController) {
		this.mapController = mapController;
	}

	public MapView getMonitorMapView() {
		return monitorMapView;
	}

	public void setMonitorMapView(MapView monitorMapView) {
		this.monitorMapView = monitorMapView;
	}

	public MarkerOverlay getMarkerOverlay() {
		return markerOverlay;
	}

	public void setMarkerOverlay(MarkerOverlay markerOverlay) {
		this.markerOverlay = markerOverlay;
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
		popView.setVisibility(View.GONE);
	}

	@Override
	protected void onDestroy() {
		if (mBMapMan != null) {
			mBMapMan.destroy();
			mBMapMan = null;
		}
		super.onDestroy();
		// 退出系统，会关闭进程，包括此进程下的所有线程(主线程,实时监控线程)
		System.exit(0);
	}
}