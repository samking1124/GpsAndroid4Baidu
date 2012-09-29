package org.gps.activity;


import java.util.ArrayList;

import junit.framework.Assert;

import org.gps.db.GpsDBOpenHelper;
import org.gps.service.HttpClientUtil;
import org.gps.service.Tracker;

import android.test.AndroidTestCase;
import android.util.Log;

public class GpsTest extends AndroidTestCase {
	private final static String TAG = "GpsTest";
	
	public void testLogin() throws Exception{
        HttpClientUtil.login("jani", "jani");
	}
	
	public void testGetTrackers() throws Exception{
		Log.e(TAG,HttpClientUtil.getTrackers().size()+"");
	}
	
	public void testSynTracker(){
		Assert.assertTrue(GpsDBOpenHelper.getInstance(this.getContext()).synTracker());
	}
	
	public void testFindByUserName() throws Exception{
		ArrayList<Tracker> trackers = GpsDBOpenHelper.getInstance(this.getContext()).findByUserName();
		if(trackers.size()==0){
			Log.e(TAG,"null");
		}
		for(Tracker tracker : trackers){
			Log.e(TAG, tracker.toString());
		}
	}
	
	public void testGetHistory() throws Exception {
		testLogin();
		HttpClientUtil.getHistory("353327022155352", "2012-3-6");
	}
}
