package org.gps.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.gps.service.HttpClientUtil;
import org.gps.service.LoginBean;
import org.gps.service.MonitorClient;
import org.gps.service.Tracker;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class GpsDBOpenHelper extends SQLiteOpenHelper {
	private final static String DATABASE = "gps.db";
	private final static int VERSION = 5;
	private static GpsDBOpenHelper helper;
	private SQLiteDatabase db;
	private HashMap<String, Tracker> trackerData;
	private ArrayList<HashMap<String, Object>> adapterData;

	private GpsDBOpenHelper(Context context) {
		super(context, DATABASE, null, VERSION);
		db = this.getWritableDatabase();
	}

	public static GpsDBOpenHelper getInstance(Context context) {
		if (helper == null) {
			helper = new GpsDBOpenHelper(context);
		}
		return helper;
	}

	public static GpsDBOpenHelper getInstance() {
		return helper;
	}

	/**
	 * @param userName
	 *            和服务器同步数据
	 */
	public boolean synTracker() {
		
		ArrayList<Tracker> trackers = HttpClientUtil.getTrackers();
		if (trackers == null) {
			return false;
		}
		db.beginTransaction();
		try {
			deleteByUserName();
			for (Tracker tracker : trackers) {
				db
						.execSQL(
								"INSERT INTO tracker (name,trackerno,username,state) values(?,?,?,?)",
								new Object[] { tracker.getName(),
										tracker.getTrackerNo(), getUserName(),
										0 });
			}
			db.setTransactionSuccessful();
			StringBuilder delTrackerNos = new StringBuilder();
			//清空地图标记
			for(String tkNo:trackerData.keySet()){
				delTrackerNos.append(tkNo + ",");
			}
			if(delTrackerNos.length()>0){
				//取消所有跟踪器监控状态
				MonitorClient.getInstance().updateTracker(null, delTrackerNos.substring(0,delTrackerNos.length()-1));
			}
			clearData();
			return true;
		} catch (Exception e) {
			return false;
		} finally {
			db.endTransaction();
		}
	}

	public ArrayList<Tracker> findByUserName() {
		ArrayList<Tracker> trackers = new ArrayList<Tracker>();
		Cursor cursor = db.rawQuery("select * from tracker where username=?",
				new String[] { getUserName() });
		while (cursor.moveToNext()) {
			Tracker tracker = new Tracker();
			tracker.setName(cursor.getString(cursor.getColumnIndex("name")));
			tracker.setTrackerNo(cursor.getString(cursor
					.getColumnIndex("trackerno")));
			tracker
					.setState(cursor.getInt(cursor.getColumnIndex("state")) == 1);
			tracker.setUserName(getUserName());
			trackers.add(tracker);
		}
		return trackers;
	}

	public HashMap<String, Tracker> getTrackerData() {
		if (trackerData != null) {
			return trackerData;
		}
		ArrayList<Tracker> trackers = findByUserName();
		// 如果数据库没有数据，先和服务器同步，重新取一次数据
		if (trackers.size() == 0) {
			synTracker();
			trackers = findByUserName();
		}
		trackerData = new HashMap<String, Tracker>();
		for (Tracker tracker : trackers) {
			trackerData.put(tracker.getTrackerNo(), tracker);
		}
		return trackerData;
	}

	/**
	 * @param userName
	 * @return 获取适配listView数据
	 */
	public List<HashMap<String, Object>> getAdapterData() {
		if (adapterData != null) {
			return adapterData;
		}
		adapterData = new ArrayList<HashMap<String, Object>>();
		for (Tracker tracker : getTrackerData().values()) {
			HashMap<String, Object> trackerMap = new HashMap<String, Object>();
			trackerMap.put("name", tracker.getName());
			trackerMap.put("trackerNo", tracker.getTrackerNo());
			trackerMap.put("state", tracker.getState());
			adapterData.add(trackerMap);
		}
		return adapterData;
	}

	public void deleteByUserName() {
		db.execSQL("DELETE FROM tracker where username=?",
				new Object[] { getUserName() });
	}

	public void updateState() {
		// listView适配数据清空
		adapterData = null;
		// 删除数据库中改用户的跟踪器记录
		deleteByUserName();
		// 定义两个字符串分别保存要监控的和取消监控的跟踪器号码
		StringBuilder addSb = new StringBuilder();
		StringBuilder delSb = new StringBuilder();
		// 重新添加用户跟踪器记录
		for (Tracker tracker : trackerData.values()) {
			if (tracker.getState()) {
				addSb.append(tracker.getTrackerNo() + ",");
			} else {
				delSb.append(tracker.getTrackerNo() + ",");
			}
			//更新数据库记录
			db
					.execSQL(
							"INSERT INTO tracker (name,trackerno,username,state) values(?,?,?,?)",
							new Object[] { tracker.getName(),
									tracker.getTrackerNo(), getUserName(),
									tracker.getState() });
		}
		String addTrackerNos = null;
		String delTrackerNos = null;
		//向实时监控服务器发送添加和删除跟踪器指令
		if(addSb.length()>0){
			addTrackerNos = 
				addSb.substring(0, addSb.length() - 1);
		}
		if(delSb.length()>0){
			delTrackerNos = 
				delSb.substring(0, delSb.length() - 1);
		}
		MonitorClient.getInstance().updateTracker(addTrackerNos, delTrackerNos);
	}

	public LoginBean getLogin() {
		Cursor cursor = db.rawQuery("select * from login", null);
		if (cursor.moveToFirst()) {
			LoginBean loginBean = new LoginBean();
			loginBean.setUsername(cursor.getString(cursor
					.getColumnIndex("username")));
			loginBean.setPassword(cursor.getString(cursor
					.getColumnIndex("password")));
			loginBean.setRemember(cursor.getInt(cursor
					.getColumnIndex("remember")));
			return loginBean;
		}
		return null;
	}

	public void updateLogin(String username, String password, boolean remember) {
		db.execSQL("DELETE FROM login");
		db
				.execSQL(
						"INSERT INTO login (username,password,remember) values (?,?,?)",
						new Object[] { username, password, remember });
	}

	private String getUserName() {
		return HttpClientUtil.getUserName();
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		String sql = "CREATE TABLE tracker(id integer primary key autoincrement,trackerno varchar(13),name varchar(50),username varchar(50),state integer)";
		db.execSQL(sql);
		sql = "CREATE TABLE login(id integer primary key autoincrement,username varchar(20),password varchar(20),remember integer)";
		db.execSQL(sql);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS tracker");
		db.execSQL("DROP TABLE IF EXISTS login");
		onCreate(db);
	}

	public void clearData() {
		this.adapterData = null;
		this.trackerData = null;
	}

	public void setTrackerData(HashMap<String, Tracker> trackerData) {
		this.trackerData = trackerData;
	}

}
