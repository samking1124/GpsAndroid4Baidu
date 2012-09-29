package org.gps.service;

import org.gps.db.GpsDBOpenHelper;

import com.baidu.mapapi.GeoPoint;
import com.baidu.mapapi.OverlayItem;


public class Marker extends OverlayItem {
	private String name;
	private String tkNo;
	private String recordeTime;
	private String address = "查询中...";
	private int isGsm;
	private String title;
	private String snippet;	
	private boolean hasAdress = false;

	public void setHasAdress(boolean hasAdress) {
		this.hasAdress = hasAdress;
	}

	public Marker(GeoPoint p, String title, String snippet) {
		super(p, title, snippet);
		this.title = title;
		this.snippet = snippet;
	}

	public Marker(GeoPoint p, String tkNo, String time, int isGsm) {
		super(p, null, null);
		this.name = GpsDBOpenHelper.getInstance().getTrackerData().get(tkNo)
				.getName();
		this.tkNo = tkNo;
		this.recordeTime = time;
		this.isGsm = isGsm;
		this.title = name + "      " + (isGsm == 1 ? "GSM" : "GPS");
	
	}


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getTkNo() {
		return tkNo;
	}

	public void setTkNo(String tkNo) {
		this.tkNo = tkNo;
	}

	public String getRecordeTime() {
		return recordeTime;
	}

	public void setRecordeTime(String recordeTime) {
		this.recordeTime = recordeTime;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}


	public String getSnippet() {
		if (snippet != null) {
			return snippet;
		}
		return "编号：" + tkNo + "\n时间：" + recordeTime + "\n地址：" + address;
	}

	public void setSnippet(String snippet) {
		this.snippet = snippet;
	}

	public int getIsGsm() {
		return isGsm;
	}

	public void setIsGsm(int isGsm) {
		this.isGsm = isGsm;
	}

	public boolean hasAdress() {
		return hasAdress;
	}

}
