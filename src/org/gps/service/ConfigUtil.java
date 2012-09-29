package org.gps.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import android.util.Log;

public class ConfigUtil {
	private final static String TAG = "ConfigUtil";
	public final static String API_KEY = getConfig().getProperty("api_key");
	public final static int INIT_LAT = Integer.parseInt(getConfig().getProperty("init_lat"));
	public final static int INIT_LNG = Integer.parseInt(getConfig().getProperty("init_lng"));
	public final static int INIT_ZOOM = Integer.parseInt(getConfig().getProperty("init_zoom"));
	public final static int MONITOR_ZOOM = Integer.parseInt(getConfig().getProperty("monitor_zoom"));
	public final static int HISTORY_PLAY_ZOOM = Integer.parseInt(getConfig().getProperty("history_play_zoom"));
	
	
	public static Properties getConfig() {  
        Properties props = new Properties();  
        InputStream in = ConfigUtil.class.getResourceAsStream("/config.properties");  
        try {  
            props.load(in);  
        } catch (IOException e) {  
           Log.e(TAG,e.getMessage());
        }  
        return props;  
    }  
	
}
