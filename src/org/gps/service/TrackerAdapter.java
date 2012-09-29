package org.gps.service;

import java.util.HashMap;
import java.util.List;

import org.gps.activity.R;
import org.gps.db.GpsDBOpenHelper;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

public class TrackerAdapter extends BaseAdapter {
	private LayoutInflater inflater;
	private List<HashMap<String, Object>> data;
	private HashMap<String, Tracker> trackerData;

	public TrackerAdapter(Context context, GpsDBOpenHelper dbHelper) {
		inflater = LayoutInflater.from(context);
		this.data = dbHelper.getAdapterData();
		this.trackerData = dbHelper.getTrackerData();
	}

	@Override
	public int getCount() {
		return data.size();
	}

	@Override
	public Object getItem(int position) {
		return data.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}
	
	public void allselect(){
		for(HashMap<String, Object> hm : data){
			hm.put("state",true);
			String trackerNo =(String)hm.get("trackerNo");
			trackerData.get(trackerNo).setState(true);
		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if (convertView == null) {
			holder = new ViewHolder();
			convertView = inflater.inflate(R.layout.trackeritem, parent, false);
			holder.nameView = (TextView) convertView.findViewById(R.id.name);
			holder.stateView = (CheckBox) convertView.findViewById(R.id.state);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		holder.nameView.setText(data.get(position).get("name").toString());
		holder.stateView
				.setChecked((Boolean) (data.get(position).get("state")));
		return convertView;
	}

	public List<HashMap<String, Object>> getData() {
		return data;
	}

	public void setData(List<HashMap<String, Object>> data) {
		this.data = data;
	}

	public final class ViewHolder {
		public TextView nameView;
		public TextView trackerNoView;
		public CheckBox stateView;
	}

}
