package org.gps.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import org.gps.activity.MonitorActivity;
import org.gps.db.GpsDBOpenHelper;

import android.util.Log;

import com.baidu.mapapi.GeoPoint;

/**
 * @author HuangXin 实时监控线程
 * 
 */
public class MonitorClient extends Thread {
	private final static String TAG = "MonitorClient";
	private final static String IP = ConfigUtil.getConfig().getProperty(
			"monitor_server_ip");
	private final static int PORT = Integer.parseInt(ConfigUtil.getConfig()
			.getProperty("monitor_server_port"));
	private Socket socket;
	private InputStream in;
	private OutputStream out;
	private MonitorActivity monitorActivity;
	private GpsDBOpenHelper dbHelper;
	private Timer timer;
	private static MonitorClient client;

	private MonitorClient(MonitorActivity monitorActivity) {
		this.setMonitorActivity(monitorActivity);
		this.setDbHelper(GpsDBOpenHelper.getInstance(monitorActivity));
	}

	public static MonitorClient getInstance(MonitorActivity monitorActivity) {
		if (client == null) {
			client = new MonitorClient(monitorActivity);
		}
		return client;
	}

	public static MonitorClient getInstance() {
		return client;
	}

	@Override
	public void run() {
		try {
			socket = new Socket(IP, PORT);
			Log.d(TAG, "实时监控socket连接已建立");
			in = socket.getInputStream();
			out = socket.getOutputStream();
			linkCheck();
			login();
			// 从数据库获取所有处于监控状态的跟踪器
			HashMap<String, Tracker> trackers = dbHelper.getTrackerData();
			StringBuilder sb = new StringBuilder();
			if (trackers != null && trackers.size() != 0) {
				for (Tracker tracker : trackers.values()) {
					if (tracker.getState()) {
						sb.append(tracker.getTrackerNo() + ",");
					}
				}
				if (sb.length() > 0) {
					// 发送跟踪器号码到服务器端以通知服务器发送这些跟踪器的实时信息(位置,上线,下线,报警)
					updateTracker(sb.substring(0, sb.length() - 1), null);
				}

			}
			// 读取服务器发送的数据
			readCmd(in);
		} catch (Exception e) {
			// 关闭链路检测
			timer.cancel();
			Log.e(TAG, "run方法捕获异常");
			e.printStackTrace();
			// 三分钟后自动重连
			Log.d(TAG, "三分钟后自动重连");
			try {
				in.close();
				out.close();
				socket.close();
				Thread.sleep(180000);
				run();
			} catch (Exception e1) {
				e1.printStackTrace();
				run();
			}

		}
	}

	private void sendMsg(String msg) {
		try {
			out.write(msg.getBytes());
			Log.d(TAG, "=>" + msg);
		} catch (Exception e) {
			// 关闭链路检测
			timer.cancel();
			Log.e(TAG, "sendMsg方法捕获异常");
			e.printStackTrace();
		}
	}

	private void readCmd(InputStream in) throws IOException {
		String cmd = "";
		char c;
		while (true) {
			c = (char) in.read();
			cmd += c;
			if (c == '}') {
				handleMsg(cmd.trim());
				cmd = "";
			}
		}
	}

	private void handleMsg(String msg) {
		Log.d(TAG, "<=" + msg);
		try {
			// {POS,353327020184032,22.537206,113.938972,4,0.0,0,2010-07-09
			// 08:21:40,22.537206,113.938972,1,68,5,0,0}
			// {POS,跟踪器编号,纬度,经度,方向,速度,高度,记录时间,原始纬度,原始经度,是否漫游,电量,信号强度,是否充电中,是否基站定位,卫星数}
			final String[] cmd = msg.split(",");
			if ("{POS".equals(cmd[0])) {
				final GeoPoint point = new History(cmd[2], cmd[3], cmd[8], cmd[9]).getCurrentGeoPoint();
				monitorActivity.getHandler().post(new Runnable() {

					@Override
					public void run() {
						monitorActivity.getMarkerOverlay().addMarker(
								new Marker(point, cmd[1], cmd[7], Integer
										.parseInt(cmd[14])));
						monitorActivity.getMapController().animateTo(point);
					}
				});

			}
		} catch (Exception e) {
			Log.e(TAG, "handleMsg方法捕获异常");
			e.printStackTrace();
		}
	}

	// 发送链路检测
	private void linkCheck() {
		timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				sendMsg("{LINKCHECK}");
			}
			// TODO 调整时间
		}, 10000, 30000);
	}

	public void login() {
		sendMsg("{LOGIN,555,sdfgsdfgd}");
	}

	public void addTracker(String tkNos) {
		sendMsg("{ADDTRACKER," + tkNos + "}");
	}

	public void delTracker(String tkNos) {
		sendMsg("{DELTRACKER," + tkNos + "}");
	}

	// 更新跟踪器监控状态
	public void updateTracker(String addTrackerNos, String delTrackerNos) {
		// 先清空地图
		monitorActivity.getMarkerOverlay().clear();
		if (addTrackerNos != null && !addTrackerNos.trim().equals("")) {
			// 获取需要监控的跟踪器最新位置
			final ArrayList<Marker> latestMarkers = HttpClientUtil
					.getLatestPos(addTrackerNos);
			if (latestMarkers != null && latestMarkers.size() > 0) {

				monitorActivity.getHandler().post(new Runnable() {

					@Override
					public void run() {
						monitorActivity.getMapController().setZoom(
								ConfigUtil.MONITOR_ZOOM);
						monitorActivity.getMapController().animateTo(
								latestMarkers.get(0).getPoint());
						// 添加所有被监控跟踪器的最新位置
						monitorActivity.getMarkerOverlay().addMarkers(
								latestMarkers);
					}
				});

			}
			// 发送添加监控跟踪器命令到服务器
			addTracker(addTrackerNos);
		}
		if (delTrackerNos != null && !delTrackerNos.trim().equals("")) {
			// 发送取消监控跟踪器命令到服务器
			delTracker(delTrackerNos);
		}
	}

	public MonitorActivity getMonitorActivity() {
		return monitorActivity;
	}

	public void setMonitorActivity(MonitorActivity monitorActivity) {
		this.monitorActivity = monitorActivity;
	}

	public void setDbHelper(GpsDBOpenHelper dbHelper) {
		this.dbHelper = dbHelper;
	}

	public GpsDBOpenHelper getDbHelper() {
		return dbHelper;
	}

	public Socket getSocket() {
		return socket;
	}

	public void setSocket(Socket socket) {
		this.socket = socket;
	}

}
