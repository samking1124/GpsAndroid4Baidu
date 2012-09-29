package org.gps.service;

public class Tracker {
	private int id;
	private String trackerNo;
	private String name;
	private String userName;
	private boolean state;
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getTrackerNo() {
		return trackerNo;
	}
	public void setTrackerNo(String trackerNo) {
		this.trackerNo = trackerNo;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	@Override
	public String toString() {
		return name;
	}
	public boolean getState() {
		return state;
	}
	public void setState(boolean state) {
		this.state = state;
	}
}
