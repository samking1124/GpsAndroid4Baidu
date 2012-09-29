package org.gps.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import android.util.Log;

public class HttpClientUtil {
	private static final String TAG = "HttpClientUtil";
	private static String JSESSIONID;
	private static String userName;
	private static String password;
	private static boolean hasLogin = false;
	private static DefaultHttpClient client = new DefaultHttpClient();
	private final static String URL = ConfigUtil.getConfig().getProperty(
			"http_server_ip");

	public static boolean login(String userName, String password) {
		try {
			
			HttpPost post = new HttpPost(URL + "login_check.do");
			List<NameValuePair> nameValues = new ArrayList<NameValuePair>();
			NameValuePair userNamePair = new BasicNameValuePair(
					"user.loginName", userName);
			NameValuePair passwordPair = new BasicNameValuePair(
					"user.loginPwd", password);
			NameValuePair md5Pair = new BasicNameValuePair("checkcode", MD5
					.getMD5(userName + password + "hx"));
			nameValues.add(userNamePair);
			nameValues.add(passwordPair);
			nameValues.add(md5Pair);
			post.setEntity(new UrlEncodedFormEntity(nameValues, HTTP.UTF_8));
			HttpResponse response = client.execute(post);
			// 返回字符超过3000，说明返回的为错误页面，否则返回的为登陆页面
			if (Integer.parseInt(response.getHeaders("Content-Length")[0]
					.getValue()) > 3000) {
				return false;
			}
			List<Cookie> cookies = client.getCookieStore().getCookies();
			for (Cookie cookie : cookies) {
				if ("JSESSIONID".equals(cookie.getName())) {
					JSESSIONID = ";jsessionid=" + cookie.getValue();
				}
			}
			if (JSESSIONID == null) {
				return false;
			}
			HttpClientUtil.userName = userName;
			HttpClientUtil.password = password;
			hasLogin = true;
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static ArrayList<Tracker> getTrackers() {
		try {
			if(!hasLogin){
				login(userName, password);
			}
			if(!hasLogin){
				return null;
			}
			HttpPost post = new HttpPost(URL + "tracker_getTkNos.do"
					+ JSESSIONID);
			HttpResponse response = client.execute(post);
			JSONObject result = new JSONObject(EntityUtils.toString(response
					.getEntity()));
			Log.d(TAG, result.get("info").toString());
			JSONArray trackers = new JSONArray(result.get("info").toString());
			ArrayList<Tracker> tks = new ArrayList<Tracker>();
			for (int i = 0; i < trackers.length(); i++) {
				Tracker tracker = new Tracker();
				tracker.setName(trackers.getJSONObject(i).getString("name"));
				tracker.setTrackerNo(trackers.getJSONObject(i).getString(
						"trackerNo"));
				tks.add(tracker);
			}
			return tks;
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
			return null;
		}
	}

	public static ArrayList<History> getHistory(String trackerNo, String date) {
		try {
			if(!hasLogin){
				login(userName, password);
			}
			if(!hasLogin){
				return null;
			}
			HttpPost post = new HttpPost(URL + "tracker_getTrack.do"
					+ JSESSIONID);
			List<NameValuePair> nameValues = new ArrayList<NameValuePair>();
			NameValuePair trakcerNoPair = new BasicNameValuePair(
					"report.trackerNo", trackerNo);
			NameValuePair startTimePair = new BasicNameValuePair(
					"report.startTime", date + " 00:00:00");
			NameValuePair endTimePair = new BasicNameValuePair(
					"report.endTime", date + " 24:00:00");
			NameValuePair showGsmPair = new BasicNameValuePair(
					"report.showGsm", "0");
			NameValuePair startPair = new BasicNameValuePair("start", "0");
			NameValuePair limitPair = new BasicNameValuePair("limit", "100");
			nameValues.add(trakcerNoPair);
			nameValues.add(startTimePair);
			nameValues.add(endTimePair);
			nameValues.add(showGsmPair);
			nameValues.add(startPair);
			nameValues.add(limitPair);
			post.setEntity(new UrlEncodedFormEntity(nameValues, HTTP.UTF_8));
			HttpResponse response = client.execute(post);
			JSONObject result = new JSONObject(EntityUtils.toString(response
					.getEntity(),HTTP.UTF_8));
			JSONArray jsonArray = result.getJSONArray("results");
			ArrayList<History> historys = new ArrayList<History>();
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject o = jsonArray.getJSONObject(i);
				historys.add(new History(o.getString("lat"),
						o.getString("lon"), o.getString("orgiLat"), o
								.getString("orgiLon")));
			}
			return historys;
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
			return null;
		}
	}

	public static String getUserName() {
		return userName;
	}

	public static void setUserName(String userName) {
		HttpClientUtil.userName = userName;
	}
	
	public static ArrayList<Marker> getLatestPos(String trackerNos){
		HttpPost post = new HttpPost(URL + "tracker_getLatestPos.do"
				+ JSESSIONID);
		List<NameValuePair> nameValues = new ArrayList<NameValuePair>();
		NameValuePair trakcerNoPair = new BasicNameValuePair(
				"tkNo", trackerNos);
		nameValues.add(trakcerNoPair);
		try {
			post.setEntity(new UrlEncodedFormEntity(nameValues, HTTP.UTF_8));
			HttpResponse response = client.execute(post);
			String xml = EntityUtils.toString(response.getEntity(),HTTP.UTF_8);
			return XmlUtil.getMarkers(xml);
		} catch (Exception e) {
			Log.e(TAG,e.getMessage());
			return null;
		}
	}
}
